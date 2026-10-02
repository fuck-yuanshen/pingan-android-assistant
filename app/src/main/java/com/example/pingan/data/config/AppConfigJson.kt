package com.example.pingan.data.config

import org.json.JSONArray
import org.json.JSONObject

object AppConfigJson {
    fun encode(config: AppConfig): String {
        val wakePhrases = WakePhraseConfig.normalize(config.wakePhrases)
        return JSONObject()
            .put("wakePhrase", wakePhrases.firstOrNull().orEmpty())
            .put("wakePhrases", JSONArray(wakePhrases))
            .put("wakeResponseText", config.wakeResponseText)
            .put("products", JSONArray().apply {
                config.products.forEach { product ->
                    put(JSONObject()
                        .put("id", product.id)
                        .put("title", product.title)
                        .put("tag", product.tag)
                        .put("features", JSONArray(product.features))
                        .put("price", product.price)
                        .put("priceSuffix", product.priceSuffix)
                        .put("imageResourceName", product.imageResourceName)
                        .put("imageUri", product.imageUri)
                        .put("category", product.category)
                        .put("details", product.details)
                        .put("detailUrl", product.detailUrl))
                }
            })
            .put("videos", JSONArray().apply {
                config.videos.forEach { video ->
                    put(JSONObject()
                        .put("id", video.id)
                        .put("title", video.title)
                        .put("category", video.category)
                        .put("rawResourceName", video.rawResourceName)
                        .put("videoUri", video.videoUri))
                }
            })
            .put("ai", JSONObject()
                .put("endpoint", config.ai.endpoint)
                .put("apiKey", config.ai.apiKey)
                .put("model", config.ai.model)
                .put("systemPrompt", config.ai.systemPrompt)
                .put("thinkingEnabled", config.ai.thinkingEnabled)
                .put("reasoningEffort", config.ai.reasoningEffort)
                .put("quickPrompts", JSONArray(QuickPromptConfig.normalize(config.ai.quickPrompts)))
                .put("voiceAutoPlay", config.ai.voiceAutoPlay)
                .put("backgroundBlurDp", config.ai.backgroundBlurDp.toDouble())
                .put("backgroundImageUri", config.ai.backgroundImageUri))
            .put("tts", JSONObject()
                .put("enabled", config.tts.enabled)
                .put("endpoint", config.tts.endpoint)
                .put("apiKey", config.tts.apiKey)
                .put("voice", config.tts.voice)
                .put("mode", config.tts.mode)
                .put("languageType", config.tts.languageType)
                .put("sampleRate", config.tts.sampleRate))
            .put("asr", JSONObject()
                .put("enabled", config.asr.enabled)
                .put("endpoint", config.asr.endpoint)
                .put("apiKey", config.asr.apiKey)
                .put("model", config.asr.model)
                .put("sampleRate", config.asr.sampleRate)
                .put("chunkFrames", config.asr.chunkFrames)
                .put("fallbackToOffline", config.asr.fallbackToOffline))
            .toString()
    }

    fun decode(raw: String): AppConfig {
        val root = JSONObject(raw)
        val fallback = AppConfigDefaults.defaultConfig
        val productsJson = root.optJSONArray("products") ?: JSONArray()
        val videosJson = root.optJSONArray("videos") ?: JSONArray()
        val aiJson = root.optJSONObject("ai") ?: JSONObject()
        val ttsJson = root.optJSONObject("tts") ?: JSONObject()
        val asrJson = root.optJSONObject("asr") ?: JSONObject()
        val wakePhrases = WakePhraseConfig.normalize(
            (root.optJSONArray("wakePhrases").toStringList())
                .ifEmpty { WakePhraseConfig.parse(root.optString("wakePhrase")) },
        ).ifEmpty { fallback.wakePhrases }

        return AppConfig(
            wakePhrases = wakePhrases,
            wakeResponseText = root.optString("wakeResponseText", fallback.wakeResponseText),
            products = List(productsJson.length()) { index ->
                val item = productsJson.getJSONObject(index)
                ProductConfig(
                    id = item.optString("id", "product_$index"),
                    title = item.optString("title"),
                    tag = item.optString("tag"),
                    features = item.optJSONArray("features").toStringList(),
                    price = item.optString("price"),
                    priceSuffix = item.optString("priceSuffix"),
                    imageResourceName = item.optString("imageResourceName"),
                    imageUri = item.optString("imageUri"),
                    category = item.optString("category"),
                    details = item.optString("details"),
                    detailUrl = item.optString("detailUrl"),
                )
            }.ifEmpty { fallback.products },
            videos = List(videosJson.length()) { index ->
                val item = videosJson.getJSONObject(index)
                VideoConfig(
                    id = item.optString("id", "video_$index"),
                    title = item.optString("title"),
                    category = item.optString("category"),
                    rawResourceName = item.optString("rawResourceName"),
                    videoUri = item.optString("videoUri"),
                )
            }.ifEmpty { fallback.videos },
            ai = AiConfig(
                endpoint = aiJson.optString("endpoint", fallback.ai.endpoint),
                apiKey = aiJson.optString("apiKey", fallback.ai.apiKey),
                model = aiJson.optString("model", fallback.ai.model),
                systemPrompt = aiJson.optString("systemPrompt", fallback.ai.systemPrompt),
                thinkingEnabled = aiJson.optBoolean("thinkingEnabled", fallback.ai.thinkingEnabled),
                reasoningEffort = aiJson.optString("reasoningEffort", fallback.ai.reasoningEffort),
                quickPrompts = QuickPromptConfig.normalize(aiJson.optJSONArray("quickPrompts").toStringList())
                    .ifEmpty { fallback.ai.quickPrompts },
                voiceAutoPlay = aiJson.optBoolean("voiceAutoPlay", fallback.ai.voiceAutoPlay),
                backgroundBlurDp = aiJson.optDouble(
                    "backgroundBlurDp",
                    fallback.ai.backgroundBlurDp.toDouble(),
                ).toFloat(),
                backgroundImageUri = aiJson.optString("backgroundImageUri", fallback.ai.backgroundImageUri),
            ),
            tts = TtsConfig(
                enabled = ttsJson.optBoolean("enabled", fallback.tts.enabled),
                endpoint = ttsJson.optString("endpoint", fallback.tts.endpoint),
                apiKey = ttsJson.optString("apiKey", fallback.tts.apiKey),
                voice = ttsJson.optString("voice", fallback.tts.voice),
                mode = ttsJson.optString("mode", fallback.tts.mode),
                languageType = ttsJson.optString("languageType", fallback.tts.languageType),
                sampleRate = ttsJson.optInt("sampleRate", fallback.tts.sampleRate),
            ),
            asr = AsrConfig(
                enabled = asrJson.optBoolean("enabled", fallback.asr.enabled),
                endpoint = asrJson.optString("endpoint", fallback.asr.endpoint),
                apiKey = asrJson.optString("apiKey", fallback.asr.apiKey),
                model = asrJson.optString("model", fallback.asr.model),
                sampleRate = asrJson.optInt("sampleRate", fallback.asr.sampleRate),
                chunkFrames = asrJson.optInt("chunkFrames", fallback.asr.chunkFrames),
                fallbackToOffline = asrJson.optBoolean("fallbackToOffline", fallback.asr.fallbackToOffline),
            ),
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }
}
