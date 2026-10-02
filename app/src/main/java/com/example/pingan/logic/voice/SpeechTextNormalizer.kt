package com.example.pingan.logic.voice

object SpeechTextNormalizer {
    private val whitespace = Regex("\\s+")
    private val cjkInnerWhitespace = Regex("(?<=[\\u4e00-\\u9fff])\\s+(?=[\\u4e00-\\u9fff])")

    fun normalize(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return ""

        return trimmed
            .replace(whitespace, " ")
            .replace(cjkInnerWhitespace, "")
            .trim()
    }
}
