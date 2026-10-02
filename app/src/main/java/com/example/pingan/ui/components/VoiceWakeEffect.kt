package com.example.pingan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.pingan.logic.voice.WakePhraseRecognizer

@Composable
fun VoiceWakeEffect(
    enabled: Boolean,
    wakePhrases: List<String>,
    onWakeDetected: () -> Unit,
) {
    val context = LocalContext.current
    val recognizer = remember(context, wakePhrases) {
        WakePhraseRecognizer(
            context = context,
            wakePhrases = wakePhrases,
            onWakeDetected = { onWakeDetected() },
        )
    }

    DisposableEffect(recognizer) {
        onDispose {
            recognizer.destroy()
        }
    }

    // 前台页面级语音唤醒入口，负责启动和释放离线语音识别资源。
    DisposableEffect(enabled, recognizer) {
        if (enabled) {
            recognizer.start()
        } else {
            recognizer.stop()
        }

        onDispose {
            recognizer.stop()
        }
    }
}
