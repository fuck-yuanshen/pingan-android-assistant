package com.example.pingan.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.config.AppConfigDefaults
import com.example.pingan.data.config.AsrConfig
import com.example.pingan.data.config.TtsConfig
import com.example.pingan.data.model.ChatSender
import com.example.pingan.logic.chat.ChatSessionState
import com.example.pingan.logic.chat.rememberChatSessionState
import com.example.pingan.logic.voice.TextToSpeechSpeaker
import com.example.pingan.ui.theme.BrandBorder
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandOrangeDeep
import com.example.pingan.ui.theme.BrandPrimary
import com.example.pingan.ui.theme.BrandSubtleBorder
import com.example.pingan.ui.theme.BrandSurface
import com.example.pingan.ui.theme.BrandTextSecondary

@Composable
fun VoiceAssistantPanel(
    modifier: Modifier = Modifier,
    compactTitle: Boolean = false,
    fillAvailableHeight: Boolean = false,
    wakeSignal: Int = 0,
    voiceOpenSignal: Int = 0,
    wakeResponseText: String = AppConfigDefaults.defaultConfig.wakeResponseText,
    aiConfig: AiConfig = AppConfigDefaults.defaultConfig.ai,
    ttsConfig: TtsConfig = AppConfigDefaults.defaultConfig.tts,
    asrConfig: AsrConfig = AppConfigDefaults.defaultConfig.asr,
    chatState: ChatSessionState = rememberChatSessionState(includeWelcome = false),
    onVoiceLayerVisibilityChange: (Boolean) -> Unit = {},
    onSpeechPlaybackActiveChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val latestOnSpeechPlaybackActiveChange by rememberUpdatedState(onSpeechPlaybackActiveChange)
    val speaker = remember(context) {
        TextToSpeechSpeaker(context) { active ->
            latestOnSpeechPlaybackActiveChange(active)
        }
    }
    val scope = rememberCoroutineScope()
    var showVoiceLayer by remember { mutableStateOf(false) }
    var voiceRecognitionEnabled by remember { mutableStateOf(true) }
    var voiceStartDelayMillis by remember { mutableStateOf(0L) }
    var wakeResponseToken by remember { mutableStateOf(0L) }
    var handledWakeSignal by remember { mutableIntStateOf(wakeSignal) }
    var handledVoiceOpenSignal by remember { mutableIntStateOf(voiceOpenSignal) }
    val quickPrompts = aiConfig.quickPrompts.ifEmpty { AppConfigDefaults.defaultConfig.ai.quickPrompts }
    var spokenMessageId by remember {
        mutableStateOf(chatState.messages.lastOrNull { it.sender == ChatSender.Assistant }?.id)
    }
    val hasConversation = chatState.messages.isNotEmpty() || chatState.isTyping || chatState.inputText.isNotBlank()

    fun openVoiceLayer() {
        speaker.stop()
        wakeResponseToken += 1L
        voiceRecognitionEnabled = true
        voiceStartDelayMillis = 0L
        showVoiceLayer = true
        onVoiceLayerVisibilityChange(true)
    }

    fun openWakeVoiceLayer() {
        speaker.stop()
        voiceRecognitionEnabled = false
        voiceStartDelayMillis = 0L
        showVoiceLayer = true
        onVoiceLayerVisibilityChange(true)

        val response = wakeResponseText.trim()
        if (response.isBlank()) {
            voiceRecognitionEnabled = true
            return
        }

        val token = wakeResponseToken + 1L
        wakeResponseToken = token
        speaker.speak(response, ttsConfig) {
            if (wakeResponseToken == token && showVoiceLayer) {
                voiceStartDelayMillis = WAKE_RESPONSE_RECOGNITION_DELAY_MILLIS
                voiceRecognitionEnabled = true
            }
        }
    }

    fun closeVoiceLayer() {
        speaker.stop()
        wakeResponseToken += 1L
        voiceRecognitionEnabled = true
        voiceStartDelayMillis = 0L
        showVoiceLayer = false
        onVoiceLayerVisibilityChange(false)
    }

    LaunchedEffect(wakeSignal) {
        if (wakeSignal > 0 && wakeSignal != handledWakeSignal) {
            handledWakeSignal = wakeSignal
            openWakeVoiceLayer()
        }
    }

    LaunchedEffect(voiceOpenSignal) {
        if (voiceOpenSignal > 0 && voiceOpenSignal != handledVoiceOpenSignal) {
            handledVoiceOpenSignal = voiceOpenSignal
            openVoiceLayer()
        }
    }

    LaunchedEffect(showVoiceLayer) {
        onVoiceLayerVisibilityChange(showVoiceLayer)
    }

    LaunchedEffect(chatState.messages.size, chatState.isTyping, aiConfig.voiceAutoPlay, showVoiceLayer) {
        val lastAssistantMessage = chatState.messages.lastOrNull { it.sender == ChatSender.Assistant }
        if (showVoiceLayer) {
            spokenMessageId = lastAssistantMessage?.id ?: spokenMessageId
            return@LaunchedEffect
        }

        if (!aiConfig.voiceAutoPlay) {
            spokenMessageId = lastAssistantMessage?.id ?: spokenMessageId
            return@LaunchedEffect
        }

        if (!chatState.isTyping && lastAssistantMessage != null && lastAssistantMessage.id != spokenMessageId) {
            spokenMessageId = lastAssistantMessage.id
            speaker.speak(lastAssistantMessage.text, ttsConfig)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onVoiceLayerVisibilityChange(false)
            onSpeechPlaybackActiveChange(false)
            speaker.destroy()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val panelHeight = responsiveVoicePanelHeight(maxWidth)
        val panelSizeModifier = if (fillAvailableHeight) {
            Modifier.fillMaxSize()
        } else {
            Modifier.height(panelHeight)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(panelSizeModifier)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF8F1), BrandSurface, BrandSurface),
                    ),
                )
                .border(1.dp, BrandSubtleBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            VoicePanelTitle(compact = compactTitle)

            if (hasConversation) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF7ED).copy(alpha = 0.58f))
                        .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ChatMessageList(
                            messages = chatState.messages,
                            isTyping = chatState.isTyping,
                            compact = true,
                            onSpeakMessage = { speaker.speak(it.text, ttsConfig) },
                        )
                    }
                }
            } else {
                LargeMicrophoneBlock(
                    modifier = Modifier.weight(1f),
                    onClick = ::openVoiceLayer,
                )
            }

            ChatInputBar(
                value = chatState.inputText,
                onValueChange = { chatState.inputText = it },
                onSend = { chatState.submitMessage(scope = scope, aiConfig = aiConfig) },
                onMicClick = ::openVoiceLayer,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    if (showVoiceLayer) {
        VoiceQuestionLayer(
            prompts = quickPrompts,
            recognitionEnabled = voiceRecognitionEnabled,
            startDelayMillis = voiceStartDelayMillis,
            asrConfig = asrConfig,
            ttsApiKeyFallback = ttsConfig.apiKey,
            onPromptSelected = { prompt ->
                closeVoiceLayer()
                chatState.submitMessage(text = prompt, scope = scope, aiConfig = aiConfig)
            },
            onSpeechTextReady = { text ->
                closeVoiceLayer()
                chatState.submitMessage(text = text, scope = scope, aiConfig = aiConfig)
            },
            onDismiss = ::closeVoiceLayer,
        )
    }
}

