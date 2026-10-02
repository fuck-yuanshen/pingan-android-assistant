package com.example.pingan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.config.AppConfigDefaults
import com.example.pingan.data.config.AsrConfig
import com.example.pingan.data.config.TtsConfig
import com.example.pingan.data.model.VideoItem
import com.example.pingan.logic.chat.rememberChatSessionState
import com.example.pingan.ui.components.VideoShowcase
import com.example.pingan.ui.components.VoiceAssistantPanel

@Composable
fun ProductVideosScreen(
    modifier: Modifier = Modifier,
    wakeSignal: Int = 0,
    wakeResponseText: String = AppConfigDefaults.defaultConfig.wakeResponseText,
    videos: List<VideoItem> = emptyList(),
    aiConfig: AiConfig = AppConfigDefaults.defaultConfig.ai,
    ttsConfig: TtsConfig = AppConfigDefaults.defaultConfig.tts,
    asrConfig: AsrConfig = AppConfigDefaults.defaultConfig.asr,
    voiceInputActive: Boolean = false,
    onVoiceLayerVisibilityChange: (Boolean) -> Unit = {},
    onSpeechPlaybackActiveChange: (Boolean) -> Unit = {},
    onVideoPlaybackActiveChange: (Boolean) -> Unit = {},
) {
    val chatState = rememberChatSessionState(includeWelcome = false)
    var voiceOpenSignal by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        VideoShowcase(
            videos = videos,
            voiceInputActive = voiceInputActive,
            onVideoPlaybackActiveChange = onVideoPlaybackActiveChange,
            onHorseQuestionClick = {
                voiceOpenSignal += 1
            },
        )
        VoiceAssistantPanel(
            modifier = Modifier.weight(1f),
            compactTitle = true,
            fillAvailableHeight = true,
            wakeSignal = wakeSignal,
            voiceOpenSignal = voiceOpenSignal,
            wakeResponseText = wakeResponseText,
            aiConfig = aiConfig,
            ttsConfig = ttsConfig,
            asrConfig = asrConfig,
            chatState = chatState,
            onVoiceLayerVisibilityChange = onVoiceLayerVisibilityChange,
            onSpeechPlaybackActiveChange = onSpeechPlaybackActiveChange,
        )
    }
}
