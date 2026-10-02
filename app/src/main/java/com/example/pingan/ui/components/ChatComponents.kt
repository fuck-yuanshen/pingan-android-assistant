package com.example.pingan.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pingan.R
import com.example.pingan.data.model.ChatMessage
import com.example.pingan.data.model.ChatSender
import com.example.pingan.ui.theme.BrandBackground
import com.example.pingan.ui.theme.BrandBorder
import com.example.pingan.ui.theme.BrandMuted
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandPrimary
import com.example.pingan.ui.theme.BrandSurface
import com.example.pingan.ui.theme.BrandText
import com.example.pingan.ui.theme.BrandTextSecondary
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatMessageList(
    messages: List<ChatMessage>,
    isTyping: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onSpeakMessage: ((ChatMessage) -> Unit)? = null,
) {
    var browserUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
    ) {
        messages.forEach { message ->
            ChatBubble(
                message = message,
                compact = compact,
                onSpeakMessage = onSpeakMessage,
                onOpenLink = { browserUrl = it },
            )
        }
        if (isTyping) {
            TypingBubble()
        }
    }

    browserUrl?.let { url ->
        InAppBrowserDialog(
            url = url,
            onDismiss = { browserUrl = null },
        )
    }
}

@Composable
fun QuickPromptRow(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prompts.forEach { prompt ->
            Text(
                text = prompt,
                color = BrandPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandOrange.copy(alpha = 0.08f))
                    .border(1.dp, BrandOrange.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                    .clickable { onPromptSelected(prompt) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "请输入您想咨询的问题...",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(BrandBackground)
            .border(1.dp, BrandBorder, RoundedCornerShape(999.dp))
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                color = BrandText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(BrandOrange),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = BrandTextSecondary.copy(alpha = 0.58f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                innerTextField()
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircleIconButton(
            onClick = onMicClick,
            background = BrandOrange.copy(alpha = 0.09f),
            tint = BrandOrange,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "语音输入",
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(modifier = Modifier.width(7.dp))
        CircleIconButton(
            onClick = { if (value.isNotBlank()) onSend() },
            background = if (value.isNotBlank()) BrandOrange else BrandBorder,
            tint = if (value.isNotBlank()) BrandSurface else BrandMuted,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送消息",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    background: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint,
            content = content,
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    compact: Boolean,
    onSpeakMessage: ((ChatMessage) -> Unit)?,
    onOpenLink: (String) -> Unit,
) {
    val isUser = message.sender == ChatSender.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!compact && !isUser) {
            ChatAvatar(sender = message.sender)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            LinkedMessageText(
                text = message.text,
                isUser = isUser,
                onOpenLink = onOpenLink,
                color = if (isUser) BrandSurface else BrandText,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = if (compact) 18.sp else 21.sp,
                modifier = Modifier
                    .fillMaxWidth(if (compact) 0.84f else 0.78f)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp,
                        ),
                    )
                    .background(if (isUser) BrandOrange else if (compact) BrandSurface else BrandBackground)
                    .border(
                        width = if (!isUser && !compact) 1.dp else 0.dp,
                        color = if (!isUser && !compact) BrandBorder else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp,
                        ),
                    )
                    .padding(horizontal = if (compact) 12.dp else 15.dp, vertical = if (compact) 9.dp else 12.dp),
            )
            if (!isUser && shouldShowQuotePreview(message.text)) {
                RemoteMessageImage(
                    imageUrl = QUOTE_PREVIEW_IMAGE_URL,
                    onClick = { onOpenLink(QUOTE_TRIGGER_URL) },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(if (compact) 0.84f else 0.78f),
                )
            }
            if (!isUser && onSpeakMessage != null) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CircleIconButton(
                        onClick = { onSpeakMessage(message) },
                        background = BrandOrange.copy(alpha = 0.1f),
                        tint = BrandOrange,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "播放回复",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (!compact) {
                        Text(
                            text = "播放",
                            color = BrandTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (!compact) {
                Text(
                    text = message.timestamp,
                    color = BrandMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        if (!compact && isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            ChatAvatar(sender = message.sender)
        }
    }
}

@Composable
private fun LinkedMessageText(
    text: String,
    isUser: Boolean,
    onOpenLink: (String) -> Unit,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
) {
    val linkColor = if (isUser) BrandSurface else BrandPrimary
    val annotatedText = remember(text, isUser) {
        buildAnnotatedString {
            append(text)
            URL_REGEX.findAll(text).forEach { match ->
                val cleanUrl = trimUrlPunctuation(match.value)
                if (cleanUrl.isBlank()) return@forEach

                val start = match.range.first
                val end = start + cleanUrl.length
                addStringAnnotation(
                    tag = URL_TAG,
                    annotation = normalizeUrl(cleanUrl),
                    start = start,
                    end = end,
                )
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start = start,
                    end = end,
                )
            }
        }
    }

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
        ),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(URL_TAG, offset, offset)
                .firstOrNull()
                ?.let { annotation -> onOpenLink(annotation.item) }
        },
    )
}

@Composable
private fun RemoteMessageImage(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        loadFailed = false
        bitmap = loadRemoteBitmap(imageUrl)
        loadFailed = bitmap == null
    }

    val imageModifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(BrandSurface)
        .border(1.dp, BrandBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = "产品预览图",
            contentScale = ContentScale.Crop,
            modifier = imageModifier.aspectRatio(16f / 9f),
        )
    } else {
        Box(
            modifier = imageModifier
                .aspectRatio(16f / 9f)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (loadFailed) "图片加载失败" else "图片加载中...",
                color = BrandTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InAppBrowserDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandSurface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface)
                    .border(1.dp, BrandBorder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = url,
                    color = BrandText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrandOrange.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭网页",
                        tint = BrandOrange,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatAvatar(sender: ChatSender) {
    val isUser = sender == ChatSender.User
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isUser) BrandOrange else BrandOrange.copy(alpha = 0.1f))
            .border(1.dp, if (isUser) BrandOrange else BrandOrange.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isUser) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = BrandSurface,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.pingan_ai_robot),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(BrandSurface)
                .border(
                    1.dp,
                    BrandBorder,
                    RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BrandOrange),
                    )
                }
            }
        }
    }
}

private fun normalizeUrl(rawUrl: String): String =
    if (rawUrl.startsWith("http://", ignoreCase = true) ||
        rawUrl.startsWith("https://", ignoreCase = true)
    ) {
        rawUrl
    } else {
        "https://$rawUrl"
    }

private fun trimUrlPunctuation(rawUrl: String): String =
    rawUrl.trimEnd('.', ',', '，', '。', '!', '！', '?', '？', ';', '；', ':', '：', ')', '）')

private fun shouldShowQuotePreview(text: String): Boolean =
    text.contains(QUOTE_TRIGGER_URL, ignoreCase = true)

private suspend fun loadRemoteBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
        }
        try {
            connection.inputStream.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

private const val URL_TAG = "url"
private const val QUOTE_TRIGGER_URL = "https://baoxian.pingan.com/pa18shopnst/quote/mobile/index.html#/ZP021414"
private const val QUOTE_PREVIEW_IMAGE_URL = "https://baoxian.pingan.com/uploadfile/clause/image/1769156079236-3.jpg"

private val URL_REGEX = Regex("""(?i)\b((?:https?://|www\.)[^\s<>"']+)""")
