package com.example.pingan.logic.voice

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

class WakePhraseRecognizer(
    context: Context,
    wakePhrases: List<String>,
    private val onWakeDetected: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val wakePhrases = wakePhrases.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var started = false
    private var startVersion = 0
    private var lastWakeAt = 0L

    fun start() {
        if (wakePhrases.isEmpty()) return
        if (started) return

        started = true
        val version = ++startVersion
        scope.launch {
            runCatching {
                val readyModel = withContext(Dispatchers.IO) {
                    LibVosk.setLogLevel(LogLevel.WARNINGS)
                    Model(VoskModelStore.prepareModelDir(appContext).absolutePath)
                }
                if (!started || version != startVersion) {
                    readyModel.close()
                    return@launch
                }
                model = readyModel
                startListening(readyModel)
            }.onFailure {
                stop()
            }
        }
    }

    fun stop() {
        started = false
        startVersion += 1
        runCatching { speechService?.stop() }
        runCatching { speechService?.shutdown() }
        speechService = null
        runCatching { model?.close() }
        model = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun startListening(readyModel: Model) {
        if (!started) return

        val recognizer = Recognizer(readyModel, VoskModelStore.SAMPLE_RATE)
        speechService = SpeechService(recognizer, VoskModelStore.SAMPLE_RATE).also { service ->
            service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    inspectHypothesis(hypothesis, "partial")
                }

                override fun onResult(hypothesis: String?) {
                    inspectHypothesis(hypothesis, "text")
                }

                override fun onFinalResult(hypothesis: String?) {
                    inspectHypothesis(hypothesis, "text")
                }

                override fun onError(exception: Exception?) {
                    if (started) {
                        stop()
                    }
                }

                override fun onTimeout() = Unit
            })
        }
    }

    private fun inspectHypothesis(hypothesis: String?, key: String) {
        val recognizedText = hypothesis
            ?.let { runCatching { JSONObject(it).optString(key) }.getOrDefault("") }
            ?.let(SpeechTextNormalizer::normalize)
            .orEmpty()

        if (WakePhraseMatcher.findMatchedWakePhrase(recognizedText, wakePhrases) != null) {
            handleWakeDetected(recognizedText)
        }
    }

    private fun handleWakeDetected(recognizedText: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWakeAt < WAKE_COOLDOWN_MILLIS) return

        lastWakeAt = now
        scope.launch {
            onWakeDetected(recognizedText)
            stop()
        }
    }

    private companion object {
        const val WAKE_COOLDOWN_MILLIS = 3_000L
    }
}
