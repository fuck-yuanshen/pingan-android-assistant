package com.example.pingan.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pingan.R
import com.example.pingan.data.model.VideoItem
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoShowcase(
    videos: List<VideoItem>,
    modifier: Modifier = Modifier,
    voiceInputActive: Boolean = false,
    onHorseQuestionClick: () -> Unit = {},
    onVideoPlaybackActiveChange: (Boolean) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { videos.size })
    val scope = rememberCoroutineScope()
    var fullscreenVideo by remember { mutableStateOf<VideoItem?>(null) }
    var inlinePlayingPage by remember { mutableStateOf<Int?>(null) }
    var fullscreenVideoPlaying by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }

    LaunchedEffect(inlinePlayingPage, fullscreenVideoPlaying) {
        onVideoPlaybackActiveChange(inlinePlayingPage != null || fullscreenVideoPlaying)
    }

    DisposableEffect(Unit) {
        onDispose {
            onVideoPlaybackActiveChange(false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardHeight = responsiveVideoCardHeight(maxWidth)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                beyondViewportPageCount = 1,
            ) { page ->
                VideoPlayerCard(
                    video = videos[page],
                    isActive = page == pagerState.currentPage,
                    voiceInputActive = voiceInputActive || fullscreenVideo != null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp),
                    onEnded = {
                        if (page == pagerState.currentPage) {
                            scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1) % videos.size)
                            }
                        }
                    },
                    onFullscreen = { fullscreenVideo = videos[page] },
                    muted = muted,
                    onToggleMute = { muted = !muted },
                    onPlaybackActiveChange = { active ->
                        if (active) {
                            inlinePlayingPage = page
                        } else if (inlinePlayingPage == page) {
                            inlinePlayingPage = null
                        }
                    },
                )
            }
        }

        PagerControls(
            itemCount = videos.size,
            activeIndex = pagerState.currentPage,
            onPrevious = {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage - 1 + videos.size) % videos.size)
                }
            },
            onNext = {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % videos.size)
                }
            },
            onDotSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
        )
    }

    fullscreenVideo?.let { video ->
        FullscreenVideoDialog(
            video = video,
            muted = muted,
            onToggleMute = { muted = !muted },
            onClose = { fullscreenVideo = null },
            onPlaybackActiveChange = { active -> fullscreenVideoPlaying = active },
            onHorseQuestionClick = {
                fullscreenVideo = null
                onHorseQuestionClick()
            },
        )
    }
}

