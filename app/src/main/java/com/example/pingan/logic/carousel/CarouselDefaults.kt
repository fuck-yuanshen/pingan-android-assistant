package com.example.pingan.logic.carousel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

object CarouselDefaults {
    const val AutoPlayMillis = 4_500L
}

// 产品页轮播使用同一段定时逻辑，确保自动切换行为集中维护。
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoCarouselEffect(
    pagerState: PagerState,
    itemCount: Int,
    enabled: Boolean = true,
    intervalMillis: Long = CarouselDefaults.AutoPlayMillis,
) {
    LaunchedEffect(pagerState, itemCount, enabled, intervalMillis) {
        if (!enabled || itemCount <= 1) return@LaunchedEffect

        while (true) {
            delay(intervalMillis)
            val nextPage = (pagerState.currentPage + 1) % itemCount
            pagerState.animateScrollToPage(nextPage)
        }
    }
}
