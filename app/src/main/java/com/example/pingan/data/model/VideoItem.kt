package com.example.pingan.data.model

import androidx.annotation.RawRes

data class VideoItem(
    val id: String,
    val title: String,
    val category: String,
    @param:RawRes val rawRes: Int,
    val videoUri: String,
)
