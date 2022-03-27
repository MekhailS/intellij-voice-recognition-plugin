package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.configuration.ConfigurationService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAwareAction

private const val YAML_EXTENSION = "yaml"

class LoadDSLFromFileAction : DumbAwareAction("Load DSL from file") {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(YAML_EXTENSION)
        FileChooser.chooseFile(descriptor, e.project, null) { virtualFile ->
            val file = virtualFile.toNioPath().toFile()
            ConfigurationService.getInstance().run {
                addConfigurationFromFile(file)
                selectConfigurationFromFile(file)
            }
        }
    }
}