@Composable
private fun VideoPlayerCard(
    video: VideoItem,
    isActive: Boolean,
    voiceInputActive: Boolean,
    onEnded: () -> Unit,
    onFullscreen: () -> Unit,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onPlaybackActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceKey = video.videoUri.ifBlank { video.rawRes.toString() }
    var isPlaying by remember(sourceKey) { mutableStateOf(false) }
    var progress by remember(sourceKey) { mutableFloatStateOf(0f) }
    var togglePlay by remember(sourceKey) { mutableStateOf<() -> Unit>({}) }

    LaunchedEffect(isActive, isPlaying) {
        onPlaybackActiveChange(isActive && isPlaying)
    }

    DisposableEffect(Unit) {
        onDispose {
            onPlaybackActiveChange(false)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val portraitModifier = if (maxWidth / maxHeight > PORTRAIT_VIDEO_ASPECT_RATIO) {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(PORTRAIT_VIDEO_ASPECT_RATIO)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(PORTRAIT_VIDEO_ASPECT_RATIO)
            }

            ConfigurableVideoPlayer(
                video = video,
                isActive = isActive,
                voiceInputActive = voiceInputActive,
                muted = muted,
                onEnded = onEnded,
                modifier = portraitModifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(22.dp)),
                onPlayingChanged = { isPlaying = it },
                onProgressChanged = { progress = it },
                onTogglePlayReady = { togglePlay = it },
            )
        }

        VideoOverlay(
            title = video.title,
            category = video.category,
            isPlaying = isPlaying,
            progress = progress,
            onTogglePlay = togglePlay,
            onFullscreen = onFullscreen,
            muted = muted,
            onToggleMute = onToggleMute,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ConfigurableVideoPlayer(
    video: VideoItem,
    isActive: Boolean,
    voiceInputActive: Boolean,
    muted: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayingChanged: (Boolean) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onTogglePlayReady: (() -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val sourceKey = video.videoUri.ifBlank { video.rawRes.toString() }
    var mediaPlayer by remember(sourceKey) { mutableStateOf<MediaPlayer?>(null) }
    var activeSurface by remember(sourceKey) { mutableStateOf<Surface?>(null) }
    var prepared by remember(sourceKey) { mutableStateOf(false) }
    var userPaused by remember(sourceKey) { mutableStateOf(false) }
    var interruptedByVoiceInput by remember(sourceKey) { mutableStateOf(false) }
    var isPlaying by remember(sourceKey) { mutableStateOf(false) }
    var progress by remember(sourceKey) { mutableFloatStateOf(0f) }

    fun updatePlaying(value: Boolean) {
        isPlaying = value
        onPlayingChanged(value)
    }

    fun updateProgress(value: Float) {
        progress = value
        onProgressChanged(value)
    }

    fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (!prepared) return

        if (player.isPlaying) {
            userPaused = true
            interruptedByVoiceInput = false
            player.pause()
            updatePlaying(false)
        } else {
            userPaused = false
            interruptedByVoiceInput = false
            player.start()
            updatePlaying(true)
        }
    }

    fun applyVolume(player: MediaPlayer) {
        val volume = if (muted) 0f else 1f
        runCatching { player.setVolume(volume, volume) }
    }

    fun pauseForVoiceInput() {
        interruptedByVoiceInput = true
        userPaused = true
        val player = mediaPlayer ?: run {
            updatePlaying(false)
            return
        }
        runCatching {
            if (prepared && player.isPlaying) {
                player.pause()
            }
        }
        updatePlaying(false)
    }

    fun releasePlayer() {
        runCatching {
            mediaPlayer?.setSurface(null)
            mediaPlayer?.release()
        }
        mediaPlayer = null
        prepared = false
        updatePlaying(false)
        updateProgress(0f)
    }

    fun createPlayer(surface: Surface) {
        releasePlayer()

        val player = MediaPlayer()
        try {
            if (video.videoUri.isNotBlank()) {
                player.setDataSource(context, Uri.parse(video.videoUri))
            } else {
                val assetFileDescriptor = context.resources.openRawResourceFd(video.rawRes) ?: return
                assetFileDescriptor.use { descriptor ->
                    player.setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length,
                    )
                }
            }
            player.setSurface(surface)
            applyVolume(player)
            player.setOnPreparedListener { preparedPlayer ->
                prepared = true
                preparedPlayer.isLooping = false
                applyVolume(preparedPlayer)
                if (isActive && !voiceInputActive && !userPaused && !interruptedByVoiceInput) {
                    preparedPlayer.seekTo(0)
                    preparedPlayer.start()
                    updatePlaying(true)
                }
            }
            player.setOnCompletionListener {
                updatePlaying(false)
                updateProgress(1f)
                onEnded()
            }
            player.prepareAsync()
            mediaPlayer = player
        } catch (_: Exception) {
            player.release()
        }
    }

    LaunchedEffect(muted, mediaPlayer, prepared) {
        mediaPlayer?.let { player ->
            if (prepared) applyVolume(player)
        }
    }

    LaunchedEffect(voiceInputActive, isActive, sourceKey, mediaPlayer, prepared) {
        if (voiceInputActive && isActive) {
            pauseForVoiceInput()
        }
    }

    LaunchedEffect(isActive, voiceInputActive, sourceKey) {
        val player = mediaPlayer
        if (voiceInputActive && isActive) {
            pauseForVoiceInput()
        } else if (isActive) {
            userPaused = false
            updateProgress(0f)
            if (prepared && player != null && !interruptedByVoiceInput) {
                runCatching {
                    player.seekTo(0)
                    player.start()
                    updatePlaying(true)
                }
            }
        } else if (player != null) {
            runCatching {
                if (player.isPlaying) player.pause()
            }
            updatePlaying(false)
        }
    }

    LaunchedEffect(isActive, sourceKey, mediaPlayer) {
        while (isActive) {
            val player = mediaPlayer
            updateProgress(if (player != null && prepared && player.duration > 0) {
                (player.currentPosition.toFloat() / player.duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            })
            delay(250)
        }
    }

    SideEffect {
        onTogglePlayReady { togglePlayback() }
    }

    DisposableEffect(sourceKey) {
        onDispose {
            releasePlayer()
            activeSurface?.release()
            activeSurface = null
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clickable { togglePlayback() },
            factory = { ctx ->
                TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(surfaceTexture)
                            activeSurface?.release()
                            activeSurface = surface
                            createPlayer(surface)
                        }

                        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) = Unit

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            releasePlayer()
                            activeSurface?.release()
                            activeSurface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                    }

                    if (isAvailable) {
                        surfaceTexture?.let { texture ->
                            val surface = Surface(texture)
                            activeSurface?.release()
                            activeSurface = surface
                            createPlayer(surface)
                        }
                    }
                }
            },
            update = {
                val player = mediaPlayer
                if (player != null && prepared) {
                    if (!isActive || voiceInputActive) {
                        runCatching {
                            if (player.isPlaying) player.pause()
                        }
                        updatePlaying(false)
                    } else if (!userPaused && !interruptedByVoiceInput && !player.isPlaying) {
                        runCatching {
                            player.start()
                            updatePlaying(true)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun FullscreenVideoDialog(
    video: VideoItem,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onClose: () -> Unit,
    onPlaybackActiveChange: (Boolean) -> Unit,
    onHorseQuestionClick: () -> Unit,
) {
    val sourceKey = video.videoUri.ifBlank { video.rawRes.toString() }
    var isPlaying by remember(sourceKey) { mutableStateOf(false) }
    var progress by remember(sourceKey) { mutableFloatStateOf(0f) }
    var togglePlay by remember(sourceKey) { mutableStateOf<() -> Unit>({}) }

    LaunchedEffect(isPlaying) {
        onPlaybackActiveChange(isPlaying)
    }

    DisposableEffect(Unit) {
        onDispose {
            onPlaybackActiveChange(false)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val portraitModifier = if (maxWidth / maxHeight > PORTRAIT_VIDEO_ASPECT_RATIO) {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(PORTRAIT_VIDEO_ASPECT_RATIO)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(PORTRAIT_VIDEO_ASPECT_RATIO)
                }

                ConfigurableVideoPlayer(
                    video = video,
                    isActive = true,
                    voiceInputActive = false,
                    muted = muted,
                    onEnded = onClose,
                    modifier = portraitModifier.align(Alignment.Center),
                    onPlayingChanged = { isPlaying = it },
                    onProgressChanged = { progress = it },
                    onTogglePlayReady = { togglePlay = it },
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭全屏",
                    tint = BrandSurface,
                    modifier = Modifier.size(24.dp),
                )
            }

            HorseFloatingAssistant(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 102.dp),
                onClick = onHorseQuestionClick,
            )

            VideoOverlay(
                title = video.title,
                category = video.category,
                isPlaying = isPlaying,
                progress = progress,
                onTogglePlay = togglePlay,
                onFullscreen = onClose,
                muted = muted,
                onToggleMute = onToggleMute,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun HorseFloatingAssistant(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = "试试说：小安小安，运动受伤怎么办?",
            color = Color(0xFF111827),
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 40.sp,
            modifier = Modifier
                .padding(bottom = 36.dp)
                .width(560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(BrandSurface.copy(alpha = 0.82f))
                .padding(horizontal = 32.dp, vertical = 18.dp),
        )
        Image(
            painter = painterResource(R.drawable.pingan_horse_floating),
            contentDescription = "小马助手",
            modifier = Modifier.size(width = 196.dp, height = 232.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun VideoOverlay(
    title: String,
    category: String,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onFullscreen: () -> Unit,
    muted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = BrandOrange,
            trackColor = BrandSurface.copy(alpha = 0.2f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = BrandSurface,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTogglePlay)
                        .padding(3.dp),
                )
                Text(
                    text = "$title · $category",
                    color = Color(0xFFD1D5DB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = "全屏播放",
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onFullscreen)
                        .padding(3.dp),
                )
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (muted) "取消静音" else "静音",
                    tint = if (muted) BrandOrange else Color(0xFFD1D5DB),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleMute)
                        .padding(3.dp),
                )
                Text(
                    text = if (muted) "已静音" else "声音",
                    color = if (muted) BrandOrange else Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(onClick = onToggleMute),
                )
            }
        }
    }
}

private fun responsiveVideoCardHeight(maxWidth: Dp): Dp = when {
    maxWidth >= 900.dp -> 430.dp
    maxWidth >= 650.dp -> 390.dp
    else -> 270.dp
}

private const val PORTRAIT_VIDEO_ASPECT_RATIO = 9f / 16f
