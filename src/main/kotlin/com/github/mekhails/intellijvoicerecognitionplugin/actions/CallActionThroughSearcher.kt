package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import javax.sound.sampled.*


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

        readFromMicro()
    }

    private fun readFromFile() {
        LibVosk.setLogLevel(LogLevel.DEBUG)
        val model = Model("vosk-model-en-us-0.22-lgraph")
        val inputStream = AudioSystem.getAudioInputStream(BufferedInputStream(FileInputStream(
            "test.wav"
        )))
        val recognizer = Recognizer(model, 16000F)

        var nbytes: Int
        val bytes = ByteArray(4096)
        while (inputStream.read(bytes).also { nbytes = it } >= 0) {
            if (recognizer.acceptWaveForm(bytes, nbytes))
                println("RESULT: ${recognizer.result}")
            else
                println("PARTIAL RESULT ${recognizer.partialResult}")
        }
        println("FINAL RESULT ${recognizer.finalResult}")
    }


    private fun readFromMicro() {
        LibVosk.setLogLevel(LogLevel.DEBUG)

        val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000F, 16, 2, 4, 44100F, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        var microphone: TargetDataLine

        Model("vosk-model-en-us-0.22-lgraph").use { model ->
            Recognizer(model, 120000F).use { recognizer ->
                try {
                    microphone = AudioSystem.getLine(info) as TargetDataLine
                    microphone.open(format)
                    microphone.start()
                    val out = ByteArrayOutputStream()
                    var numBytesRead: Int
                    val CHUNK_SIZE = 1024
                    var bytesRead = 0
                    var maxBytes = CHUNK_SIZE * 1000
                    val b = ByteArray(4096)
                    println("START OF RECOGNITION")
                    while (bytesRead <= maxBytes) {
                        numBytesRead = microphone.read(b, 0, CHUNK_SIZE)
                        bytesRead += numBytesRead
                        out.write(b, 0, numBytesRead)
                        if (recognizer.acceptWaveForm(b, numBytesRead)) {
                            println(recognizer.result)
                        } else {
                            println(recognizer.partialResult)
                        }
                    }
                    println("END OF RECOGNITION")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

}