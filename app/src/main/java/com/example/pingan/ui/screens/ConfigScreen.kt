package com.example.pingan.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pingan.data.config.AiConfig
import com.example.pingan.data.config.AppConfig
import com.example.pingan.data.config.AppConfigDefaults
import com.example.pingan.data.config.AsrConfig
import com.example.pingan.data.config.ProductConfig
import com.example.pingan.data.config.QuickPromptConfig
import com.example.pingan.data.config.TtsConfig
import com.example.pingan.data.config.VideoConfig
import com.example.pingan.data.config.WakePhraseConfig
import com.example.pingan.logic.voice.OfflineSpeechRecognizer
import com.example.pingan.logic.voice.OfflineSpeechStatus
import com.example.pingan.logic.voice.WakePhraseMatcher
import com.example.pingan.ui.theme.BrandBackground
import com.example.pingan.ui.theme.BrandBorder
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandPrimary
import com.example.pingan.ui.theme.BrandSurface
import com.example.pingan.ui.theme.BrandText
import com.example.pingan.ui.theme.BrandTextSecondary

@Composable
fun ConfigScreen(
    config: AppConfig,
    onSave: (AppConfig) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(config) { mutableStateOf(config) }
    var wakePhraseDraft by remember(config) { mutableStateOf(WakePhraseConfig.display(config.wakePhrases)) }
    var saveMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(BrandSurface)
            .border(1.dp, BrandBorder, RoundedCornerShape(28.dp))
            .padding(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "隐藏配置",
                    color = BrandText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "连续点击顶部“精选产品”10次进入。配置保存后立即生效。",
                    color = BrandTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            TextButton(onClick = onClose) {
                Text(text = "关闭")
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "语音唤醒") {
                ConfigTextField(
                    label = "唤醒词，每行一个",
                    value = wakePhraseDraft,
                    onValueChange = {
                        wakePhraseDraft = it
                        draft = draft.copy(wakePhrases = WakePhraseConfig.parse(it))
                    },
                    helper = "支持多个唤醒词，也可用逗号、顿号分隔。默认：小安小安、平安平安",
                    minLines = 2,
                )
                ConfigTextField(
                    label = "唤醒后语音回应",
                    value = draft.wakeResponseText,
                    onValueChange = { draft = draft.copy(wakeResponseText = it) },
                    helper = "识别到唤醒词后先播报这句话，再进入语音输入。",
                    modifier = Modifier.padding(top = 10.dp),
                )
                WakePhraseTestPanel(
                    wakePhrases = draft.wakePhrases,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            SectionCard(title = "精选产品滚动卡片") {
                draft.products.forEachIndexed { index, product ->
                    ProductEditor(
                        index = index,
                        product = product,
                        onChange = { changed ->
                            draft = draft.copy(products = draft.products.replaceAt(index, changed))
                        },
                    )
                    if (index != draft.products.lastIndex) {
                        HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(vertical = 14.dp))
                    }
                }
            }

            SectionCard(title = "产品视频") {
                draft.videos.forEachIndexed { index, video ->
                    VideoEditor(
                        index = index,
                        video = video,
                        onChange = { changed ->
                            draft = draft.copy(videos = draft.videos.replaceAt(index, changed))
                        },
                        onRemove = {
                            draft = draft.copy(videos = draft.videos.filterIndexed { i, _ -> i != index })
                        },
                    )
                    if (index != draft.videos.lastIndex) {
                        HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(vertical = 14.dp))
                    }
                }
                OutlinedButton(
                    onClick = {
                        val nextIndex = draft.videos.size + 1
                        draft = draft.copy(
                            videos = draft.videos + VideoConfig(
                                id = "video_$nextIndex",
                                title = "新视频",
                                category = "产品介绍",
                                rawResourceName = "good_insurance_ping_an",
                                videoUri = "",
                            ),
                        )
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(text = "新增视频配置")
                }
            }

            SectionCard(title = "AI 对话接口") {
                AiEditor(
                    ai = draft.ai,
                    onChange = { draft = draft.copy(ai = it) },
                )
            }

            SectionCard(title = "语音播报 TTS") {
                TtsEditor(
                    tts = draft.tts,
                    onChange = { draft = draft.copy(tts = it) },
                )
            }

            SectionCard(title = "云端语音识别 ASR") {
                AsrEditor(
                    asr = draft.asr,
                    onChange = { draft = draft.copy(asr = it) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = saveMessage,
                color = BrandPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onReset()
                        draft = AppConfigDefaults.defaultConfig
                        wakePhraseDraft = WakePhraseConfig.display(AppConfigDefaults.defaultConfig.wakePhrases)
                        saveMessage = "已恢复默认配置"
                    },
                ) {
                    Text(text = "恢复默认")
                }
                Button(
                    onClick = {
                        onSave(draft.normalized())
                        saveMessage = "已保存"
                    },
                ) {
                    Text(text = "保存配置")
                }
            }
        }
    }
}

