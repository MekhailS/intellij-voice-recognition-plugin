package com.github.mekhails.intellijvoicerecognitionplugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.runBackgroundableTask
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*


private const val DAFAULT_MODEL = "/Users/Mikhail.Shagvaliev/Downloads/vosk-model-en-us-0.22-lgraph"

class VoiceRecognizer : Disposable {
    val isActive: Boolean
        get() = voiceModelFuture.getNow(null)?.isActive?.get() ?: false

    private class VoiceModel {
        var model = Model(DAFAULT_MODEL)
        val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000F, 16, 2, 4, 44100F, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val microphone: TargetDataLine = AudioSystem.getLine(info) as TargetDataLine
        val recognizer: Recognizer = Recognizer(model, 120000F)

        val isActive = AtomicBoolean(false)
        val exit = AtomicBoolean(false)
    }

    private val voiceModelFuture = CompletableFuture<VoiceModel>()

    private val voiceRecognition = CompletableFuture<String>()

    init {
        runBackgroundableTask("Loading Model", null, false) { _ -> voiceModelFuture.complete(VoiceModel()) }
    }

    override fun dispose() {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            voiceModel.exit.set(true)
            voiceModel.recognizer.close()
            voiceModel.model.close()
        }
    }

    fun endRecognition(callBack: (String) -> Unit) {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            if (!voiceModel.isActive.compareAndSet(true, false))
                return@whenComplete
            voiceRecognition.whenComplete { voiceRecognitionCompleted, _ ->
                callBack(voiceRecognitionCompleted)
            }
        }
    }

    fun startRecognition() {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            if (!voiceModel.isActive.compareAndSet(false, true))
                return@whenComplete
            runBackgroundableTask("Start Recognition", null, false) {
                try {
                    voiceModel.microphone.open(voiceModel.format)
                    voiceModel.microphone.start()
                    val out = ByteArrayOutputStream()
                    var numBytesRead: Int
                    val chunkSize = 2048 * 1
                    var bytesRead = 0
                    var maxBytes = 100000000
                    val b = ByteArray(chunkSize)
                    voiceModel.recognizer.reset()

                    val dataLineInfo = DataLine.Info(SourceDataLine::class.java, voiceModel.format)
                    val speakers = AudioSystem.getLine(dataLineInfo) as SourceDataLine
                    speakers.open(voiceModel.format)
                    speakers.start()

                    while (bytesRead <= maxBytes && !voiceModel.exit.get() && voiceModel.isActive.get()) {
                        numBytesRead = voiceModel.microphone.read(b, 0, chunkSize)
                        bytesRead += numBytesRead
                        out.write(b, 0, numBytesRead)
                        voiceModel.recognizer.acceptWaveForm(b, numBytesRead)
                        with(voiceModel.recognizer) {
                            println(partialResult)
                            println(finalResult)
                            println(result)
                        }
                        speakers.write(b, 0, numBytesRead)
                        println(voiceModel.recognizer.partialResult)
//                        val result = JSONObject(voiceModel.recognizer.partialResult).getString("partial")
//                        if (result.isNotBlank()) {
//                            voiceRecognition.complete(result)
//                        }
                    }
                    speakers.drain()
                    speakers.close()
                    voiceModel.microphone.close()
                    voiceModel.isActive.set(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}