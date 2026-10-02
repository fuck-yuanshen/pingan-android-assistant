package com.example.pingan.data.config

import android.content.Context
import com.example.pingan.R
import com.example.pingan.data.model.Product
import com.example.pingan.data.model.VideoItem

object ConfiguredContentMapper {
    fun products(context: Context, config: AppConfig): List<Product> {
        return config.products.map { product ->
            Product(
                id = product.id,
                title = product.title,
                tag = product.tag,
                features = product.features,
                price = product.price,
                priceSuffix = product.priceSuffix,
                imageRes = resolveResource(context, "drawable", product.imageResourceName, R.drawable.product_accident_short_term),
                imageUri = product.imageUri,
                category = product.category,
                details = product.details,
                detailUrl = product.detailUrl,
            )
        }
    }

    fun videos(context: Context, config: AppConfig): List<VideoItem> {
        return config.videos.map { video ->
            VideoItem(
                id = video.id,
                title = video.title,
                category = video.category,
                rawRes = resolveResource(context, "raw", video.rawResourceName, R.raw.good_insurance_ping_an),
                videoUri = video.videoUri,
            )
        }
    }

    private fun resolveResource(context: Context, type: String, name: String, fallback: Int): Int {
        val resolved = context.resources.getIdentifier(name.trim(), type, context.packageName)
        return if (resolved != 0) resolved else fallback
    }
}
