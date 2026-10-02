package com.example.pingan.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pingan.data.model.Product
import com.example.pingan.logic.carousel.AutoCarouselEffect
import com.example.pingan.logic.image.decodeImageBitmapFromUri
import com.example.pingan.ui.theme.BrandBackground
import com.example.pingan.ui.theme.BrandBluePill
import com.example.pingan.ui.theme.BrandBorder
import com.example.pingan.ui.theme.BrandMuted
import com.example.pingan.ui.theme.BrandOrange
import com.example.pingan.ui.theme.BrandPrimary
import com.example.pingan.ui.theme.BrandSubtleBorder
import com.example.pingan.ui.theme.BrandSurface
import com.example.pingan.ui.theme.BrandText
import com.example.pingan.ui.theme.BrandTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductShowcase(
    products: List<Product>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { products.size })
    val scope = rememberCoroutineScope()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    AutoCarouselEffect(pagerState = pagerState, itemCount = products.size)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardHeight = responsiveCardHeight(maxWidth)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                beyondViewportPageCount = 1,
            ) { page ->
                ProductCard(
                    product = products[page],
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp),
                    onDetailsClick = {
                        val opened = openExternalLink(context, products[page].detailUrl)
                        if (!opened) {
                            selectedProduct = products[page]
                        }
                    },
                )
            }
        }

        PagerControls(
            itemCount = products.size,
            activeIndex = pagerState.currentPage,
            onPrevious = {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage - 1 + products.size) % products.size)
                }
            },
            onNext = {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % products.size)
                }
            },
            onDotSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
        )
    }

    selectedProduct?.let { product ->
        ProductDetailsLayer(
            product = product,
            onDismiss = { selectedProduct = null },
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(BrandSurface)
            .border(1.dp, BrandBorder, RoundedCornerShape(28.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.46f)
                .background(BrandBackground),
        ) {
            ProductImage(product = product, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.12f), androidx.compose.ui.graphics.Color.Transparent),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = product.title,
                    color = BrandText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.tag,
                    color = androidx.compose.ui.graphics.Color(0xFF2563EB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(BrandBluePill.copy(alpha = 0.72f))
                        .border(1.dp, BrandBluePill, RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                product.features.forEach { feature ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = feature,
                            color = BrandMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = product.price,
                        color = androidx.compose.ui.graphics.Color(0xFFD9531E),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (product.priceSuffix.isNotBlank()) {
                        Text(
                            text = product.priceSuffix,
                            color = BrandTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 3.dp, bottom = 3.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, BrandOrange, RoundedCornerShape(999.dp))
                        .clickable(onClick = onDetailsClick)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "了解详情",
                        color = androidx.compose.ui.graphics.Color(0xFFD9531E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFD9531E),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsLayer(
    product: Product,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.58f))
                .padding(18.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(BrandSurface)
                    .padding(bottom = 18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandBorder),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "产品保障详情",
                            color = BrandPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = product.title,
                            color = BrandText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 25.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = BrandTextSecondary,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(7.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    ) {
                        ProductImage(product = product, modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f),
                                        ),
                                    ),
                                ),
                        )
                        Text(
                            text = "官方承保 · 简单省心",
                            color = BrandSurface,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 34.dp),
                        )
                        Text(
                            text = "平安保险为您全程守护",
                            color = androidx.compose.ui.graphics.Color(0xFFFFD8B0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 14.dp),
                        )
                    }

                    DetailSection(
                        title = "产品简介",
                        text = product.details,
                    )
                    ProductBenefitGrid()
                    ProductFlow()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "试算保费", color = BrandTextSecondary, fontSize = 12.sp)
                        Text(
                            text = product.price,
                            color = BrandPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text(
                        text = "立即为自己/家人投保",
                        color = BrandSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(BrandPrimary)
                            .padding(horizontal = 24.dp, vertical = 13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    text: String,
) {
    Column {
        Text(
            text = title,
            color = BrandText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            color = BrandTextSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BrandBackground)
                .border(1.dp, BrandBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
        )
    }
}

@Composable
private fun ProductBenefitGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "特色尊享权益",
            color = BrandText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BenefitCell("自费药/进口药", "100%全额赔付", Modifier.weight(1f))
            BenefitCell("极速出单", "电子保单秒送达", Modifier.weight(1f))
        }
        BenefitCell("售后服务", "24小时全国客服专线及极速闪赔", Modifier.fillMaxWidth())
    }
}

@Composable
private fun BenefitCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBackground)
            .border(1.dp, BrandBorder, RoundedCornerShape(14.dp))
            .padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, color = BrandTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = BrandPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ProductFlow() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "极速投保流程",
            color = BrandText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BrandBackground)
                .border(1.dp, BrandBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("填写资料", "在线缴费", "保单生效").forEachIndexed { index, label ->
                FlowStep(index = index + 1, label = label)
                if (index < 2) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandBorder,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowStep(
    index: Int,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(BrandPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = index.toString(), color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = label, color = BrandTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

private fun responsiveCardHeight(maxWidth: Dp): Dp = when {
    maxWidth >= 900.dp -> 350.dp
    maxWidth >= 650.dp -> 320.dp
    else -> 270.dp
}

@Composable
private fun ProductImage(
    product: Product,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uploadedImage by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = product.imageUri,
    ) {
        value = decodeProductImage(context, product.imageUri)
    }

    if (uploadedImage != null) {
        Image(
            bitmap = uploadedImage!!,
            contentDescription = product.title,
            modifier = modifier.clipToBounds(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(product.imageRes),
            contentDescription = product.title,
            modifier = modifier.clipToBounds(),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun decodeProductImage(context: Context, imageUri: String): ImageBitmap? {
    return decodeImageBitmapFromUri(
        context = context,
        imageUri = imageUri,
        maxSide = MAX_DECODE_IMAGE_SIDE,
    )
}

private fun openExternalLink(context: Context, rawUrl: String): Boolean {
    val normalizedUrl = normalizeUrl(rawUrl) ?: return false
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.isSuccess
}

private fun normalizeUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null

    val uri = Uri.parse(trimmed)
    return if (uri.scheme.isNullOrBlank()) {
        "https://$trimmed"
    } else {
        trimmed
    }
}

private const val MAX_DECODE_IMAGE_SIDE = 1600
