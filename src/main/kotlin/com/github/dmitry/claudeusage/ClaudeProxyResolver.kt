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

    fun resolve(): ResolvedProxy? = effectiveProxyUrl()?.let { parse(it) }

    fun autoDetectedProxyUrl(): String? {
        readFromClaudeSettingsFile()?.let { return it }
        return System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")
            ?: System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy")
    }

    private fun effectiveProxyUrl(): String? {
        val manual = ClaudeUsageSettings.getInstance().state.proxyUrl.trim()
        return manual.ifEmpty { autoDetectedProxyUrl() }
    }

    private fun readFromClaudeSettingsFile(): String? {
        val file = File(System.getProperty("user.home"), ".claude/settings.json")
        if (!file.exists()) return null
        return try {
            val parsed = json.decodeFromString<ClaudeSettingsFile>(file.readText())
            parsed.env?.get("HTTPS_PROXY") ?: parsed.env?.get("HTTP_PROXY")
        } catch (e: Exception) {
            LOG.warn("Failed to read Claude settings.json for proxy config: ${e.message}")
            null
        }
    }

    private fun parse(raw: String): ResolvedProxy? {
        return try {
            val uri = if ("://" in raw) URI(raw) else URI("http://$raw")
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 80
            val (user, pass) = uri.userInfo?.split(":", limit = 2)
                ?.let { it.getOrNull(0) to it.getOrNull(1) } ?: (null to null)
            ResolvedProxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)), user, pass)
        } catch (e: Exception) {
            LOG.warn("Failed to parse proxy URL: ${e.message}")
            null
        }
    }
}
