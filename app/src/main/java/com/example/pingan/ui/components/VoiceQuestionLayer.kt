package com.example.pingan.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pingan.R
import com.example.pingan.data.config.AsrConfig
import com.example.pingan.data.config.AppConfigDefaults
import com.example.pingan.logic.voice.CloudSpeechRecognizer
import com.example.pingan.logic.voice.OfflineSpeechRecognizer
import com.example.pingan.logic.voice.OfflineSpeechStatus
import com.example.pingan.ui.theme.BrandBackground
import com.example.pingan.ui.theme.BrandBorder
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandSurface
import com.example.pingan.ui.theme.BrandText
import com.example.pingan.ui.theme.BrandTextSecondary
import kotlinx.coroutines.delay

@Composable
fun VoiceQuestionLayer(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
    onSpeechTextReady: (String) -> Unit,
    onDismiss: () -> Unit,
    recognitionEnabled: Boolean = true,
    startDelayMillis: Long = 0L,
    asrConfig: AsrConfig = AppConfigDefaults.defaultConfig.asr,
    ttsApiKeyFallback: String = AppConfigDefaults.defaultConfig.tts.apiKey,
) {
    val context = LocalContext.current
    val latestOnSpeechTextReady by rememberUpdatedState(onSpeechTextReady)
    var status by remember { mutableStateOf(OfflineSpeechStatus.Preparing) }
    var recognizedText by remember { mutableStateOf("") }
    var submittedText by remember { mutableStateOf("") }
    var useOfflineFallback by remember(asrConfig) { mutableStateOf(false) }
    val shouldUseCloudAsr = asrConfig.enabled &&
        (asrConfig.apiKey.isNotBlank() || ttsApiKeyFallback.isNotBlank()) &&
        !useOfflineFallback
    val offlineRecognizer = remember(context) {
        OfflineSpeechRecognizer(
            context = context,
            onStatusChanged = { newStatus, _ ->
                status = newStatus
            },
            onPartialText = { text -> recognizedText = text },
            onFinalText = { text -> recognizedText = text },
        )
    }
    val cloudRecognizer = remember(context, asrConfig, ttsApiKeyFallback) {
        CloudSpeechRecognizer(
            context = context,
            asrConfig = asrConfig,
            ttsApiKeyFallback = ttsApiKeyFallback,
            onStatusChanged = { newStatus, _ -> status = newStatus },
            onPartialText = { text -> recognizedText = text },
            onFinalText = { text -> recognizedText = text },
            onError = {
                if (asrConfig.fallbackToOffline) {
                    useOfflineFallback = true
                }
            },
        )
    }

    LaunchedEffect(offlineRecognizer, cloudRecognizer, recognitionEnabled, startDelayMillis, shouldUseCloudAsr) {
        if (!recognitionEnabled) {
            cloudRecognizer.stop()
            offlineRecognizer.stop()
            status = OfflineSpeechStatus.Preparing
            return@LaunchedEffect
        }
        if (startDelayMillis > 0L) {
            status = OfflineSpeechStatus.Preparing
            delay(startDelayMillis)
        }
        if (shouldUseCloudAsr) {
            offlineRecognizer.stop()
            cloudRecognizer.start()
        } else {
            cloudRecognizer.stop()
            offlineRecognizer.start()
        }
    }

    DisposableEffect(offlineRecognizer, cloudRecognizer) {
        onDispose {
            cloudRecognizer.destroy()
            offlineRecognizer.destroy()
        }
    }

    // 识别内容稳定后自动提交，形成“说完停顿即发送”的语音输入体验。
    LaunchedEffect(recognizedText) {
        val candidate = recognizedText.trim()
        if (candidate.isBlank() || candidate == submittedText) return@LaunchedEffect

        delay(AUTO_SUBMIT_DELAY_MILLIS)
        if (recognizedText.trim() == candidate && submittedText != candidate) {
            submittedText = candidate
            latestOnSpeechTextReady(candidate)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC).copy(alpha = 0.72f),
                            Color(0xFFFFF7ED).copy(alpha = 0.66f),
                            Color(0xFF111827).copy(alpha = 0.38f),
                        ),
                    ),
                )
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(BrandSurface)
                    .clickable(enabled = false) {}
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 126.dp, height = 134.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.pingan_horse_mascot),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "你好我是小安，正在聆听",
                    color = BrandText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))
                VoiceListeningState(
                    status = status,
                    recognizedText = recognizedText,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    prompts.forEach { prompt ->
                        VoicePromptItem(
                            prompt = prompt,
                            onClick = { onPromptSelected(prompt) },
                        )
                    }
                }
                Text(
                    text = "取消并退出语音听取",
                    color = BrandTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandTextSecondary.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 13.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceListeningState(
    status: OfflineSpeechStatus,
    recognizedText: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BrandBackground)
                .border(1.dp, BrandBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (recognizedText.isBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (status == OfflineSpeechStatus.Preparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BrandOrange,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        VoiceBars()
                    }
                    Text(
                        text = if (status == OfflineSpeechStatus.Error) {
                            "离线识别暂不可用，可直接选择下方问题"
                        } else {
                            "请说出要咨询的问题"
                        },
                        color = BrandTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    text = recognizedText,
                    color = BrandText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (recognizedText.isNotBlank()) {
            Text(
                text = "停顿后自动发送",
                color = BrandOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun VoicePromptItem(
    prompt: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(BrandOrange),
        )
        Text(
            text = prompt,
            color = BrandText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f),
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BrandOrange,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun VoiceBars() {
    Row(
        modifier = Modifier.height(42.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(32.dp, 20.dp, 40.dp, 18.dp, 34.dp).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index % 2 == 0) BrandOrange else BrandOrange.copy(alpha = 0.65f)),
            )
        }
    }
}

private const val AUTO_SUBMIT_DELAY_MILLIS = 1_800L
