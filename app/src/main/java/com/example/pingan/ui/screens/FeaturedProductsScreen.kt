package com.example.pingan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.config.AppConfigDefaults
import com.example.pingan.data.config.AsrConfig
import com.example.pingan.data.config.TtsConfig
import com.example.pingan.data.model.Product
import com.example.pingan.ui.components.ProductShowcase
import com.example.pingan.ui.components.VoiceAssistantPanel

@Composable
fun FeaturedProductsScreen(
    modifier: Modifier = Modifier,
    wakeSignal: Int = 0,
    wakeResponseText: String = AppConfigDefaults.defaultConfig.wakeResponseText,
    products: List<Product> = emptyList(),
    aiConfig: AiConfig = AppConfigDefaults.defaultConfig.ai,
    ttsConfig: TtsConfig = AppConfigDefaults.defaultConfig.tts,
    asrConfig: AsrConfig = AppConfigDefaults.defaultConfig.asr,
    onVoiceLayerVisibilityChange: (Boolean) -> Unit = {},
    onSpeechPlaybackActiveChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProductShowcase(products = products)
        VoiceAssistantPanel(
            modifier = Modifier.weight(1f),
            fillAvailableHeight = true,
            wakeSignal = wakeSignal,
            wakeResponseText = wakeResponseText,
            aiConfig = aiConfig,
            ttsConfig = ttsConfig,
            asrConfig = asrConfig,
            onVoiceLayerVisibilityChange = onVoiceLayerVisibilityChange,
            onSpeechPlaybackActiveChange = onSpeechPlaybackActiveChange,
        )
    }
}