@Composable
private fun VoicePanelTitle(compact: Boolean) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceBarsTiny()
            Text(
                text = "一句话享受平安金融服务",
                color = BrandPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            VoiceBarsTiny()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentBars(reverse = false)
            Text(
                text = "一句话享受平安金融服务",
                color = BrandPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            AccentBars(reverse = true)
        }
    }
}

@Composable
fun LargeMicrophoneBlock(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ringSize: Dp = 168.dp,
    middleRingSize: Dp = 136.dp,
    buttonSize: Dp = 124.dp,
    iconSize: Dp = 46.dp,
    promptFontSize: TextUnit = 12.sp,
    promptTopPadding: Dp = 14.dp,
    promptText: String = "点击麦克风或呼叫小安小安 开启智能语音资讯",
) {
    val transition = rememberInfiniteTransition(label = "voice-ring")
    val ringScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voice-ring-scale",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.06f)),
            )
            Box(
                modifier = Modifier
                    .size(middleRingSize)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.07f)),
            )
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandOrange, BrandOrangeDeep)))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "启动语音助手",
                    tint = BrandSurface,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Text(
            text = promptText,
            color = BrandTextSecondary,
            fontSize = promptFontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = promptTopPadding)
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun AccentBars(reverse: Boolean) {
    val colors = if (reverse) {
        listOf(BrandOrange, Color(0xFFFFBE85), Color(0xFFFFDCB9))
    } else {
        listOf(Color(0xFFFFDCB9), Color(0xFFFFBE85), BrandOrange)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .size(width = 3.dp, height = 16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun VoiceBarsTiny() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(9.dp, 14.dp, 8.dp).forEach { height ->
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandOrange),
            )
        }
    }
}

private fun responsiveVoicePanelHeight(maxWidth: Dp): Dp = when {
    maxWidth >= 900.dp -> 500.dp
    maxWidth >= 650.dp -> 440.dp
    else -> 360.dp
}

private const val WAKE_RESPONSE_RECOGNITION_DELAY_MILLIS = 350L
