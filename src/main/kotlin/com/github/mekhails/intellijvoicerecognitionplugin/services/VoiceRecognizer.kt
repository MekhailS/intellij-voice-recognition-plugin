package com.github.mekhails.intellijvoicerecognitionplugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.runBackgroundableTask
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*


private const val DAFAULT_MODEL = "/Users/Mikhail.Shagvaliev/Downloads/vosk-model-en-us-0.22-lgraph"
///home/viktor/IdeaProjects/intellij-voice-recognition-plugin/vosk-model-en-us-0.22-lgraph

class VoiceRecognizer : Disposable {
    val isActive: Boolean
        get() = voiceModelFuture.getNow(null)?.isActive?.get() ?: false

    private class VoiceModel {
        val model = Model(DAFAULT_MODEL)
        val format = AudioFormat(160000F, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val microphone: TargetDataLine = AudioSystem.getLine(info) as TargetDataLine
        val recognizer: Recognizer = Recognizer(model, 160000F)

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

    fun endRecognition(actionOnRecognizedString: (String) -> Unit) {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            if (!voiceModel.isActive.compareAndSet(true, false))
                return@whenComplete
            voiceRecognition.whenComplete { voiceRecognitionCompleted, _ ->
                actionOnRecognizedString(voiceRecognitionCompleted)
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
                    var numBytesRead: Int
                    val chunkSize = 2048 * 2
                    var bytesRead = 0
                    val maxBytes = 100000000
                    val b = ByteArray(chunkSize)
                    voiceModel.recognizer.reset()

                    val dataLineInfo = DataLine.Info(SourceDataLine::class.java, voiceModel.format)
                    val speakers = AudioSystem.getLine(dataLineInfo) as SourceDataLine
                    speakers.open(voiceModel.format)
                    speakers.start()

                    while (bytesRead <= maxBytes && !voiceModel.exit.get() && voiceModel.isActive.get()) {
                        numBytesRead = voiceModel.microphone.read(b, 0, chunkSize)
                        bytesRead += numBytesRead
                        voiceModel.recognizer.acceptWaveForm(b, numBytesRead)
                        with(voiceModel.recognizer) {
                            val result = JSONObject(partialResult).getString("partial")
                            if (result.isNotBlank()) {
                                voiceRecognition.complete(result)
                            }
                        }
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