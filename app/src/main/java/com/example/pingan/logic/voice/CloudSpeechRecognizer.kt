package com.example.pingan.logic.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.pingan.data.config.AsrConfig
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

class CloudSpeechRecognizer(
    context: Context,
    private val asrConfig: AsrConfig,
    private val ttsApiKeyFallback: String,
    private val onStatusChanged: (OfflineSpeechStatus, String) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    private val active = AtomicBoolean(false)
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    private var taskId = ""
    private var latestText = ""
    private var finalText = ""
    private var autoStopFuture: ScheduledFuture<*>? = null

    fun start() {
        if (active.getAndSet(true)) return

        val apiKey = activeApiKey()
        if (apiKey.isBlank()) {
            fail("云端 ASR API Key 为空")
            return
        }
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail("缺少麦克风权限")
            return
        }

        taskId = UUID.randomUUID().toString().replace("-", "")
        emitStatus(OfflineSpeechStatus.Preparing, "正在连接云端语音识别")

        val request = Request.Builder()
            .url(asrConfig.endpoint.ifBlank { DEFAULT_ENDPOINT })
            .addHeader("Authorization", "bearer $apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!active.get()) return
                webSocket.send(buildRunTaskMessage())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!active.get()) return
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!active.get()) return
                fail(t.message ?: "云端 ASR 连接失败")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!active.get()) return
                finishWithBestText()
            }
        })
    }

    fun stop() {
        if (!active.getAndSet(false)) return
        autoStopFuture?.cancel(false)
        autoStopFuture = null
        stopAudioCapture()
        runCatching { webSocket?.send(buildFinishTaskMessage()) }
        runCatching { webSocket?.close(1000, "client stop") }
        webSocket = null
    }

    fun destroy() {
        stop()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        executor.shutdownNow()
    }

    private fun handleServerMessage(rawJson: String) {
        runCatching { JSONObject(rawJson) }
            .onFailure { fail(it.message ?: "云端 ASR 返回格式错误") }
            .onSuccess { message ->
                val header = message.optJSONObject("header") ?: JSONObject()
                when (header.optString("event")) {
                    EVENT_TASK_STARTED -> {
                        emitStatus(OfflineSpeechStatus.Listening, "云端语音识别正在聆听")
                        startAudioCapture()
                        scheduleAutoStop()
                    }

                    EVENT_RESULT_GENERATED -> handleRecognitionPayload(message.optJSONObject("payload"))
                    EVENT_TASK_FINISHED -> finishWithBestText()
                    EVENT_TASK_FAILED -> fail(
                        header.optString("error_message").ifBlank { "云端 ASR 识别失败" },
                    )
                }
            }
    }

    private fun handleRecognitionPayload(payload: JSONObject?) {
        val sentence = payload?.optJSONObject("output")?.optJSONObject("sentence") ?: return
        val text = SpeechTextNormalizer.normalize(sentence.optString("text"))
        if (text.isBlank()) return

        latestText = text
        emitPartial(text)
        if (!sentence.isNull("end_time")) {
            finalText += text
            emitFinal(finalText)
        }
    }

    private fun startAudioCapture() {
        val sampleRate = asrConfig.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE
        val chunkFrames = asrConfig.chunkFrames.takeIf { it > 0 } ?: DEFAULT_CHUNK_FRAMES
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = (chunkFrames * BYTES_PER_PCM16_MONO_FRAME)
            .coerceAtLeast(minBufferSize)
            .coerceAtLeast(MIN_AUDIO_BUFFER_BYTES)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        val recorder = audioRecord ?: return
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            fail("麦克风初始化失败")
            return
        }

        recorder.startRecording()
        audioThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (active.get()) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    webSocket?.send(buffer.toByteString(0, read))
                }
            }
        }.apply {
            name = "cloud-asr-audio"
            isDaemon = true
            start()
        }
    }

    private fun stopAudioCapture() {
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        if (Thread.currentThread() != audioThread) {
            runCatching { audioThread?.join(300L) }
        }
        audioThread = null
    }

    private fun scheduleAutoStop() {
        autoStopFuture?.cancel(false)
        autoStopFuture = executor.schedule({
            if (active.get()) {
                stopAudioCapture()
                runCatching { webSocket?.send(buildFinishTaskMessage()) }
            }
        }, AUTO_STOP_SECONDS, TimeUnit.SECONDS)
    }

    private fun finishWithBestText() {
        val text = finalText.ifBlank { latestText }.trim()
        if (text.isNotBlank()) {
            emitFinal(text)
        }
        stop()
    }

    private fun fail(message: String) {
        val wasActive = active.getAndSet(false)
        autoStopFuture?.cancel(false)
        autoStopFuture = null
        stopAudioCapture()
        runCatching { webSocket?.close(1000, "error") }
        webSocket = null
        if (wasActive) {
            emitStatus(OfflineSpeechStatus.Error, message)
            mainHandler.post { onError(message) }
        }
    }

    private fun buildRunTaskMessage(): String =
        JSONObject()
            .put(
                "header",
                JSONObject()
                    .put("action", ACTION_RUN_TASK)
                    .put("task_id", taskId)
                    .put("streaming", "duplex"),
            )
            .put(
                "payload",
                JSONObject()
                    .put("model", asrConfig.model.ifBlank { DEFAULT_MODEL })
                    .put("task_group", "audio")
                    .put("task", "asr")
                    .put("function", "recognition")
                    .put("input", JSONObject())
                    .put(
                        "parameters",
                        JSONObject()
                            .put("sample_rate", asrConfig.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE)
                            .put("format", "pcm"),
                    ),
            )
            .toString()

    private fun buildFinishTaskMessage(): String =
        JSONObject()
            .put(
                "header",
                JSONObject()
                    .put("action", ACTION_FINISH_TASK)
                    .put("task_id", taskId)
                    .put("streaming", "duplex"),
            )
            .put("payload", JSONObject().put("input", JSONObject()))
            .toString()

    private fun activeApiKey(): String =
        asrConfig.apiKey.ifBlank { ttsApiKeyFallback }.trim()

    private fun emitStatus(status: OfflineSpeechStatus, message: String) {
        mainHandler.post { onStatusChanged(status, message) }
    }

    private fun emitPartial(text: String) {
        mainHandler.post { onPartialText(text) }
    }

    private fun emitFinal(text: String) {
        mainHandler.post { onFinalText(text) }
    }

    private companion object {
        const val DEFAULT_ENDPOINT = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
        const val DEFAULT_MODEL = "fun-asr-realtime"
        const val DEFAULT_SAMPLE_RATE = 16000
        const val DEFAULT_CHUNK_FRAMES = 3200
        const val BYTES_PER_PCM16_MONO_FRAME = 2
        const val MIN_AUDIO_BUFFER_BYTES = 6400
        const val AUTO_STOP_SECONDS = 8L
        const val ACTION_RUN_TASK = "run-task"
        const val ACTION_FINISH_TASK = "finish-task"
        const val EVENT_TASK_STARTED = "task-started"
        const val EVENT_RESULT_GENERATED = "result-generated"
        const val EVENT_TASK_FINISHED = "task-finished"
        const val EVENT_TASK_FAILED = "task-failed"
    }
}
