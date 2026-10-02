package com.example.pingan.logic.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatSessionState(includeWelcome: Boolean) {
    val messages = mutableStateListOf<ChatMessage>()
    var inputText by mutableStateOf("")
    var isTyping by mutableStateOf(false)
        private set

    private var pendingReplies by mutableIntStateOf(0)

    init {
        if (includeWelcome) {
            messages += ChatEngine.welcomeMessage()
        }
    }

    fun submitMessage(text: String = inputText, scope: CoroutineScope, aiConfig: AiConfig) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        messages += ChatEngine.userMessage(trimmed)
        inputText = ""
        pendingReplies += 1
        isTyping = true

        // 延迟回复用于统一模拟 AI 正在生成答案的交互节奏。
        scope.launch {
            delay(650)
            messages += ChatEngine.assistantMessage(trimmed, aiConfig)
            pendingReplies -= 1
            isTyping = pendingReplies > 0
        }
    }
}
