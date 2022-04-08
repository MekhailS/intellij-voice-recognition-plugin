package com.github.mekhails.intellijvoicerecognitionplugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.util.Disposer
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

private const val DEFAULT_MODEL = "default_model"

private val MICROPHONE_INITIALIZATION_DURATION = Duration.ofSeconds(40)

class VoiceRecognizer : Disposable {
    val isActive: Boolean get() = voiceModelInitializationTask.getNow(null)?.isActive ?: false

    private val voiceModelInitializationTask = CompletableFuture<VoiceModel>()

    init {
        runBackgroundableTask("Loading Model", null, false) {
            val voiceModel = VoiceModel()
            runBackgroundableTask("Checking Microphone", null, false) {
                voiceModel.startRecognition(runOnNewBackgroundThread = false)
            }
            Thread.sleep(MICROPHONE_INITIALIZATION_DURATION.toMillis())
            voiceModel.endRecognition {
                voiceModelInitializationTask.complete(voiceModel)
            }
        }
    }

    fun endRecognition(actionOnRecognizedString: (String) -> Unit) {
        voiceModelInitializationTask.whenComplete { voiceModel, _ ->
            voiceModel.endRecognition(actionOnRecognizedString)
        }
    }

    fun startRecognition() {
        voiceModelInitializationTask.whenComplete { voiceModel, _ ->
            voiceModel.startRecognition(runOnNewBackgroundThread = true)
        }
    }

    private inner class VoiceModel : Disposable {

        init {
            thisLogger().error("Failed initializing model: NOT SUPPORTED")
        }

        private val model = Model(DEFAULT_MODEL)
        private val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000f, 16, 2, 4, 44100f, false)
        private val info = DataLine.Info(TargetDataLine::class.java, format)
        private val microphone: TargetDataLine = AudioSystem.getLine(info) as TargetDataLine
        private val recognizer: Recognizer = Recognizer(model, 120000F)

        private val _isActive = AtomicBoolean(false)
        private val exit = AtomicBoolean(false)

        @Volatile
        private var voiceRecognitionTask = CompletableFuture<String?>()

        val isActive: Boolean get() = _isActive.get()

        init {
            Disposer.register(this@VoiceRecognizer, this)
        }

        fun startRecognition(runOnNewBackgroundThread: Boolean) {
            if (!_isActive.compareAndSet(false, true)) return

            voiceRecognitionTask = voiceRecognitionTask.newIncompleteFuture()
            if (runOnNewBackgroundThread) {
                runBackgroundableTask("Recognizing...", null, false) {
                    startRecognitionLoop()
                }
            } else {
                startRecognitionLoop()
            }
        }

        private fun startRecognitionLoop() {
            try {
                microphone.open(format)
                microphone.start()
                var numBytesRead: Int
                val chunkSize = 2048 * 1
                var bytesRead = 0
                val maxBytes = 100000000
                val b = ByteArray(chunkSize)
                recognizer.reset()

                while (bytesRead <= maxBytes && !exit.get() && _isActive.get()) {
                    numBytesRead = microphone.read(b, 0, chunkSize)
                    bytesRead += numBytesRead
                    recognizer.acceptWaveForm(b, numBytesRead)
                }
                microphone.close()
                _isActive.set(false)

                if (exit.get()) {
                    voiceRecognitionTask.complete(null)
                } else {
                    val result = JSONObject(recognizer.partialResult).getString("partial")
                    voiceRecognitionTask.complete(result)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun endRecognition(actionOnRecognizedString: (String) -> Unit) {
            if (!_isActive.compareAndSet(true, false)) return

            voiceRecognitionTask.whenComplete { recognizedString, _ ->
                if (recognizedString == null) return@whenComplete

                actionOnRecognizedString.invoke(recognizedString)
            }
        }

        override fun dispose() {
            exit.set(true)
            recognizer.close()
            model.close()
        }
    }

    override fun dispose() = Unit
}