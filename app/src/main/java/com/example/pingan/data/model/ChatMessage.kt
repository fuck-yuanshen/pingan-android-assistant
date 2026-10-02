package com.example.pingan.data.model

enum class ChatSender {
    User,
    Assistant,
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val timestamp: String,
)
