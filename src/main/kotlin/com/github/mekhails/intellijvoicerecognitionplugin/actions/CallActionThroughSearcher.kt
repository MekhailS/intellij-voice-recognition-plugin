package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.searcher.ActionSearcher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.runBackgroundableTask
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.LiveSpeechRecognizer
import edu.cmu.sphinx.api.SpeechResult
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream


class CallActionThroughSearcher : AnAction() {
    override fun update(e: AnActionEvent) {
        if (e.project == null)
            e.presentation.isEnabledAndVisible = false
    }

    override fun actionPerformed(e: AnActionEvent) {
//        runBackgroundableTask("Rename file", e.project, true) { progressIndicator ->
//            val operationSearcher = ActionSearcher(e.project, e.dataContext.getData(CommonDataKeys.EDITOR))
//            operationSearcher.searchAction("Commit", progressIndicator)?.invoke()
//        }
//        recognizeLiveSpeech()
        recognizeSpeechFromFile("rename_file.wav")
    }

    private fun recognizeLiveSpeech() {
        val configuration = Configuration()
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us")
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")

        val recognizer = LiveSpeechRecognizer(configuration)
        // Start recognition process pruning previously cached data.
        recognizer.startRecognition(true)

        System.out.println("START")

        var result: SpeechResult?
        var i = 1
        while (recognizer.result.also { result = it } != null) {
            System.out.println("iteration + " + i++)
            if (result != null) {
                System.out.format("Hypothesis: %s\n", result!!.hypothesis)
            }
        }

        // Pause recognition process. It can be resumed then with startRecognition(false).
        recognizer.stopRecognition()

        System.out.println("END")
    }

    private fun recognizeSpeechFromFile (file: String) {
        System.out.println("START")

        val configuration = Configuration()
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us")
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")

        val recognizer = StreamSpeechRecognizer(configuration)
        val stream: InputStream = FileInputStream(File("D:\\dsl_project\\intellij-voice-recognition-plugin\\$file"))
        recognizer.startRecognition(stream)
        var result: SpeechResult?
        var i = 1
        while (recognizer.result.also { result = it } != null) {
            System.out.println("iteration + " + i++)
            if (result != null) {
                System.out.format("Hypothesis: %s\n", result!!.hypothesis)
            }
        }
        recognizer.stopRecognition()

        System.out.println("END")
    }

}