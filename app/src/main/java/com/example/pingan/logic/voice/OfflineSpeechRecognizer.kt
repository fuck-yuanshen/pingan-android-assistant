package com.example.pingan.logic.voice

import android.content.Context
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

enum class OfflineSpeechStatus {
    Preparing,
    Listening,
    Error,
}

class OfflineSpeechRecognizer(
    context: Context,
    private val onStatusChanged: (OfflineSpeechStatus, String) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var started = false

    fun start() {
        if (started) return

        started = true
        onStatusChanged(OfflineSpeechStatus.Preparing, "正在加载离线语音模型")
        scope.launch {
            runCatching {
                val readyModel = withContext(Dispatchers.IO) {
                    LibVosk.setLogLevel(LogLevel.WARNINGS)
                    model ?: Model(VoskModelStore.prepareModelDir(appContext).absolutePath).also { model = it }
                }
                startListening(readyModel)
            }.onFailure { error ->
                onStatusChanged(
                    OfflineSpeechStatus.Error,
                    error.message ?: "离线语音模型加载失败",
                )
            }
        }
    }

    fun stop() {
        started = false
        runCatching { speechService?.stop() }
    }

    fun destroy() {
        stop()
        runCatching { speechService?.shutdown() }
        speechService = null
        runCatching { model?.close() }
        model = null
        scope.cancel()
    }

    private fun startListening(readyModel: Model) {
        if (!started) return

        runCatching { speechService?.shutdown() }
        val recognizer = Recognizer(readyModel, VoskModelStore.SAMPLE_RATE)
        speechService = SpeechService(recognizer, VoskModelStore.SAMPLE_RATE).also { service ->
            service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    emitText(hypothesis, "partial", onPartialText)
                }

                override fun onResult(hypothesis: String?) {
                    emitText(hypothesis, "text", onFinalText)
                }

                override fun onFinalResult(hypothesis: String?) {
                    emitText(hypothesis, "text", onFinalText)
                }

                override fun onError(exception: Exception?) {
                    emitStatus(
                        OfflineSpeechStatus.Error,
                        exception?.message ?: "离线语音识别失败",
                    )
                }

                override fun onTimeout() = Unit
            })
        }
        emitStatus(OfflineSpeechStatus.Listening, "你好我是小安，正在聆听")
    }

    private fun emitText(
        hypothesis: String?,
        jsonKey: String,
        onText: (String) -> Unit,
    ) {
        val text = hypothesis
            ?.let { runCatching { JSONObject(it).optString(jsonKey) }.getOrDefault("") }
            ?.let(SpeechTextNormalizer::normalize)
            .orEmpty()

        if (text.isNotBlank()) {
            scope.launch {
                onText(text)
            }
        }
    }

    private fun emitStatus(status: OfflineSpeechStatus, message: String) {
        scope.launch {
            onStatusChanged(status, message)
        }
    }
}
