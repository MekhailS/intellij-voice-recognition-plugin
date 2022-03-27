package com.github.mekhails.intellijvoicerecognitionplugin.configuration

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class ConfigurationService {
    companion object {
        fun getInstance() = service<ConfigurationService>()
    }

    private val configurationFiles = ConcurrentHashMap<File, Unit>().keySet(Unit)

    @Volatile
    var selectedConfiguration: Configuration? = null
        get() {
            if (field?.file?.isFile == false)
                field = null

            return field
        }
        private set

    fun refreshAndGetConfigurationFiles(): List<File> {
        refreshConfigurationFiles()
        checkSelectedConfiguration()
        return configurationFiles.toList().sorted()
    }

    private fun refreshConfigurationFiles() {
        val unavailableConfigurationProvidersFiles = configurationFiles.filter { !it.isFile }
        configurationFiles.removeAll(unavailableConfigurationProvidersFiles.toSet())
    }

    private fun checkSelectedConfiguration() {
        selectedConfiguration
    }

    fun addConfigurationFromFile(file: File): Configuration? {
        val configuration = Configuration.loadAndValidate(file) ?: return null
        configurationFiles.add(file)
        return configuration
    }

    fun selectConfigurationFromFile(file: File): Configuration? {
        selectedConfiguration = null
        if (file !in configurationFiles) return null

        val loadedConfiguration = Configuration.loadAndValidate(file)
        selectedConfiguration = loadedConfiguration
        return loadedConfiguration
    }

    fun unselectConfiguration() {
        selectedConfiguration = null
    }
}