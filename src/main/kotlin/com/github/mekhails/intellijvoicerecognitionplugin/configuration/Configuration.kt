package com.github.mekhails.intellijvoicerecognitionplugin.configuration

class Configuration(
    var actions: List<ActionDescription> = emptyList(),
    var voiceCommands: List<VoiceCommandDescription> = emptyList()
) {
    fun validate(): ConfigurationValidationResult {
        val validationRules: List<Pair<String, () -> Boolean>> = listOf(
            "Action has no specified id" to {
                actions.any { it.id == null }
            },
            "Action has no definition, must be defined using 'actionString' or 'nestedActions'" to {
                actions.any { it.actionString == null && it.nestedActions == null }
            },
            "Action is defined both as 'actionString' and as 'nestedActions'" to {
                actions.any { it.actionString != null && it.nestedActions != null }
            },
            "Can't find action with actionId from 'nestedActions' list" to {
                val actionIds = actions.map { it.id }.toSet()
                val nestedActionsIds = actions.flatMap {
                    it.nestedActions?.map { nestedAction -> nestedAction.id } ?: emptyList()
                }
                nestedActionsIds.any { it !in actionIds}
            },
            "No actionId in voiceCommand" to {
              voiceCommands.any { it.actionId == null }
            },
            "No phraze in voiceCommand" to {
              voiceCommands.any { it.phrase == null }
            },
            "Can't find action with actionId from voice command" to {
                val actionIds = actions.map { it.id }.toSet()
                voiceCommands.any { it.actionId !in actionIds }
            }

        )
        return validationRules.firstOrNull { it.second.invoke() }?.let {
            ConfigurationValidationResult.Fail(it.first)
        } ?: ConfigurationValidationResult.Success(this)
    }

    fun phrasesToActionStrings(): Map<String, List<String>>? {
        class DFSNode(
            val data: ActionDescription,
            var wasVisited: Boolean = false,
            var foundLeaves: List<DFSNode> = emptyList(),
        ) {
            val id: String = data.id!!
            val children: List<String> = data.nestedActions?.map { it.id!! } ?: emptyList()

            val isLeafe: Boolean get() = children.isEmpty()
        }
        return null
    }

    sealed interface ConfigurationValidationResult {
        class Success(val validatedConfiguration: Configuration) : ConfigurationValidationResult
        class Fail(val message: String) : ConfigurationValidationResult
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