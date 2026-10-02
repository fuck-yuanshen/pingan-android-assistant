package com.example.pingan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.pingan.ui.theme.BrandDotMuted
import com.example.pingan.ui.theme.BrandPrimary
import com.example.pingan.ui.theme.BrandTextSecondary

@Composable
fun PagerControls(
    itemCount: Int,
    activeIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDotSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "前一个",
            tint = BrandTextSecondary,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onPrevious)
                .padding(3.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(itemCount) { index ->
                val selected = index == activeIndex
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (selected) 22.dp else 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) BrandPrimary else BrandDotMuted)
                        .clickable { onDotSelected(index) },
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "下一个",
            tint = BrandTextSecondary,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onNext)
                .padding(3.dp),
        )
    }
}
