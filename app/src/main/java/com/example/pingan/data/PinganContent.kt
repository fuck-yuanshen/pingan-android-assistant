package com.example.pingan.data

import com.example.pingan.R
import com.example.pingan.data.model.Product
import com.example.pingan.data.model.VideoItem

object PinganContent {
    val products = listOf(
        Product(
            id = "short_accident",
            title = "平安短期意外伤害保险 2.0",
            tag = "互联网版",
            features = listOf("国内短途旅游、出行投保", "专享意外住院津贴"),
            price = "¥5.4",
            priceSuffix = "起",
            imageRes = R.drawable.product_accident_short_term,
            imageUri = "",
            category = "意外险",
            details = "面向高频出行、户外运动和短期旅行场景，覆盖交通意外、旅行意外和日常意外等多重风险。产品投保流程轻量，保费门槛低，并包含意外住院津贴，适合为临时出行快速补齐保障。",
            detailUrl = "https://www.pingan.com/",
        ),
        Product(
            id = "pet_protection",
            title = "平安宠无忧·爱宠保障卡 2.0版",
            tag = "互联网版",
            features = listOf("宠物医疗保障，1元起赔", "适合猫狗家庭基础配置"),
            price = "¥199",
            priceSuffix = "元起",
            imageRes = R.drawable.product_pet_protection,
            imageUri = "",
            category = "宠物险",
            details = "为猫狗家庭提供宠物医疗保障，覆盖常见宠物就医场景。线上投保方便，低起赔门槛能降低看病负担，适合为爱宠配置基础医疗保障。",
            detailUrl = "https://www.pingan.com/",
        ),
        Product(
            id = "small_business",
            title = "小微企业保",
            tag = "企业保障",
            features = listOf("免核组合产品投保方便", "保额/险种自由调整"),
            price = "按需定制",
            priceSuffix = "",
            imageRes = R.drawable.product_small_business,
            imageUri = "",
            category = "企业险",
            details = "为门店、办公室和小型经营主体提供组合式保障方案。险种与保额可按经营规模和风险点灵活配置，适合小微企业日常风险管理。",
            detailUrl = "https://www.pingan.com/",
        ),
        Product(
            id = "travel_safety",
            title = "平安行·出行安全守护计划",
            tag = "互联网版",
            features = listOf("秒级响应，救援先行", "7X24小时出行守护"),
            price = "¥12",
            priceSuffix = "元起",
            imageRes = R.drawable.product_travel_safety,
            imageUri = "",
            category = "出行保障",
            details = "聚焦旅行、户外和日常出行场景，提供快速响应与救援协助。7X24小时守护机制能帮助用户在突发情况下优先获得协助，提升出行安全感。",
            detailUrl = "https://www.pingan.com/",
        ),
    )

    val videos = listOf(
        VideoItem(
            id = "good_insurance",
            title = "好保险、添平安视频",
            category = "产品介绍",
            rawRes = R.raw.good_insurance_ping_an,
            videoUri = "",
        ),
        VideoItem(
            id = "quick_service",
            title = "快捷服务视频",
            category = "服务介绍",
            rawRes = R.raw.quick_service,
            videoUri = "",
        ),
    )
}
