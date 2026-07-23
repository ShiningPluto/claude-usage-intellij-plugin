package com.github.dmitry.claudeusage

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.Json
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

data class ResolvedProxy(
    val proxy: Proxy,
    val username: String? = null,
    val password: String? = null
)

object ClaudeProxyResolver {
    private val LOG = Logger.getInstance(ClaudeProxyResolver::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val PROXY_ENV_KEYS = listOf("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy")
    private val SOCKS_SCHEMES = setOf("socks", "socks4", "socks5")
    private const val DEFAULT_HTTP_PROXY_PORT = 80
    private const val DEFAULT_SOCKS_PORT = 1080

    fun resolve(): ResolvedProxy? = effectiveProxyUrl()?.let { parse(it) }

    fun autoDetectedProxyUrl(): String? {
        readFromClaudeSettingsFile()?.let { return it }
        return PROXY_ENV_KEYS.firstNotNullOfOrNull { System.getenv(it)?.trim()?.takeIf { url -> url.isNotEmpty() } }
    }

    private fun effectiveProxyUrl(): String? {
        val manual = ClaudeUsageSettings.getInstance().state.proxyUrl.trim()
        return manual.ifEmpty { autoDetectedProxyUrl() }
    }

    private fun readFromClaudeSettingsFile(): String? {
        val file = File(System.getProperty("user.home"), ".claude/settings.json")
        if (!file.exists()) return null
        return try {
            val env = json.decodeFromString<ClaudeSettingsFile>(file.readText()).env ?: return null
            PROXY_ENV_KEYS.firstNotNullOfOrNull { env[it]?.trim()?.takeIf { url -> url.isNotEmpty() } }
        } catch (e: Exception) {
            LOG.warn("Failed to read Claude settings.json for proxy config: ${e.message}")
            null
        }
    }

    private fun parse(raw: String): ResolvedProxy? {
        return try {
            val uri = if ("://" in raw) URI(raw) else URI("http://$raw")
            val host = uri.host ?: return null
            val socks = uri.scheme?.lowercase() in SOCKS_SCHEMES
            val type = if (socks) Proxy.Type.SOCKS else Proxy.Type.HTTP
            val port = if (uri.port != -1) uri.port else if (socks) DEFAULT_SOCKS_PORT else DEFAULT_HTTP_PROXY_PORT
            val (user, pass) = uri.userInfo?.split(":", limit = 2)
                ?.let { it.getOrNull(0) to it.getOrNull(1) } ?: (null to null)
            val proxy = Proxy(type, InetSocketAddress(host, port))
            if (socks && user != null) {
                // The JDK only reads SOCKS credentials from the JVM-wide default Authenticator,
                // which the plugin must not replace, so they are dropped here.
                LOG.warn("Credentials in a SOCKS proxy URL are not supported and will be ignored")
                ResolvedProxy(proxy)
            } else {
                ResolvedProxy(proxy, user, pass)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse proxy URL: ${e.message}")
            null
        }
    }
}
