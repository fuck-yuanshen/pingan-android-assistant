package com.example.pingan.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pingan.R
import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.config.AppConfigDefaults
import com.example.pingan.data.config.AsrConfig
import com.example.pingan.data.config.TtsConfig
import com.example.pingan.data.model.ChatSender
import com.example.pingan.logic.chat.rememberChatSessionState
import com.example.pingan.logic.image.decodeImageBitmapFromUri
import com.example.pingan.logic.voice.TextToSpeechSpeaker
import com.example.pingan.ui.components.ChatInputBar
import com.example.pingan.ui.components.ChatMessageList
import com.example.pingan.ui.components.LargeMicrophoneBlock
import com.example.pingan.ui.components.QuickPromptRow
import com.example.pingan.ui.components.VoiceQuestionLayer
import com.example.pingan.ui.theme.BrandBorder
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandPrimary
import com.example.pingan.ui.theme.BrandSurface
import com.example.pingan.ui.theme.BrandText
import com.example.pingan.ui.theme.BrandTextSecondary
import com.example.pingan.ui.theme.BrandSuccessBackground
import com.example.pingan.ui.theme.BrandSuccessText
import kotlinx.coroutines.launch

@Composable
fun AiChatScreen(
    modifier: Modifier = Modifier,
    wakeSignal: Int = 0,
    wakeResponseText: String = AppConfigDefaults.defaultConfig.wakeResponseText,
    aiConfig: AiConfig = AppConfigDefaults.defaultConfig.ai,
    ttsConfig: TtsConfig = AppConfigDefaults.defaultConfig.tts,
    asrConfig: AsrConfig = AppConfigDefaults.defaultConfig.asr,
    backgroundBlurDp: Float = AppConfigDefaults.defaultConfig.ai.backgroundBlurDp,
    backgroundImageUri: String = AppConfigDefaults.defaultConfig.ai.backgroundImageUri,
    onVoiceLayerVisibilityChange: (Boolean) -> Unit = {},
    onSpeechPlaybackActiveChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val chatState = rememberChatSessionState(includeWelcome = false)
    val latestOnSpeechPlaybackActiveChange by rememberUpdatedState(onSpeechPlaybackActiveChange)
    val speaker = remember(context) {
        TextToSpeechSpeaker(context) { active ->
            latestOnSpeechPlaybackActiveChange(active)
        }
    }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showVoiceLayer by remember { mutableStateOf(false) }
    var voiceRecognitionEnabled by remember { mutableStateOf(true) }
    var voiceStartDelayMillis by remember { mutableStateOf(0L) }
    var wakeResponseToken by remember { mutableStateOf(0L) }
    var handledWakeSignal by remember { androidx.compose.runtime.mutableIntStateOf(wakeSignal) }
    val quickPrompts = aiConfig.quickPrompts.ifEmpty { AppConfigDefaults.defaultConfig.ai.quickPrompts }
    var spokenMessageId by remember {
        mutableStateOf(chatState.messages.lastOrNull { it.sender == ChatSender.Assistant }?.id)
    }
    val hasConversation = chatState.messages.isNotEmpty() || chatState.isTyping

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

    LaunchedEffect(chatState.messages.size, chatState.isTyping) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(wakeSignal) {
        if (wakeSignal > 0 && wakeSignal != handledWakeSignal) {
            handledWakeSignal = wakeSignal
            openWakeVoiceLayer()
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

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AiChatBackgroundImage(
            blurDp = backgroundBlurDp,
            imageUri = backgroundImageUri,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFBF7).copy(alpha = 0.24f)),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            if (hasConversation) {
                ChatHeader()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                ) {
                    ChatMessageList(
                        messages = chatState.messages,
                        isTyping = chatState.isTyping,
                        modifier = Modifier.fillMaxWidth(),
                        onSpeakMessage = { speaker.speak(it.text, ttsConfig) },
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandSurface.copy(alpha = 0.92f))
                        .border(1.dp, BrandBorder.copy(alpha = 0.7f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickPromptRow(
                        prompts = quickPrompts,
                        onPromptSelected = { prompt -> chatState.submitMessage(prompt, scope, aiConfig) },
                    )
                    ChatInputBar(
                        value = chatState.inputText,
                        onValueChange = { chatState.inputText = it },
                        onSend = { chatState.submitMessage(scope = scope, aiConfig = aiConfig) },
                        onMicClick = ::openVoiceLayer,
                    )
                }
            } else {
                AiVoiceLandingPage(
                    modifier = Modifier.fillMaxSize(),
                    onMicClick = ::openVoiceLayer,
                )
            }
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
                chatState.submitMessage(prompt, scope, aiConfig)
            },
            onSpeechTextReady = { text ->
                closeVoiceLayer()
                chatState.submitMessage(text, scope, aiConfig)
            },
            onDismiss = ::closeVoiceLayer,
        )
    }
}

