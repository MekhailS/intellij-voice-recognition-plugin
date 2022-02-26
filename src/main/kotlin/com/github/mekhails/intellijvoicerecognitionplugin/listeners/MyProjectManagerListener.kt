package com.github.mekhails.intellijvoicerecognitionplugin.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.github.mekhails.intellijvoicerecognitionplugin.services.MyProjectService
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.SpeechResult
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import org.vosk.Model
import org.vosk.Recognizer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.sound.sampled.AudioSystem

internal class MyProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.service<MyProjectService>()

        System.out.println("START")

        val configuration = Configuration()
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us")
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")
        val recognizer = StreamSpeechRecognizer(configuration)
        val stream: InputStream = FileInputStream(File("test.wav"))
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

//        Model("D:\\dsl_project\\intellij-voice-recognition-plugin\\model").use { model ->
//            AudioSystem.getAudioInputStream(BufferedInputStream(FileInputStream("D:\\dsl_project\\intellij-voice-recognition-plugin\\test.wav")))
//                .use { ais ->
//                    Recognizer(model, 16000f).use { recognizer ->
//                        var nbytes: Int
//                        val b = ByteArray(4096)
//                        while (ais.read(b).also { nbytes = it } >= 0) {
//                            if (recognizer.acceptWaveForm(b, nbytes)) {
//                                println(recognizer.result)
//                            } else {
//                                println(recognizer.partialResult)
//                            }
//                        }
//                        println(recognizer.finalResult)
//                    }
//                }
//        }
    }
}
