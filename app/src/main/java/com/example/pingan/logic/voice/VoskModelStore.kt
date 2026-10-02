package com.example.pingan.logic.voice

import android.content.Context
import java.io.File

object VoskModelStore {
    const val MODEL_ASSET_DIR = "vosk-model-small-cn-0.22"
    const val SAMPLE_RATE = 16_000.0f

    fun prepareModelDir(context: Context): File {
        val appContext = context.applicationContext
        val targetDir = File(appContext.filesDir, MODEL_ASSET_DIR)
        val marker = File(targetDir, ".ready")
        if (marker.exists() && hasModelFiles(targetDir)) return targetDir

        val topLevelEntries = appContext.assets.list(MODEL_ASSET_DIR).orEmpty()
        check(topLevelEntries.isNotEmpty()) {
            "未找到离线语音模型，请确认 assets/$MODEL_ASSET_DIR 已随 APK 打包"
        }

        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        copyAssetTree(context = appContext, assetPath = MODEL_ASSET_DIR, outputFile = targetDir)
        marker.writeText("ready", Charsets.UTF_8)
        return targetDir
    }

    private fun copyAssetTree(
        context: Context,
        assetPath: String,
        outputFile: File,
    ) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            outputFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        outputFile.mkdirs()
        children.forEach { child ->
            copyAssetTree(
                context = context,
                assetPath = "$assetPath/$child",
                outputFile = File(outputFile, child),
            )
        }
    }

    private fun hasModelFiles(targetDir: File): Boolean =
        File(targetDir, "conf").exists() &&
            File(targetDir, "graph").exists() &&
            File(targetDir, "am").exists()
}
