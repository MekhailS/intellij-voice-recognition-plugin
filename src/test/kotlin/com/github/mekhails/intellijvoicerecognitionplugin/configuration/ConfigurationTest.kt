package com.github.mekhails.intellijvoicerecognitionplugin.configuration

import org.junit.Test
import kotlin.test.assertEquals

class ConfigurationTest {
    @Test
    fun `no action id`() = doConfigurationTest(
        Configuration(
            listOf(
                ActionDescription(null, null, "rename")
            ),
            listOf(
                VoiceCommandDescription("rename_id", "rename")
            )
        ),
        expectedValidationError = Configuration.ValidationError.NO_SPECIFIED_ID,
        expectedPhrasesToActionStringMapping = null
    )

    @Test
    fun `valid action tree`() = doConfigurationTest(
        Configuration(
            listOf(
                actionAsNested("A", listOf("AA", "AB")),

                actionAsNested("AA", listOf("AAA", "AAB")),
                actionAsString("AAA", "AAA"),
                actionAsString("AAB", "AAB"),

                actionAsNested("AB", listOf("ABA", "ABB")),
                actionAsString("ABA", "ABA"),
                actionAsString("ABB", "ABB"),

                actionAsNested("B", listOf("BA", "BB")),

                actionAsNested("BA", listOf("BAA", "BAB")),
                actionAsString("BAA", "BAA"),
                actionAsString("BAB", "BAB"),

                actionAsNested("BB", listOf("BBA", "BBB")),
                actionAsString("BBA", "BBA"),
                actionAsString("BBB", "BBB"),
            ),
            listOf(
                VoiceCommandDescription("A", "phrase_A"),
                VoiceCommandDescription("B", "phrase_B")
            )
        ),
        expectedValidationError = null,
        expectedPhrasesToActionStringMapping = mapOf(
            "phrase_A" to setOf("AAA", "AAB", "ABA", "ABB"),
            "phrase_B" to setOf("BAA", "BAB", "BBA", "BBB")
        )
    )

    @Test
    fun `valid action tree common leaf`() = doConfigurationTest(
        Configuration(
            listOf(
                actionAsNested("A", listOf("AA", "AB")),

                actionAsNested("AA", listOf("AAA", "AAB")),
                actionAsString("AAA", "AAA"),
                actionAsString("AAB", "AAB"),

                actionAsNested("AB", listOf("ABA", "ABB")),
                actionAsString("ABA", "ABA"),
                actionAsNested("ABB", listOf("ABBBBB")),

                actionAsNested("B", listOf("BA", "BB")),

                actionAsNested("BA", listOf("BAA", "BAB")),
                actionAsString("BAA", "BAA"),
                actionAsString("BAB", "BAB"),

                actionAsNested("BB", listOf("BBA", "BBB")),
                actionAsString("BBA", "BBA"),
                actionAsNested("BBB", listOf("ABBBBB")),

                actionAsString("ABBBBB", "ABBBBB")
            ),
            listOf(
                VoiceCommandDescription("A", "phrase_A"),
                VoiceCommandDescription("B", "phrase_B")
            )
        ),
        expectedValidationError = null,
        expectedPhrasesToActionStringMapping = mapOf(
            "phrase_A" to setOf("AAA", "AAB", "ABA", "ABBBBB"),
            "phrase_B" to setOf("BAA", "BAB", "BBA", "ABBBBB")
        )
    )

    @Test
    fun `invalid action tree cycle in B part`() = doConfigurationTest(
        Configuration(
            listOf(
                actionAsNested("A", listOf("AA", "AB")),

                actionAsNested("AA", listOf("AAA", "AAB")),
                actionAsString("AAA", "AAA"),
                actionAsString("AAB", "AAB"),

                actionAsNested("AB", listOf("ABA", "ABB")),
                actionAsString("ABA", "ABA"),
                actionAsString("ABB", "ABB"),

                actionAsNested("B", listOf("BA", "BB")),

                actionAsNested("BA", listOf("BAA", "BAB")),
                actionAsString("BAA", "BAA"),
                actionAsString("BAB", "BAB"),

                actionAsNested("BB", listOf("B", "BBA", "BBB")),
                actionAsString("BBA", "BBA"),
                actionAsString("BBB", "BBB"),
            ),
            listOf(
                VoiceCommandDescription("A", "phrase_A"),
                VoiceCommandDescription("B", "phrase_B")
            )
        ),
        expectedValidationError = Configuration.ValidationError.INVALID_ACTIONS_STRUCTURE,
        expectedPhrasesToActionStringMapping = null
    )

    @Test
    fun `invalid action tree duplicated action ids`() = doConfigurationTest(
        Configuration(
            listOf(
                actionAsNested("A", listOf("AA", "AB")),

                actionAsNested("AA", listOf("AAA", "AAB")),
                actionAsString("AAA", "AAA"),
                actionAsString("AAB", "AAB"),

                actionAsNested("AB", listOf("ABA", "ABB")),
                actionAsString("ABA", "ABA"),
                actionAsString("ABB", "ABB"),

                actionAsNested("B", listOf("BA", "BB")),

                actionAsNested("BA", listOf("BAA", "BAB")),
                actionAsString("BAA", "BAA"),
                actionAsString("BAB", "BAB"),

                actionAsNested("BB", listOf("BBA", "BBB")),
                actionAsString("BBA", "BBA"),
                actionAsString("BBB", "BBB"),

                actionAsNested("BB", listOf("BBA"))
            ),
            listOf(
                VoiceCommandDescription("A", "phrase_A"),
                VoiceCommandDescription("B", "phrase_B")
            )
        ),
        expectedValidationError = Configuration.ValidationError.DUPLICATED_ACTION_IDS,
        expectedPhrasesToActionStringMapping = null
    )

    private fun doConfigurationTest(configuration: Configuration,
                                    expectedValidationError: Configuration.ValidationError?,
                                    expectedPhrasesToActionStringMapping: Map<String, Set<String>>?) {
        val result = configuration.validate()
        when(result) {
            is Configuration.ConfigurationValidationResult.Success ->
                assertEquals(expectedPhrasesToActionStringMapping, configuration.phrasesToActionStrings)
            is Configuration.ConfigurationValidationResult.Fail ->
                assertEquals(expectedValidationError, result.validationError)
        }
    }

    private fun actionAsNested(id: String, nestedActionsIds: List<String>) = ActionDescription(
        id,
        nestedActionsIds.map { ActionId(it) }
    )

    private fun actionAsString(id: String, actionString: String) = ActionDescription(
        id,
        null,
        actionString
    )
}