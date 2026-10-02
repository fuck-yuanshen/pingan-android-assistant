package com.example.pingan.data.config

object QuickPromptConfig {
    val defaults = listOf(
        "我想咨询意外伤害保险2.0的保费和保障",
        "宠无忧爱宠保障卡适合猫狗吗？",
        "小微企业保可以怎么配置？",
        "平安行出行安全守护计划多少钱？",
    )

    fun parse(rawText: String): List<String> =
        normalize(rawText.lines())

    fun normalize(prompts: List<String>): List<String> =
        prompts
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    fun display(prompts: List<String>): String =
        normalize(prompts).joinToString("\n")
}
