package com.example.pingan.logic.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import com.example.pingan.data.config.TtsConfig
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class TextToSpeechSpeaker(
    context: Context,
    private val onPlaybackActiveChanged: (Boolean) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private var failed = false
    private var pendingText = ""
    private var pendingOnDone: (() -> Unit)? = null
    private val doneCallbacks = mutableMapOf<String, () -> Unit>()
    private var tts: TextToSpeech? = null
    private var engineGeneration = 0
    private var utteranceCounter = 0L
    private var playbackActive = false
    private val cloudSpeechAttemptId = AtomicLong(0L)
    private val qwenEngine = QwenRealtimeTtsEngine(
        onPlaybackActiveChanged = { active -> setPlaybackActive(active) },
        onAudioStarted = { cloudSpeechAttemptId.incrementAndGet() },
    )

    init {
        createEngine(GOOGLE_TTS_ENGINE)
    }

    fun speak(text: String, ttsConfig: TtsConfig? = null, onDone: (() -> Unit)? = null) {
        val spokenText = sanitizeSpeechText(text)
        if (spokenText.isEmpty()) {
            onDone?.invoke()
            return
        }

        stop()

        if (ttsConfig != null && ttsConfig.enabled && ttsConfig.apiKey.isNotBlank()) {
            val attemptId = cloudSpeechAttemptId.incrementAndGet()
            mainHandler.postDelayed({
                if (cloudSpeechAttemptId.get() == attemptId) {
                    cloudSpeechAttemptId.incrementAndGet()
                    qwenEngine.stop()
                    speakWithSystemTts(spokenText, onDone)
                }
            }, CLOUD_TTS_FIRST_AUDIO_TIMEOUT_MILLIS)
            qwenEngine.speak(
                text = spokenText,
                config = ttsConfig,
                onDone = {
                    cloudSpeechAttemptId.incrementAndGet()
                    onDone?.invoke()
                },
                onError = {
                    cloudSpeechAttemptId.incrementAndGet()
                    showToast("云端语音播报失败，已切换系统语音")
                    mainHandler.post {
                        speakWithSystemTts(spokenText, onDone)
                    }
                },
            )
            return
        }

        speakWithSystemTts(spokenText, onDone)
    }

    private fun speakWithSystemTts(spokenText: String, onDone: (() -> Unit)? = null) {
        if (failed) {
            showToast("系统语音播报不可用，请在系统设置中启用文字转语音引擎")
            onDone?.invoke()
            return
        }

        if (!ready) {
            pendingText = spokenText
            pendingOnDone = onDone
            showToast("语音播报正在准备，稍后自动播放")
            return
        }

        speakNow(spokenText, onDone)
    }

    fun stop() {
        cloudSpeechAttemptId.incrementAndGet()
        pendingText = ""
        pendingOnDone = null
        clearDoneCallbacks()
        qwenEngine.stop()
        tts?.stop()
        setPlaybackActive(false)
    }

    fun destroy() {
        cloudSpeechAttemptId.incrementAndGet()
        pendingText = ""
        pendingOnDone = null
        clearDoneCallbacks()
        engineGeneration += 1
        qwenEngine.release()
        tts?.stop()
        tts?.shutdown()
        tts = null
        setPlaybackActive(false)
    }

    private fun createEngine(enginePackage: String?) {
        ready = false
        failed = false
        val generation = ++engineGeneration
        runCatching { tts?.shutdown() }

        val listener = TextToSpeech.OnInitListener { status ->
            mainHandler.post {
                handleInitResult(
                    status = status,
                    requestedEngine = enginePackage,
                    generation = generation,
                )
            }
        }

        tts = if (enginePackage == null) {
            TextToSpeech(appContext, listener)
        } else {
            TextToSpeech(appContext, listener, enginePackage)
        }
    }

    private fun handleInitResult(
        status: Int,
        requestedEngine: String?,
        generation: Int,
    ) {
        if (generation != engineGeneration) return

        if (status == TextToSpeech.SUCCESS) {
            configureEngine()
            configureProgressListener()
            ready = true
            failed = false
            flushPendingText()
            return
        }

        if (requestedEngine == GOOGLE_TTS_ENGINE) {
            createEngine(enginePackage = null)
            return
        }

        failed = true
        ready = false
        val pendingCallback = pendingOnDone
        pendingText = ""
        pendingOnDone = null
        showToast("语音播报初始化失败，请安装或启用 Google 文字转语音引擎")
        pendingCallback?.invoke()
    }

    private fun configureEngine() {
        val engine = tts ?: return
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        engine.setSpeechRate(DEFAULT_SPEECH_RATE)

        val languageSet = listOf(
            Locale.CHINA,
            Locale.SIMPLIFIED_CHINESE,
            Locale.getDefault(),
            Locale.US,
        ).any { locale ->
            val result = engine.setLanguage(locale)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }

        if (!languageSet) {
            showToast("当前文字转语音引擎缺少中文语音包，将尝试使用默认语音")
        }
    }

    private fun flushPendingText() {
        val text = pendingText
        val onDone = pendingOnDone
        pendingText = ""
        pendingOnDone = null
        if (text.isNotBlank()) {
            speakNow(text, onDone)
        }
    }

    private fun speakNow(text: String, onDone: (() -> Unit)?) {
        tts?.stop()
        clearDoneCallbacks()
        val utteranceId = "pingan-${++utteranceCounter}-${System.currentTimeMillis()}"
        if (onDone != null) {
            storeDoneCallback(utteranceId, onDone)
        }
        setPlaybackActive(true)
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            removeDoneCallback(utteranceId)
            setPlaybackActive(false)
            showToast("语音播报失败，请检查媒体音量或系统 TTS 引擎")
            onDone?.invoke()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private fun configureProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                setPlaybackActive(true)
            }

            override fun onDone(utteranceId: String?) {
                setPlaybackActive(false)
                runDoneCallback(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                setPlaybackActive(false)
                runDoneCallback(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                setPlaybackActive(false)
                runDoneCallback(utteranceId)
            }
        })
    }

    private fun setPlaybackActive(active: Boolean) {
        if (playbackActive == active) return
        playbackActive = active
        mainHandler.post {
            onPlaybackActiveChanged(active)
        }
    }

    private fun runDoneCallback(utteranceId: String?) {
        val callback = removeDoneCallback(utteranceId) ?: return
        mainHandler.post { callback() }
    }

    private fun storeDoneCallback(utteranceId: String, callback: () -> Unit) {
        synchronized(doneCallbacks) {
            doneCallbacks[utteranceId] = callback
        }
    }

    private fun removeDoneCallback(utteranceId: String?): (() -> Unit)? {
        if (utteranceId == null) return null
        return synchronized(doneCallbacks) {
            doneCallbacks.remove(utteranceId)
        }
    }

    private fun clearDoneCallbacks() {
        synchronized(doneCallbacks) {
            doneCallbacks.clear()
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sanitizeSpeechText(text: String): String {
        return text
            .replace(MARKDOWN_LINK_REGEX) { matchResult ->
                matchResult.groupValues.getOrNull(1).orEmpty()
            }
            .replace(WRAPPED_URL_REGEX, "")
            .replace(AUTO_LINK_REGEX, "")
            .replace(BARE_URL_REGEX, "")
            .replace(DANGLING_LINK_LABEL_REGEX, "")
            .replace(HORIZONTAL_SPACE_REGEX, " ")
            .replace(LINE_SPACE_REGEX, "\n")
            .replace(BLANK_LINES_REGEX, "\n\n")
            .trim()
    }

    private companion object {
        const val GOOGLE_TTS_ENGINE = "com.google.android.tts"
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val CLOUD_TTS_FIRST_AUDIO_TIMEOUT_MILLIS = 3_500L
        val MARKDOWN_LINK_REGEX = Regex(
            pattern = """\[([^\]\n]+)]\(\s*<?(?:https?://|www\.)[^\s>)]+>?(?:\s+["'][^"']*["'])?\s*\)""",
            option = RegexOption.IGNORE_CASE,
        )
        val WRAPPED_URL_REGEX = Regex(
            pattern = """[（(\[]\s*(?:https?://|www\.)[^\s<>"'，。！？；：、（）()\]]+\s*[）)\]]""",
            option = RegexOption.IGNORE_CASE,
        )
        val AUTO_LINK_REGEX = Regex(
            pattern = """<\s*(?:https?://|www\.)[^>\s]+\s*>""",
            option = RegexOption.IGNORE_CASE,
        )
        val BARE_URL_REGEX = Regex(
            pattern = """\b(?:https?://|www\.)[^\s<>"',，。！？；：、（）()]+""",
            option = RegexOption.IGNORE_CASE,
        )
        val DANGLING_LINK_LABEL_REGEX = Regex("""(?m)^[ \t]*(?:链接|网址|地址|URL)[：:][ \t]*$""")
        val HORIZONTAL_SPACE_REGEX = Regex("""[ \t\u00A0]{2,}""")
        val LINE_SPACE_REGEX = Regex("""[ \t]*\n[ \t]*""")
        val BLANK_LINES_REGEX = Regex("""\n{3,}""")
    }
}
