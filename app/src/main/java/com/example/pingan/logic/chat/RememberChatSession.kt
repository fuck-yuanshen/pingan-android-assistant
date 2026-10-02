package com.example.pingan.logic.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberChatSessionState(includeWelcome: Boolean = true): ChatSessionState =
    remember(includeWelcome) { ChatSessionState(includeWelcome) }
