package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.services.VoiceRecognizer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import javax.sound.sampled.*


class CallActionThroughSearcher : DumbAwareAction() {
    override fun update(e: AnActionEvent) {
        if (e.project == null)
            e.presentation.isEnabledAndVisible = false
    }

    override fun actionPerformed(e: AnActionEvent) {
//        runBackgroundableTask("Rename file", e.project, true) { progressIndicator ->
//            val operationSearcher = ActionSearcher(e.project, e.dataContext.getData(CommonDataKeys.EDITOR))
//            operationSearcher.searchAction("Commit", progressIndicator)?.invoke()
//        }

        val voiceRecognizer = service<VoiceRecognizer>()

        if (!voiceRecognizer.isActive) {
            voiceRecognizer.startRecognition()
        } else {
            voiceRecognizer.endRecognition { s -> println(s) }
        }
    }

//    private fun readFromFile() {
//        LibVosk.setLogLevel(LogLevel.DEBUG)
//        val model = Model("vosk-model-en-us-0.22-lgraph")
//        val inputStream = AudioSystem.getAudioInputStream(BufferedInputStream(FileInputStream(
//            "test.wav"
//        )))
//        val recognizer = Recognizer(model, 16000F)
//
//        var nbytes: Int
//        val bytes = ByteArray(4096)
//        while (inputStream.read(bytes).also { nbytes = it } >= 0) {
//            if (recognizer.acceptWaveForm(bytes, nbytes))
//                println("RESULT: ${recognizer.result}")
//            else
//                println("PARTIAL RESULT ${recognizer.partialResult}")
//        }
//        println("FINAL RESULT ${recognizer.finalResult}")
//    }


}