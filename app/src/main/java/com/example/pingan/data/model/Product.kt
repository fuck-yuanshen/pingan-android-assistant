package com.example.pingan.data.model

import androidx.annotation.DrawableRes

data class Product(
    val id: String,
    val title: String,
    val tag: String,
    val features: List<String>,
    val price: String,
    val priceSuffix: String,
    @param:DrawableRes val imageRes: Int,
    val imageUri: String,
    val category: String,
    val details: String,
    val detailUrl: String,
)
