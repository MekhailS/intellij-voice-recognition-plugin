package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.configuration.ConfigurationService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import java.io.File

class OpenDSLAction(private val configurationFile: File): DumbAwareToggleAction(configurationFile.name.escapeMnemonic()) {
    override fun isSelected(e: AnActionEvent): Boolean =
        configurationFile == ConfigurationService.getInstance().selectedConfiguration?.file

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val configurationService = ConfigurationService.getInstance()
        if (!state && configurationFile == configurationService.selectedConfiguration?.file) {
            configurationService.unselectConfiguration()
            return
        }
        if (state) {
            configurationService.selectConfigurationFromFile(configurationFile)
        }
    }
}

private fun String.escapeMnemonic(): String = listOf("_", "&").fold(this) { str, symbolToEscape ->
    str.replace(symbolToEscape, symbolToEscape.repeat(2))
}