package com.github.mekhails.intellijvoicerecognitionplugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.runBackgroundableTask
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.*

private const val DAFAULT_MODEL = "/home/viktor/IdeaProjects/intellij-voice-recognition-plugin/vosk-model-en-us-0.22-lgraph"

class VoiceRecognizer : Disposable {
    private class VoiceModel {
        var model = Model(DAFAULT_MODEL)
        val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000F, 16, 2, 4, 44100F, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val microphone: TargetDataLine = AudioSystem.getLine(info) as TargetDataLine
        val isActive = AtomicLong(-1)
        val exit = AtomicBoolean(false)
    }

    private val voiceModelFuture = CompletableFuture<VoiceModel>()

    init {
        runBackgroundableTask("tt", null, false) { _ -> voiceModelFuture.complete(VoiceModel()) }
        startRecognition()
    }

    override fun dispose() {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            voiceModel.exit.set(true)
        }
    }

    fun changeJobStatus() {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            voiceModel.isActive.getAndIncrement();
        }
    }

    private fun isActive(): Boolean {
        return voiceModelFuture.thenApply { voiceModel ->
            voiceModel.isActive.get() % 2 == 0L
        }.getNow(false)
    }

    private fun startRecognition2() {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            voiceModel.model.use {
                Recognizer(it, 120000F).use { recognizer ->
                    try {
                        voiceModel.microphone.open(voiceModel.format)
                        voiceModel.microphone.start()
                        val out = ByteArrayOutputStream()
                        var numBytesRead: Int
                        val CHUNK_SIZE = 1024
                        var bytesRead = 0
                        var maxBytes = 100000000
                        val b = ByteArray(4096)
                        while (bytesRead <= maxBytes && !voiceModel.exit.get() && isActive()) {
                            numBytesRead = voiceModel.microphone.read(b, 0, CHUNK_SIZE)
                            bytesRead += numBytesRead
                            out.write(b, 0, numBytesRead)
                            recognizer.acceptWaveForm(b, numBytesRead)
//                                    println(recognizer.finalResult)
                            val result = JSONObject(recognizer.finalResult).getString("text")
                            if (result.isNotBlank()) {
                                println(result)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun startRecognition() {
        voiceModelFuture.whenComplete { voiceModel, _ ->
            runBackgroundableTask("e", null, false) {
                voiceModel.model.use {
                    Recognizer(it, 120000F).use { recognizer ->
                        try {
                            voiceModel.microphone.open(voiceModel.format)
                            voiceModel.microphone.start()
                            val out = ByteArrayOutputStream()
                            var numBytesRead: Int
                            val CHUNK_SIZE = 1024
                            var bytesRead = 0
                            var maxBytes = 100000000
                            val b = ByteArray(4096)
                            while (bytesRead <= maxBytes && !voiceModel.exit.get() && isActive()) {
                                numBytesRead = voiceModel.microphone.read(b, 0, CHUNK_SIZE)
                                bytesRead += numBytesRead
                                out.write(b, 0, numBytesRead)
                                recognizer.acceptWaveForm(b, numBytesRead)
//                                    println(recognizer.finalResult)
                                val result = JSONObject(recognizer.finalResult).getString("text")
                                if (result.isNotBlank()) {
                                    println(result)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