@Composable
private fun AiChatBackgroundImage(
    blurDp: Float,
    imageUri: String,
) {
    val context = LocalContext.current
    val uploadedImage by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imageUri,
    ) {
        value = decodeAiBackgroundImage(context, imageUri)
    }
    val effectiveBlurDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blurDp.coerceIn(0f, MAX_AI_BACKGROUND_BLUR_DP)
    } else {
        0f
    }
    val imageModifier = Modifier
        .fillMaxSize()
        .scale(if (effectiveBlurDp > 0f) 1.035f else 1f)
        .blur(effectiveBlurDp.dp)

    if (uploadedImage != null) {
        Image(
            bitmap = uploadedImage!!,
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ai_chat_background),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
        )
    }
}

private const val MAX_AI_BACKGROUND_BLUR_DP = 12f

private fun decodeAiBackgroundImage(context: Context, imageUri: String): ImageBitmap? {
    return decodeImageBitmapFromUri(context = context, imageUri = imageUri)
}

@Composable
private fun AiVoiceLandingPage(
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val largeTablet = maxWidth >= 900.dp && maxHeight >= 620.dp
        val mediumTablet = maxWidth >= 650.dp && maxHeight >= 520.dp
        val ringSize = when {
            largeTablet -> 300.dp
            mediumTablet -> 250.dp
            else -> 205.dp
        }
        val middleRingSize = when {
            largeTablet -> 244.dp
            mediumTablet -> 204.dp
            else -> 166.dp
        }
        val buttonSize = when {
            largeTablet -> 224.dp
            mediumTablet -> 184.dp
            else -> 148.dp
        }
        val iconSize = when {
            largeTablet -> 82.dp
            mediumTablet -> 68.dp
            else -> 54.dp
        }
        val promptSize = when {
            largeTablet -> 18.sp
            mediumTablet -> 16.sp
            else -> 14.sp
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "一句话享受平安金融服务",
                color = BrandPrimary,
                fontSize = if (largeTablet) 24.sp else 19.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = if (largeTablet) 34.dp else 24.dp),
            )
            LargeMicrophoneBlock(
                onClick = onMicClick,
                modifier = Modifier.fillMaxWidth(),
                ringSize = ringSize,
                middleRingSize = middleRingSize,
                buttonSize = buttonSize,
                iconSize = iconSize,
                promptFontSize = promptSize,
                promptTopPadding = if (largeTablet) 24.dp else 18.dp,
            )
        }
    }
}

@Composable
private fun ChatHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandSurface.copy(alpha = 0.9f))
            .border(1.dp, BrandBorder.copy(alpha = 0.7f))
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.pingan_ai_robot),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "平安 AI 对话",
                    color = BrandText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "保险产品咨询、理赔问题和方案规划",
                    color = BrandTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Text(
            text = "在线秒答",
            color = BrandSuccessText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(BrandSuccessBackground)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private const val WAKE_RESPONSE_RECOGNITION_DELAY_MILLIS = 350L