@Composable
private fun ProductEditor(
    index: Int,
    product: ProductConfig,
    onChange: (ProductConfig) -> Unit,
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(context, it)
            onChange(product.copy(imageUri = it.toString()))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "产品 ${index + 1}",
            color = BrandPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        ConfigTextField("标题", product.title, { onChange(product.copy(title = it)) })
        ConfigTextField("标签", product.tag, { onChange(product.copy(tag = it)) })
        ConfigTextField(
            label = "卖点，每行一个",
            value = product.features.joinToString("\n"),
            onValueChange = { text ->
                onChange(product.copy(features = text.lines().map { it.trim() }.filter { it.isNotBlank() }))
            },
            minLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField("价格", product.price, { onChange(product.copy(price = it)) }, modifier = Modifier.weight(1f))
            ConfigTextField("价格后缀", product.priceSuffix, { onChange(product.copy(priceSuffix = it)) }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField("分类", product.category, { onChange(product.copy(category = it)) }, modifier = Modifier.weight(1f))
            ConfigTextField(
                label = "图片资源名",
                value = product.imageResourceName,
                onValueChange = { onChange(product.copy(imageResourceName = it)) },
                helper = "如 product_pet_protection",
                modifier = Modifier.weight(1f),
            )
        }
        ConfigTextField(
            label = "详情跳转链接",
            value = product.detailUrl,
            onValueChange = { onChange(product.copy(detailUrl = it)) },
            helper = "点击“了解详情”后会打开该链接；为空时显示本地详情层。",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                Text(text = if (product.imageUri.isBlank()) "上传/更换图片" else "重新选择图片")
            }
            OutlinedButton(
                onClick = { onChange(product.copy(imageUri = "")) },
                enabled = product.imageUri.isNotBlank(),
            ) {
                Text(text = "使用内置图片")
            }
        }
        if (product.imageUri.isNotBlank()) {
            Text(
                text = product.imageUri,
                color = BrandTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ConfigTextField("详情", product.details, { onChange(product.copy(details = it)) }, minLines = 3)
    }
}

@Composable
private fun VideoEditor(
    index: Int,
    video: VideoConfig,
    onChange: (VideoConfig) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(context, it)
            onChange(video.copy(videoUri = it.toString()))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "视频 ${index + 1}",
                color = BrandPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onRemove, enabled = index > 0) {
                Text(text = "删除")
            }
        }
        ConfigTextField("标题", video.title, { onChange(video.copy(title = it)) })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField("分类", video.category, { onChange(video.copy(category = it)) }, modifier = Modifier.weight(1f))
            ConfigTextField(
                label = "raw视频资源名",
                value = video.rawResourceName,
                onValueChange = { onChange(video.copy(rawResourceName = it)) },
                helper = "未选择本地视频时使用，如 good_insurance_ping_an、quick_service",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) {
                Text(text = if (video.videoUri.isBlank()) "上传/更换本地视频" else "重新选择视频")
            }
            OutlinedButton(
                onClick = { onChange(video.copy(videoUri = "")) },
                enabled = video.videoUri.isNotBlank(),
            ) {
                Text(text = "使用内置视频")
            }
        }
        if (video.videoUri.isNotBlank()) {
            Text(
                text = video.videoUri,
                color = BrandTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AiEditor(
    ai: AiConfig,
    onChange: (AiConfig) -> Unit,
) {
    val context = LocalContext.current
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(context, it)
            onChange(ai.copy(backgroundImageUri = it.toString()))
        }
    }
    var quickPromptDraft by remember(ai.quickPrompts) {
        mutableStateOf(QuickPromptConfig.display(ai.quickPrompts))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ConfigTextField("接口地址", ai.endpoint, { onChange(ai.copy(endpoint = it)) })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField("模型", ai.model, { onChange(ai.copy(model = it)) }, modifier = Modifier.weight(1f))
            ConfigTextField("推理强度", ai.reasoningEffort, { onChange(ai.copy(reasoningEffort = it)) }, modifier = Modifier.weight(1f))
        }
        ConfigTextField(
            label = "API Key",
            value = ai.apiKey,
            onValueChange = { onChange(ai.copy(apiKey = it)) },
            helper = "默认接口为 DeepSeek；Key 为空时使用本地模拟回复。",
            password = true,
        )
        ConfigTextField("System Prompt", ai.systemPrompt, { onChange(ai.copy(systemPrompt = it)) }, minLines = 3)
        ConfigTextField(
            label = "预设提示词，每行一个",
            value = quickPromptDraft,
            onValueChange = {
                quickPromptDraft = it
                onChange(ai.copy(quickPrompts = QuickPromptConfig.parse(it)))
            },
            helper = "AI 对话页和底部语音/文字交互区共用这一组预设问题。",
            minLines = 4,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = ai.thinkingEnabled,
                onCheckedChange = { onChange(ai.copy(thinkingEnabled = it)) },
            )
            Text(
                text = "启用 thinking 参数",
                color = BrandText,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = ai.voiceAutoPlay,
                onCheckedChange = { onChange(ai.copy(voiceAutoPlay = it)) },
            )
            Text(
                text = "AI 回复后自动语音播放",
                color = BrandText,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI 对话背景模糊",
                    color = BrandText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${ai.backgroundBlurDp.toInt()} dp",
                    color = BrandTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Slider(
                value = ai.backgroundBlurDp.coerceIn(0f, MAX_AI_BACKGROUND_BLUR_DP),
                onValueChange = { value ->
                    onChange(ai.copy(backgroundBlurDp = value.coerceIn(0f, MAX_AI_BACKGROUND_BLUR_DP)))
                },
                valueRange = 0f..MAX_AI_BACKGROUND_BLUR_DP,
                steps = 11,
            )
            Text(
                text = "0 为关闭模糊，数值越大背景越柔和。",
                color = BrandTextSecondary,
                fontSize = 11.sp,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) {
                Text(text = if (ai.backgroundImageUri.isBlank()) "上传/更换 AI 背景图" else "重新选择 AI 背景图")
            }
            OutlinedButton(
                onClick = { onChange(ai.copy(backgroundImageUri = "")) },
                enabled = ai.backgroundImageUri.isNotBlank(),
            ) {
                Text(text = "使用内置背景")
            }
        }
        if (ai.backgroundImageUri.isNotBlank()) {
            Text(
                text = ai.backgroundImageUri,
                color = BrandTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TtsEditor(
    tts: TtsConfig,
    onChange: (TtsConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = tts.enabled,
                onCheckedChange = { onChange(tts.copy(enabled = it)) },
            )
            Text(
                text = "启用 Qwen-TTS 云端语音",
                color = BrandText,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        ConfigTextField(
            label = "WebSocket 地址",
            value = tts.endpoint,
            onValueChange = { onChange(tts.copy(endpoint = it)) },
            helper = "默认：阿里云百炼 qwen3-tts-flash-realtime；为空会回退默认地址。",
        )
        ConfigTextField(
            label = "DashScope API Key",
            value = tts.apiKey,
            onValueChange = { onChange(tts.copy(apiKey = it)) },
            helper = "Key 为空时自动使用系统 TTS 兜底。生产环境建议走后端代理，不要把 Key 写入 APK。",
            password = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField(
                label = "音色 voice",
                value = tts.voice,
                onValueChange = { onChange(tts.copy(voice = it)) },
                helper = "如 Cherry",
                modifier = Modifier.weight(1f),
            )
            ConfigTextField(
                label = "模式 mode",
                value = tts.mode,
                onValueChange = { onChange(tts.copy(mode = it)) },
                helper = "server_commit 或 commit",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField(
                label = "语言类型",
                value = tts.languageType,
                onValueChange = { onChange(tts.copy(languageType = it)) },
                helper = "默认 Chinese",
                modifier = Modifier.weight(1f),
            )
            ConfigTextField(
                label = "采样率",
                value = tts.sampleRate.toString(),
                onValueChange = { value ->
                    onChange(tts.copy(sampleRate = value.toIntOrNull() ?: tts.sampleRate))
                },
                helper = "PCM 默认 24000",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AsrEditor(
    asr: AsrConfig,
    onChange: (AsrConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = asr.enabled,
                onCheckedChange = { onChange(asr.copy(enabled = it)) },
            )
            Text(
                text = "唤醒后使用 DashScope Fun-ASR 云端识别",
                color = BrandText,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        ConfigTextField(
            label = "WebSocket 地址",
            value = asr.endpoint,
            onValueChange = { onChange(asr.copy(endpoint = it)) },
            helper = "默认：DashScope inference；一般无需修改。",
        )
        ConfigTextField(
            label = "DashScope ASR API Key",
            value = asr.apiKey,
            onValueChange = { onChange(asr.copy(apiKey = it)) },
            helper = "为空时自动复用 TTS 的 DashScope Key；仍为空则使用离线识别兜底。",
            password = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField(
                label = "模型",
                value = asr.model,
                onValueChange = { onChange(asr.copy(model = it)) },
                helper = "默认 fun-asr-realtime",
                modifier = Modifier.weight(1f),
            )
            ConfigTextField(
                label = "采样率",
                value = asr.sampleRate.toString(),
                onValueChange = { value ->
                    onChange(asr.copy(sampleRate = value.toIntOrNull() ?: asr.sampleRate))
                },
                helper = "默认 16000",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigTextField(
                label = "分片帧数",
                value = asr.chunkFrames.toString(),
                onValueChange = { value ->
                    onChange(asr.copy(chunkFrames = value.toIntOrNull() ?: asr.chunkFrames))
                },
                helper = "默认 3200",
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = asr.fallbackToOffline,
                    onCheckedChange = { onChange(asr.copy(fallbackToOffline = it)) },
                )
                Text(
                    text = "失败后离线兜底",
                    color = BrandText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WakePhraseTestPanel(
    wakePhrases: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activeWakePhrases = remember(wakePhrases) {
        WakePhraseConfig.normalize(wakePhrases).ifEmpty { AppConfigDefaults.defaultConfig.wakePhrases }
    }
    var testing by remember(activeWakePhrases) { mutableStateOf(false) }
    var statusText by remember(activeWakePhrases) { mutableStateOf("点击开始测试后，说出当前配置的任一唤醒词。") }
    var recognizedText by remember(activeWakePhrases) { mutableStateOf("") }
    var matchedPhrase by remember(activeWakePhrases) { mutableStateOf("") }

    fun inspectRecognizedText(text: String) {
        recognizedText = text
        val match = WakePhraseMatcher.findMatchedWakePhrase(text, activeWakePhrases) ?: return
        matchedPhrase = match
        statusText = "检测成功：$match"
        testing = false
    }

    val recognizer = remember(context, activeWakePhrases) {
        OfflineSpeechRecognizer(
            context = context,
            onStatusChanged = { status, message ->
                statusText = when (status) {
                    OfflineSpeechStatus.Preparing -> "正在加载离线语音模型"
                    OfflineSpeechStatus.Listening -> "请说出当前配置的唤醒词"
                    OfflineSpeechStatus.Error -> message
                }
            },
            onPartialText = ::inspectRecognizedText,
            onFinalText = ::inspectRecognizedText,
        )
    }

    LaunchedEffect(testing, recognizer) {
        if (testing) {
            recognizer.start()
        } else {
            recognizer.stop()
        }
    }

    DisposableEffect(recognizer) {
        onDispose {
            recognizer.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface)
            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "唤醒词检测测试",
                color = BrandText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        recognizedText = ""
                        matchedPhrase = ""
                        statusText = "请说出当前配置的唤醒词"
                        testing = true
                    },
                    enabled = !testing,
                ) {
                    Text(text = "开始测试")
                }
                OutlinedButton(
                    onClick = {
                        testing = false
                        statusText = "已停止测试"
                    },
                    enabled = testing,
                ) {
                    Text(text = "停止")
                }
            }
        }
        Text(
            text = "当前检测词：${activeWakePhrases.joinToString("、")}",
            color = BrandTextSecondary,
            fontSize = 12.sp,
        )
        Text(
            text = statusText,
            color = if (matchedPhrase.isBlank()) BrandTextSecondary else BrandPrimary,
            fontSize = 12.sp,
            fontWeight = if (matchedPhrase.isBlank()) FontWeight.Medium else FontWeight.Bold,
        )
        if (recognizedText.isNotBlank()) {
            Text(
                text = "识别文本：$recognizedText",
                color = BrandTextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandBackground)
            .border(1.dp, BrandBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = BrandText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        content()
    }
}

@Composable
private fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helper: String = "",
    minLines: Int = 1,
    password: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label) },
            minLines = minLines,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        )
        if (helper.isNotBlank()) {
            Text(
                text = helper,
                color = BrandTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

private fun <T> List<T>.replaceAt(index: Int, item: T): List<T> =
    toMutableList().also { it[index] = item }

private fun AppConfig.normalized(): AppConfig =
    copy(
        wakePhrases = WakePhraseConfig.normalize(wakePhrases)
            .ifEmpty { AppConfigDefaults.defaultConfig.wakePhrases },
        wakeResponseText = wakeResponseText.trim()
            .ifBlank { AppConfigDefaults.defaultConfig.wakeResponseText },
        products = products.ifEmpty { AppConfigDefaults.defaultConfig.products },
        videos = videos.ifEmpty { AppConfigDefaults.defaultConfig.videos },
        ai = ai.copy(
            endpoint = ai.endpoint.trim().ifBlank { AppConfigDefaults.defaultConfig.ai.endpoint },
            model = ai.model.trim().ifBlank { AppConfigDefaults.defaultConfig.ai.model },
            reasoningEffort = ai.reasoningEffort.trim().ifBlank { AppConfigDefaults.defaultConfig.ai.reasoningEffort },
            quickPrompts = QuickPromptConfig.normalize(ai.quickPrompts)
                .ifEmpty { AppConfigDefaults.defaultConfig.ai.quickPrompts },
            backgroundBlurDp = ai.backgroundBlurDp.coerceIn(0f, MAX_AI_BACKGROUND_BLUR_DP),
            backgroundImageUri = ai.backgroundImageUri.trim(),
        ),
        tts = tts.copy(
            endpoint = tts.endpoint.trim().ifBlank { AppConfigDefaults.defaultConfig.tts.endpoint },
            voice = tts.voice.trim().ifBlank { AppConfigDefaults.defaultConfig.tts.voice },
            mode = normalizeTtsMode(tts.mode),
            languageType = tts.languageType.trim().ifBlank { AppConfigDefaults.defaultConfig.tts.languageType },
            sampleRate = tts.sampleRate.takeIf { it > 0 } ?: AppConfigDefaults.defaultConfig.tts.sampleRate,
        ),
        asr = asr.copy(
            endpoint = asr.endpoint.trim().ifBlank { AppConfigDefaults.defaultConfig.asr.endpoint },
            model = asr.model.trim().ifBlank { AppConfigDefaults.defaultConfig.asr.model },
            sampleRate = asr.sampleRate.takeIf { it > 0 } ?: AppConfigDefaults.defaultConfig.asr.sampleRate,
            chunkFrames = asr.chunkFrames.takeIf { it > 0 } ?: AppConfigDefaults.defaultConfig.asr.chunkFrames,
        ),
    )

private fun normalizeTtsMode(mode: String): String {
    val normalized = mode.trim()
    return when (normalized) {
        "server_commit", "commit" -> normalized
        else -> AppConfigDefaults.defaultConfig.tts.mode
    }
}

private const val MAX_AI_BACKGROUND_BLUR_DP = 12f

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
