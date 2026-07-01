package com.github.dmitry.claudeusage

import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.FlowLayout
import javax.swing.*

class ClaudeUsageSettingsComponent {

    private val mainPanel: JPanel
    private val quotaTierComboBox = JComboBox(ClaudeUsageSettings.QUOTA_TIER_OPTIONS)
    private val yellowThresholdField = JSpinner(SpinnerNumberModel(70, 0, 100, 1))
    private val redThresholdField = JSpinner(SpinnerNumberModel(90, 0, 100, 1))
    private val credentialsPathField = JBTextField()
    private val refreshIntervalField = JSpinner(SpinnerNumberModel(ClaudeUsageSettings.DEFAULT_REFRESH_INTERVAL_MINUTES, 1, 60, 1))
    private val useMacKeychainCheckBox = JBCheckBox("Use macOS Keychain (recommended on macOS)").apply {
        isEnabled = SystemInfo.isMac
        toolTipText = if (SystemInfo.isMac) {
            "Read credentials from macOS Keychain (Claude Code stores them there). Falls back to the file below if Keychain is unavailable."
        } else {
            "Available on macOS only"
        }
    }
    private val notifyOnThresholdCheckBox = JBCheckBox("Show IDE notification when usage crosses a warning threshold").apply {
        toolTipText = "Pops up a balloon when the status bar quota crosses the yellow or red threshold"
    }
    private val resetButton = JButton("Reset to Defaults")

    init {
        val resetPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(resetButton)
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Status bar quota display:"), quotaTierComboBox, 1, false)
            .addLabeledComponent(JBLabel("Yellow warning threshold (%):"), yellowThresholdField, 1, false)
            .addLabeledComponent(JBLabel("Red warning threshold (%):"), redThresholdField, 1, false)
            .addLabeledComponent(JBLabel("Credentials file path:"), credentialsPathField, 1, false)
            .addLabeledComponent(JBLabel("Refresh interval (minutes):"), refreshIntervalField, 1, false)
            .addComponent(useMacKeychainCheckBox)
            .addComponent(notifyOnThresholdCheckBox)
            .addSeparator()
            .addComponent(resetPanel)
            .addComponentFillVertically(JPanel(), 0)
            .getPanel()

        resetButton.addActionListener {
            resetToDefaults()
        }
    }

    fun getPanel(): JPanel = mainPanel

    fun getPreferredFocusedComponent(): JComponent = quotaTierComboBox

    fun getQuotaTier(): String = quotaTierComboBox.selectedItem as String

    fun getYellowThreshold(): Int = yellowThresholdField.value as Int

    fun getRedThreshold(): Int = redThresholdField.value as Int

    fun getCredentialsPath(): String = credentialsPathField.text

    fun getRefreshIntervalMinutes(): Int = refreshIntervalField.value as Int

    fun getUseMacKeychain(): Boolean = useMacKeychainCheckBox.isSelected

    fun getNotifyOnThreshold(): Boolean = notifyOnThresholdCheckBox.isSelected

    fun setQuotaTier(tier: String) {
        quotaTierComboBox.selectedItem = tier
    }

    fun setYellowThreshold(value: Int) {
        yellowThresholdField.value = value
    }

    fun setRedThreshold(value: Int) {
        redThresholdField.value = value
    }

    fun setCredentialsPath(path: String) {
        credentialsPathField.text = path
    }

    fun setRefreshIntervalMinutes(value: Int) {
        refreshIntervalField.value = value
    }

    fun setUseMacKeychain(value: Boolean) {
        useMacKeychainCheckBox.isSelected = value
    }

    fun setNotifyOnThreshold(value: Boolean) {
        notifyOnThresholdCheckBox.isSelected = value
    }

    private fun resetToDefaults() {
        setQuotaTier(ClaudeUsageSettings.DEFAULT_QUOTA_TIER)
        setYellowThreshold(ClaudeUsageSettings.DEFAULT_YELLOW_THRESHOLD)
        setRedThreshold(ClaudeUsageSettings.DEFAULT_RED_THRESHOLD)
        setCredentialsPath(ClaudeUsageSettings.DEFAULT_CREDENTIALS_PATH)
        setRefreshIntervalMinutes(ClaudeUsageSettings.DEFAULT_REFRESH_INTERVAL_MINUTES)
        setUseMacKeychain(ClaudeUsageSettings.DEFAULT_USE_MAC_KEYCHAIN)
        setNotifyOnThreshold(ClaudeUsageSettings.DEFAULT_NOTIFY_ON_THRESHOLD)
    }
}
