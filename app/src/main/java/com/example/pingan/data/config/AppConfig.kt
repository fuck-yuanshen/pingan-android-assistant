package com.example.pingan.data.config

data class ProductConfig(
    val id: String,
    val title: String,
    val tag: String,
    val features: List<String>,
    val price: String,
    val priceSuffix: String,
    val imageResourceName: String,
    val imageUri: String,
    val category: String,
    val details: String,
    val detailUrl: String,
)

data class VideoConfig(
    val id: String,
    val title: String,
    val category: String,
    val rawResourceName: String,
    val videoUri: String,
)

data class AiConfig(
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val systemPrompt: String,
    val thinkingEnabled: Boolean,
    val reasoningEffort: String,
    val quickPrompts: List<String>,
    val voiceAutoPlay: Boolean,
    val backgroundBlurDp: Float,
    val backgroundImageUri: String,
)

data class TtsConfig(
    val enabled: Boolean,
    val endpoint: String,
    val apiKey: String,
    val voice: String,
    val mode: String,
    val languageType: String,
    val sampleRate: Int,
)

data class AsrConfig(
    val enabled: Boolean,
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val sampleRate: Int,
    val chunkFrames: Int,
    val fallbackToOffline: Boolean,
)

data class AppConfig(
    val wakePhrases: List<String>,
    val wakeResponseText: String,
    val products: List<ProductConfig>,
    val videos: List<VideoConfig>,
    val ai: AiConfig,
    val tts: TtsConfig,
    val asr: AsrConfig,
)
