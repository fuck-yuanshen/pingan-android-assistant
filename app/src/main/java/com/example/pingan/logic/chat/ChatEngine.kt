package com.example.pingan.logic.chat

import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.model.ChatMessage
import com.example.pingan.data.model.ChatSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

private data class ReplyRule(
    val keys: List<String>,
    val reply: String,
)

object ChatEngine {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

    private val defaultReply =
        "您好，我是平安保险智能助手。您可以咨询短期意外险、爱宠医疗保障、小微企业保、出行安全守护计划等问题。"

    private val replyRules = listOf(
        ReplyRule(
            keys = listOf("意外", "伤害", "短期", "住院津贴", "保费"),
            reply = "平安短期意外伤害保险 2.0 适合短途旅行、通勤和临时出行补充保障，¥5.4 起，重点覆盖意外伤害和意外住院津贴。",
        ),
        ReplyRule(
            keys = listOf("宠", "猫", "狗", "宠无忧", "爱宠", "宠物医疗"),
            reply = "平安宠无忧·爱宠保障卡 2.0版 面向猫狗家庭，主打宠物医疗保障，1元起赔，适合给爱宠配置基础看病保障。",
        ),
        ReplyRule(
            keys = listOf("小微", "企业", "门店", "经营", "保额", "险种"),
            reply = "小微企业保适合门店、办公室和小型经营主体，可按经营风险自由配置保额和险种，投保流程相对轻量。",
        ),
        ReplyRule(
            keys = listOf("平安行", "出行", "安全", "救援", "7x24", "7X24"),
            reply = "平安行·出行安全守护计划 ¥12 元起，覆盖旅行、户外和日常出行场景，提供秒级响应、救援先行和7X24小时守护。",
        ),
        ReplyRule(
            keys = listOf("视频", "服务", "理赔", "快捷"),
            reply = "产品视频页已内置本地视频，视频会自动播放并在结束后切换到下一条。",
        ),
    )

    fun welcomeMessage(): ChatMessage = ChatMessage(
        id = "welcome",
        sender = ChatSender.Assistant,
        text = defaultReply,
        timestamp = "刚刚",
    )

    fun userMessage(text: String): ChatMessage = message(ChatSender.User, text)

    suspend fun assistantMessage(query: String, aiConfig: AiConfig): ChatMessage {
        val reply = if (aiConfig.apiKey.isBlank()) {
            findLocalReply(query)
        } else {
            requestRemoteReply(query, aiConfig).getOrElse { error ->
                "AI接口调用失败：${error.message ?: "未知错误"}\n\n本地建议：${findLocalReply(query)}"
            }
        }

        return message(ChatSender.Assistant, reply)
    }

    private suspend fun requestRemoteReply(query: String, aiConfig: AiConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("model", aiConfig.model)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", aiConfig.systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", query)))
                .put("thinking", JSONObject().put("type", if (aiConfig.thinkingEnabled) "enabled" else "disabled"))
                .put("reasoning_effort", aiConfig.reasoningEffort)
                .put("stream", false)
                .toString()

            val connection = (URL(aiConfig.endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${aiConfig.apiKey}")
            }

            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw IllegalStateException("HTTP $responseCode $errorText")
            }

            JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content")
                .takeIf { it.isNotBlank() }
                ?: "AI接口返回为空。"
        }
    }

    private fun message(sender: ChatSender, text: String): ChatMessage = ChatMessage(
        id = "${sender.name}-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}",
        sender = sender,
        text = text,
        timestamp = LocalTime.now().format(timeFormatter),
    )

    private fun findLocalReply(text: String): String {
        val query = text.lowercase(Locale.CHINA)
        return replyRules.firstOrNull { rule ->
            rule.keys.any { key -> query.contains(key.lowercase(Locale.CHINA)) }
        }?.reply ?: defaultReply
    }
}
