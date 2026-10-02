package com.example.pingan.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pingan.data.config.AppConfigRepository
import com.example.pingan.data.config.ConfiguredContentMapper
import com.example.pingan.data.model.AppScreen
import com.example.pingan.ui.components.AppHeader
import com.example.pingan.ui.components.VoiceWakeEffect
import com.example.pingan.ui.screens.AiChatScreen
import com.example.pingan.ui.screens.ConfigScreen
import com.example.pingan.ui.screens.FeaturedProductsScreen
import com.example.pingan.ui.screens.ProductVideosScreen
import com.example.pingan.ui.theme.BrandBackground
import com.example.pingan.ui.theme.BrandSurface

@Composable
fun PinganApp() {
    val context = LocalContext.current
    val configRepository = remember(context) { AppConfigRepository(context) }
    val config = configRepository.config
    val configuredProducts = remember(context, config.products) {
        ConfiguredContentMapper.products(context, config)
    }
    val configuredVideos = remember(context, config.videos) {
        ConfiguredContentMapper.videos(context, config)
    }
    var currentScreen by remember { mutableStateOf(AppScreen.Featured) }
    var showConfig by remember { mutableStateOf(false) }
    var featuredTapCount by remember { mutableIntStateOf(0) }
    var wakeSignal by remember { mutableIntStateOf(0) }
    var voiceLayerActive by remember { mutableStateOf(false) }
    var speechPlaybackActive by remember { mutableStateOf(false) }
    var videoPlaybackActive by remember { mutableStateOf(false) }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasRecordAudioPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasRecordAudioPermission) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(currentScreen, showConfig) {
        if (currentScreen != AppScreen.Video || showConfig) {
            videoPlaybackActive = false
        }
    }

    VoiceWakeEffect(
        enabled = hasRecordAudioPermission &&
            !showConfig &&
            !voiceLayerActive &&
            !speechPlaybackActive &&
            !videoPlaybackActive,
        wakePhrases = config.wakePhrases,
        onWakeDetected = { wakeSignal += 1 },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandBackground, BrandSurface))),
    ) {
        AppHeader(
            currentScreen = currentScreen,
            onScreenSelected = { screen ->
                if (screen == AppScreen.Featured) {
                    featuredTapCount += 1
                    if (featuredTapCount >= 10) {
                        showConfig = true
                        featuredTapCount = 0
                    }
                } else {
                    featuredTapCount = 0
                }
                currentScreen = screen
            },
        )

        if (showConfig) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                ConfigScreen(
                    config = config,
                    onSave = { configRepository.save(it) },
                    onReset = { configRepository.resetToDefaults() },
                    onClose = { showConfig = false },
                    modifier = Modifier
                        .widthIn(max = 1080.dp)
                        .fillMaxSize(),
                )
            }
            return@Column
        }

        when (currentScreen) {
            AppScreen.Chat -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AiChatScreen(
                        modifier = Modifier.fillMaxSize(),
                        wakeSignal = wakeSignal,
                        wakeResponseText = config.wakeResponseText,
                        aiConfig = config.ai,
                        ttsConfig = config.tts,
                        asrConfig = config.asr,
                        backgroundBlurDp = config.ai.backgroundBlurDp,
                        backgroundImageUri = config.ai.backgroundImageUri,
                        onVoiceLayerVisibilityChange = { voiceLayerActive = it },
                        onSpeechPlaybackActiveChange = { speechPlaybackActive = it },
                    )
                }
            }

            AppScreen.Featured,
            AppScreen.Video
            -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val contentModifier = Modifier
                        .widthIn(max = 1080.dp)
                        .fillMaxHeight()
                        .fillMaxWidth()

                    when (currentScreen) {
                        AppScreen.Featured -> FeaturedProductsScreen(
                            modifier = contentModifier,
                            wakeSignal = wakeSignal,
                            wakeResponseText = config.wakeResponseText,
                            products = configuredProducts,
                            aiConfig = config.ai,
                            ttsConfig = config.tts,
                            asrConfig = config.asr,
                            onVoiceLayerVisibilityChange = { voiceLayerActive = it },
                            onSpeechPlaybackActiveChange = { speechPlaybackActive = it },
                        )
                        AppScreen.Video -> ProductVideosScreen(
                            modifier = contentModifier,
                            wakeSignal = wakeSignal,
                            wakeResponseText = config.wakeResponseText,
                            videos = configuredVideos,
                            aiConfig = config.ai,
                            ttsConfig = config.tts,
                            asrConfig = config.asr,
                            voiceInputActive = voiceLayerActive,
                            onVoiceLayerVisibilityChange = { voiceLayerActive = it },
                            onSpeechPlaybackActiveChange = { speechPlaybackActive = it },
                            onVideoPlaybackActiveChange = { videoPlaybackActive = it },
                        )
                        AppScreen.Chat -> Unit
                    }
                }
            }
        }
    }
}
