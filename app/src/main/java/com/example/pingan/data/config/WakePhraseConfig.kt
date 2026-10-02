package com.example.pingan.data.config

object WakePhraseConfig {
    private val separators = Regex("[,，、;；\\r\\n]+")

    fun parse(rawText: String): List<String> =
        normalize(rawText.split(separators))

    fun normalize(phrases: List<String>): List<String> =
        phrases
            .flatMap { it.split(separators) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    fun display(phrases: List<String>): String =
        normalize(phrases).joinToString("\n")
}
