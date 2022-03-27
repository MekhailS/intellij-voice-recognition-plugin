package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.configuration.ConfigurationService
import com.github.mekhails.intellijvoicerecognitionplugin.searcher.ActionSearcher
import com.github.mekhails.intellijvoicerecognitionplugin.searcher.RecognizedStringMatcher
import com.github.mekhails.intellijvoicerecognitionplugin.services.VoiceRecognizer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction


class CallActionThroughSearcher : DumbAwareAction() {
    override fun update(e: AnActionEvent) {
        if (e.project == null)
            e.presentation.isEnabledAndVisible = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val configurationService = ConfigurationService.getInstance()
        val currentConfiguration = configurationService.selectedConfiguration ?: return

        val voiceRecognizer = service<VoiceRecognizer>()

        if (!voiceRecognizer.isActive) {
            voiceRecognizer.startRecognition()
        } else {
            voiceRecognizer.endRecognition { recognizedString ->
                val actionPhrases = currentConfiguration.phrasesToActionStrings.keys

                val bestMatchedActionPhrase = actionPhrases.maxByOrNull { actionPhrase ->
                    RecognizedStringMatcher(recognizedString, actionPhrase).matchRate
                } ?: return@endRecognition

                val matchingRate = RecognizedStringMatcher(recognizedString, bestMatchedActionPhrase).matchRate
                if (matchingRate == 0F) return@endRecognition

                val actionStringsToPerform = currentConfiguration.phrasesToActionStrings[bestMatchedActionPhrase] ?: return@endRecognition

                runBackgroundableTask("Performing actions", null, false) { indicator ->
                    val actionSearcher = ActionSearcher(e.project, e.dataContext.getData(CommonDataKeys.EDITOR))
                    actionStringsToPerform.forEach { actionString ->
                        actionSearcher.searchAction(actionString, indicator)?.invoke()
                    }
                }
            }
        }
    }
}