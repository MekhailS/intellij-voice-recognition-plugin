package com.github.mekhails.intellijvoicerecognitionplugin.configuration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.error.YAMLException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

const val OUR_NOTIFICATION_GROUP_ID = "voice_recognition"

const val ELEVEN_ID = "11"
const val ELEVEN_ACTION_STRING = "11"
const val ELEVEN_PHRASE = "eleven"

class Configuration(
    var actions: List<ActionDescription> = emptyList(),
    var voiceCommands: List<VoiceCommandDescription> = emptyList()
) {
    companion object {
        fun loadAndValidate(file: File): Configuration? {
            val configuration = loadFromFile(file)?.withBundledActions()
            if (configuration == null) {
                Notification(
                    OUR_NOTIFICATION_GROUP_ID,
                    "Can't load configuration from file $file",
                    "",
                    NotificationType.WARNING
                ).notify(null)
                return null
            }

            return when(val validationResult = configuration.validate()) {
                is ConfigurationValidationResult.Fail -> {
                    Notification(
                        OUR_NOTIFICATION_GROUP_ID,
                        "Invalid configuration file $file",
                        validationResult.validationError.message,
                        NotificationType.WARNING
                    ).notify(null)
                    null
                }
                is ConfigurationValidationResult.Success -> validationResult.validatedConfiguration
            }
        }

        private fun loadFromFile(file: File): Configuration? {
            val yaml = Yaml(Constructor(Configuration::class.java))
            return try {
                yaml.load<Configuration?>(FileInputStream(file))?.apply { this.file = file }
            } catch (e: FileNotFoundException) {
                null
            } catch (e: YAMLException) {
                null
            }
        }
    }

    lateinit var file: File
        private set

    lateinit var phrasesToActionStrings: Map<String, Set<String>>
        private set

    enum class ValidationError(val message: String, val rule: Configuration.() -> Boolean) {
        NO_SPECIFIED_ID("Action has no specified id", {
            actions.any { it.id == null }
        }),
        NO_ACTION_DEFINITION("Action has no definition, must be defined using 'actionString' or 'nestedActions'", {
            actions.any { it.actionString == null && it.nestedActions == null }
        }),
        DUPLICATE_DEFINITION("Action is defined both as 'actionString' and as 'nestedActions'", {
            actions.any { it.actionString != null && it.nestedActions != null }
        }),
        INVALID_ACTION_IN_NESTED("Can't find action with actionId from 'nestedActions' list", {
            val actionIds = actions.map { it.id }.toSet()
            val nestedActionsIds = actions.flatMap {
                it.nestedActions?.map { nestedAction -> nestedAction.id } ?: emptyList()
            }
            nestedActionsIds.any { it !in actionIds
            }
        }),
        NO_ACTION_IN_VOICE_COMMAND("No actionId in voiceCommand", {
            voiceCommands.any { it.actionId == null }
        }),
        NO_PHRASE_IN_VOICE_COMMAND("No phrase in voiceCommand", {
            voiceCommands.any { it.phrase == null }
        }),
        INVALID_ACTION_IN_VOICE_COMMAND("Can't find action with actionId from voice command", {
            val actionIds = actions.map { it.id }.toSet()
            voiceCommands.any { it.actionId !in actionIds }
        }),
        DUPLICATED_ACTION_IDS("Actions have same ids", {
           actions.groupBy { it.id }.values.any { it.size > 1 }
        }),
        INVALID_ACTIONS_STRUCTURE("", checkActionsStructureAndInitMapping@{
            val computedPhrasesToActionStrings = computePhrasesToActionStrings()
                ?: return@checkActionsStructureAndInitMapping true

            phrasesToActionStrings = computedPhrasesToActionStrings
            false
        })
    }

    fun validate(): ConfigurationValidationResult {
        return ValidationError.values().firstOrNull { it.rule.invoke(this) }?.let {
            ConfigurationValidationResult.Fail(it)
        } ?: ConfigurationValidationResult.Success(this)
    }

    private fun withBundledActions(): Configuration {
        return Configuration(
            actions + ActionDescription(ELEVEN_ID, null, ELEVEN_ACTION_STRING),
            voiceCommands + VoiceCommandDescription(ELEVEN_ID, ELEVEN_PHRASE)
        ).also { it.file = file }
    }

    fun computePhrasesToActionStrings(): Map<String, Set<String>>? {
        if (this::phrasesToActionStrings.isInitialized) return phrasesToActionStrings

        class DFSNode(
            val allNodes: MutableMap<String, DFSNode>,
            val data: ActionDescription,
            var wasVisited: Boolean = false,
            var foundLeaves: List<DFSNode> = emptyList(),
        ) {
            val id: String = data.id!!
            val childrenIds: List<String> = data.nestedActions?.map { it.id!! } ?: emptyList()

            val isLeafe: Boolean get() = childrenIds.isEmpty()

            fun findLeaves(): Boolean {
                wasVisited = true
                if (isLeafe) return true

                childrenIds.forEach { childId ->
                    allNodes.putIfAbsent(childId, DFSNode(allNodes, actions.first { it.id == childId }))
                }
                val children = childrenIds.mapNotNull { allNodes[it] }

                for (child in children) {
                    if (child.wasVisited)
                        return false
                    if (!child.findLeaves())
                        return false
                }

                foundLeaves = children.flatMap { if (it.isLeafe) listOf(it) else it.foundLeaves }
                return true
            }
        }
        val rootNodesWithPhrases = voiceCommands.map { voiceCommand ->
            val node = DFSNode(mutableMapOf(), actions.first { it.id == voiceCommand.actionId })
            node.allNodes[node.id] = node

            voiceCommand.phrase to node
        }
        for (rootNode in rootNodesWithPhrases.map { it.second }) {
            if (!rootNode.findLeaves()) return null
        }
        return rootNodesWithPhrases.associate {
            val rootNode = it.second
            val leavesIds = if (rootNode.isLeafe) listOf(rootNode) else rootNode.foundLeaves
            
            it.first!! to leavesIds.map { leaf -> leaf.data.actionString!! }.toSet()
        }
    }

    sealed interface ConfigurationValidationResult {
        class Success(val validatedConfiguration: Configuration) : ConfigurationValidationResult
        class Fail(val validationError: ValidationError) : ConfigurationValidationResult
    }
}

class ActionDescription(
    var id: String? = null,
    var nestedActions: List<ActionId>? = null,
    var actionString: String? = null
)

class ActionId(var id: String? = null)

class VoiceCommandDescription(
    var actionId: String? = null,
    var phrase: String? = null
)