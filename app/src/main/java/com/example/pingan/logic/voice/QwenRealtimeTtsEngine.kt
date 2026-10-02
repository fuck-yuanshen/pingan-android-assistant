package com.example.pingan.logic.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import com.example.pingan.data.config.TtsConfig
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class QwenRealtimeTtsEngine(
    private val onPlaybackActiveChanged: (Boolean) -> Unit,
    private val onAudioStarted: () -> Unit = {},
) {
    private val client = OkHttpClient()
    private val playbackActive = AtomicBoolean(false)
    private val audioStarted = AtomicBoolean(false)
    private val generation = AtomicLong(0L)
    private var webSocket: WebSocket? = null
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private var audioQueue = LinkedBlockingQueue<ByteArray>()
    private var doneCallback: (() -> Unit)? = null
    private var errorCallback: ((Throwable) -> Unit)? = null

    fun speak(
        text: String,
        config: TtsConfig,
        onDone: (() -> Unit)? = null,
        onError: (Throwable) -> Unit,
    ) {
        stop(invokeDone = false)
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            onDone?.invoke()
            return
        }

        val apiKey = config.apiKey.trim()
        val endpoint = config.endpoint.trim()
        if (apiKey.isBlank() || endpoint.isBlank()) {
            onError(IllegalStateException("Qwen TTS endpoint or API key is blank"))
            return
        }

        val currentGeneration = generation.incrementAndGet()
        doneCallback = onDone
        errorCallback = onError
        audioStarted.set(false)
        audioQueue = LinkedBlockingQueue()
        startAudioTrack(config.sampleRate.coerceAtLeast(MIN_SAMPLE_RATE), currentGeneration)
        connectAndSpeak(trimmedText, config, endpoint, apiKey, currentGeneration)
    }

    fun stop(invokeDone: Boolean = false) {
        generation.incrementAndGet()
        runCatching {
            webSocket?.send(
                JSONObject()
                    .put("event_id", eventId())
                    .put("type", "response.cancel")
                    .toString(),
            )
        }
        runCatching { webSocket?.close(1000, "client stop") }
        webSocket = null
        stopAudioTrack()

        val callback = doneCallback
        doneCallback = null
        errorCallback = null
        if (invokeDone) callback?.invoke()
    }

    fun release() {
        stop(invokeDone = false)
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun connectAndSpeak(
        text: String,
        config: TtsConfig,
        endpoint: String,
        apiKey: String,
        currentGeneration: Long,
    ) {
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent(currentGeneration)) return
                sendSessionUpdate(webSocket, config)
                splitText(text).forEach { chunk ->
                    sendText(webSocket, chunk)
                    if (config.mode.equals(MODE_COMMIT, ignoreCase = true)) {
                        sendCommit(webSocket)
                    }
                }
                sendFinish(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrent(currentGeneration)) return
                handleServerEvent(text, currentGeneration)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrent(currentGeneration)) return
                fail(currentGeneration, t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrent(currentGeneration)) return
                finishPlayback(currentGeneration)
            }
        })
    }

    private fun sendSessionUpdate(webSocket: WebSocket, config: TtsConfig) {
        val session = JSONObject()
            .put("voice", config.voice.ifBlank { DEFAULT_VOICE })
            .put("mode", normalizedMode(config.mode))
            .put("response_format", RESPONSE_FORMAT_PCM)
            .put("sample_rate", config.sampleRate.coerceAtLeast(MIN_SAMPLE_RATE))
            .put("language_type", config.languageType.ifBlank { DEFAULT_LANGUAGE_TYPE })

        webSocket.send(
            JSONObject()
                .put("event_id", eventId())
                .put("type", "session.update")
                .put("session", session)
                .toString(),
        )
    }

    private fun sendText(webSocket: WebSocket, text: String) {
        webSocket.send(
            JSONObject()
                .put("event_id", eventId())
                .put("type", "input_text_buffer.append")
                .put("text", text)
                .toString(),
        )
    }

    private fun sendCommit(webSocket: WebSocket) {
        webSocket.send(
            JSONObject()
                .put("event_id", eventId())
                .put("type", "input_text_buffer.commit")
                .toString(),
        )
    }

    private fun sendFinish(webSocket: WebSocket) {
        webSocket.send(
            JSONObject()
                .put("event_id", eventId())
                .put("type", "session.finish")
                .toString(),
        )
    }

    private fun handleServerEvent(rawJson: String, currentGeneration: Long) {
        runCatching { JSONObject(rawJson) }
            .onFailure { fail(currentGeneration, it) }
            .onSuccess { event ->
                when (event.optString("type")) {
                    "response.audio.delta" -> {
                        val delta = event.optString("delta")
                        if (delta.isNotBlank()) {
                            val audioData = Base64.decode(delta, Base64.DEFAULT)
                            if (audioData.isNotEmpty()) {
                                if (audioStarted.compareAndSet(false, true)) {
                                    onAudioStarted()
                                }
                                audioQueue.offer(audioData)
                            }
                        }
                    }
                    "response.done", "session.finished" -> finishPlayback(currentGeneration)
                    "error" -> fail(
                        currentGeneration,
                        IllegalStateException(event.optJSONObject("error")?.optString("message").orEmpty()),
                    )
                }
            }
    }

    private fun startAudioTrack(sampleRate: Int, currentGeneration: Long) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = (minBufferSize.coerceAtLeast(sampleRate) * 2).coerceAtLeast(MIN_BUFFER_SIZE)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        playbackActive.set(true)
        setPlaybackActive(true)
        audioThread = Thread {
            var completedNormally = false
            var writtenFrames = 0L
            runCatching {
                while (isCurrent(currentGeneration) && playbackActive.get()) {
                    val data = audioQueue.take()
                    if (data === STOP_SIGNAL || data.isEmpty()) {
                        completedNormally = playbackActive.get()
                        break
                    }
                    writtenFrames += writePcmData(data)
                }
            }
            if (isCurrent(currentGeneration) && completedNormally) {
                releaseCompletedAudioTrack(
                    writtenFrames = writtenFrames,
                    sampleRate = sampleRate,
                    currentGeneration = currentGeneration,
                )
                val callback = doneCallback
                doneCallback = null
                errorCallback = null
                callback?.invoke()
            }
        }.apply {
            name = "qwen-tts-audio"
            isDaemon = true
            start()
        }
    }

    private fun finishPlayback(currentGeneration: Long) {
        if (!isCurrent(currentGeneration)) return
        if (!audioStarted.get()) {
            fail(currentGeneration, IllegalStateException("Qwen TTS finished without audio data"))
            return
        }
        runCatching { webSocket?.close(1000, "done") }
        webSocket = null
        audioQueue.offer(STOP_SIGNAL)
    }

    private fun writePcmData(data: ByteArray): Long {
        var offset = 0
        var writtenFrames = 0L
        while (offset < data.size && playbackActive.get()) {
            val writtenBytes = audioTrack?.write(data, offset, data.size - offset) ?: break
            if (writtenBytes <= 0) break
            offset += writtenBytes
            writtenFrames += writtenBytes / BYTES_PER_PCM16_MONO_FRAME
        }
        return writtenFrames
    }

    private fun releaseCompletedAudioTrack(
        writtenFrames: Long,
        sampleRate: Int,
        currentGeneration: Long,
    ) {
        waitForBufferedAudioToPlay(
            writtenFrames = writtenFrames,
            sampleRate = sampleRate,
            currentGeneration = currentGeneration,
        )
        playbackActive.set(false)
        audioThread = null
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.release() }
        audioTrack = null
        setPlaybackActive(false)
    }

    private fun waitForBufferedAudioToPlay(
        writtenFrames: Long,
        sampleRate: Int,
        currentGeneration: Long,
    ) {
        val track = audioTrack ?: return
        if (writtenFrames <= 0L || sampleRate <= 0) return

        val initialPlayedFrames = track.playbackHeadPosition.toLong() and UNSIGNED_INT_MASK
        val initialRemainingFrames = (writtenFrames - initialPlayedFrames).coerceAtLeast(0L)
        val maxWaitMs = (
            (initialRemainingFrames * MILLIS_PER_SECOND) / sampleRate +
                PLAYBACK_DRAIN_GRACE_MS
            ).coerceIn(PLAYBACK_DRAIN_GRACE_MS, MAX_PLAYBACK_DRAIN_WAIT_MS)
        val deadline = System.currentTimeMillis() + maxWaitMs

        while (isCurrent(currentGeneration) && playbackActive.get()) {
            val playedFrames = track.playbackHeadPosition.toLong() and UNSIGNED_INT_MASK
            val remainingFrames = writtenFrames - playedFrames
            if (remainingFrames <= 0L) break

            val sleepMs = (
                (remainingFrames * MILLIS_PER_SECOND) / sampleRate
                ).coerceIn(MIN_PLAYBACK_DRAIN_POLL_MS, MAX_PLAYBACK_DRAIN_POLL_MS)
            if (System.currentTimeMillis() >= deadline) break
            Thread.sleep(sleepMs)
        }

        if (isCurrent(currentGeneration) && playbackActive.get()) {
            Thread.sleep(PLAYBACK_DRAIN_GRACE_MS)
        }
    }

    private fun fail(currentGeneration: Long, throwable: Throwable) {
        if (!isCurrent(currentGeneration)) return
        val callback = errorCallback
        doneCallback = null
        errorCallback = null
        runCatching { webSocket?.close(1000, "error") }
        webSocket = null
        stopAudioTrack()
        callback?.invoke(throwable)
    }

    private fun stopAudioTrack() {
        playbackActive.set(false)
        audioQueue.clear()
        audioQueue.offer(STOP_SIGNAL)
        if (Thread.currentThread() != audioThread) {
            runCatching { audioThread?.join(300L) }
        }
        audioThread = null
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.release() }
        audioTrack = null
        setPlaybackActive(false)
    }

    private fun setPlaybackActive(active: Boolean) {
        onPlaybackActiveChanged(active)
    }

    private fun isCurrent(currentGeneration: Long): Boolean =
        generation.get() == currentGeneration

    private fun splitText(text: String): List<String> {
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        text.trim().forEach { char ->
            current.append(char)
            if (TEXT_SEPARATORS.contains(char)) {
                chunks += current.toString()
                current.clear()
            }
        }
        if (current.isNotBlank()) {
            chunks += current.toString()
        }
        return chunks.ifEmpty { listOf(text) }
    }

    private fun eventId(): String =
        "event_${UUID.randomUUID().toString().replace("-", "")}"

    private fun normalizedMode(mode: String): String =
        if (mode.equals(MODE_COMMIT, ignoreCase = true)) MODE_COMMIT else MODE_SERVER_COMMIT

    private companion object {
        const val DEFAULT_VOICE = "Cherry"
        const val DEFAULT_LANGUAGE_TYPE = "Chinese"
        const val MODE_SERVER_COMMIT = "server_commit"
        const val MODE_COMMIT = "commit"
        const val RESPONSE_FORMAT_PCM = "pcm"
        const val MIN_SAMPLE_RATE = 8000
        const val MIN_BUFFER_SIZE = 4096
        const val BYTES_PER_PCM16_MONO_FRAME = 2
        const val MILLIS_PER_SECOND = 1000L
        const val MIN_PLAYBACK_DRAIN_POLL_MS = 10L
        const val MAX_PLAYBACK_DRAIN_POLL_MS = 80L
        const val PLAYBACK_DRAIN_GRACE_MS = 160L
        const val MAX_PLAYBACK_DRAIN_WAIT_MS = 5000L
        const val UNSIGNED_INT_MASK = 0xFFFFFFFFL
        val STOP_SIGNAL = ByteArray(0)
        val TEXT_SEPARATORS = setOf('，', '。', '！', '？', '；', ',', '.', '!', '?', ';')
    }
}
