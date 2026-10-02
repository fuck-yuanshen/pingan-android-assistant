package com.example.pingan.logic.voice

object WakePhraseMatcher {
    private val ignoredCharacters = Regex("[\\s,，。.!！?？、~～-]")

    // 唤醒词匹配器统一处理语音识别文本中的空格和标点差异。
    fun containsWakePhrase(text: String, wakePhrase: String): Boolean {
        val normalizedText = text.replace(ignoredCharacters, "").lowercase()
        val normalizedWakePhrase = wakePhrase.replace(ignoredCharacters, "").lowercase()
        return normalizedWakePhrase.isNotBlank() && normalizedText.contains(normalizedWakePhrase)
    }

    fun findMatchedWakePhrase(text: String, wakePhrases: List<String>): String? =
        wakePhrases.firstOrNull { phrase -> containsWakePhrase(text, phrase) }
}
