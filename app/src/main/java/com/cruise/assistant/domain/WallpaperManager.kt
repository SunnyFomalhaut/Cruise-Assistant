package com.cruise.assistant.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object WallpaperManager {

    private const val CUSTOM_WALLPAPER_FILE = "custom_wallpaper.jpg"
    private const val MAX_DIMENSION = 1920

    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun loadWallpaperBitmap(context: Context, key: String): Bitmap? {
        if (key == WallpaperPreset.NONE.key) return null
        
        cache[key]?.let { if (!it.isRecycled) return it }

        val bitmap = if (key == WallpaperPreset.CUSTOM.key) {
            val file = File(context.filesDir, CUSTOM_WALLPAPER_FILE)
            if (file.exists()) {
                decodeSampledBitmap(file.absolutePath, MAX_DIMENSION, MAX_DIMENSION)
            } else null
        } else {
            val preset = WallpaperPreset.fromKey(key)
            if (preset.assetPath.isNotEmpty()) {
                try {
                    context.assets.open(preset.assetPath).use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        if (bitmap != null) {
            cache[key] = bitmap
        }
        return bitmap
    }

    fun hasCustomWallpaper(context: Context): Boolean {
        return File(context.filesDir, CUSTOM_WALLPAPER_FILE).exists()
    }

    fun saveCustomWallpaper(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return false

            val scaled = if (original.width > MAX_DIMENSION || original.height > MAX_DIMENSION) {
                val ratio = minOf(
                    MAX_DIMENSION.toFloat() / original.width,
                    MAX_DIMENSION.toFloat() / original.height
                )
                val targetW = (original.width * ratio).toInt()
                val targetH = (original.height * ratio).toInt()
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            val targetFile = File(context.filesDir, CUSTOM_WALLPAPER_FILE)
            FileOutputStream(targetFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }

            cache.remove(WallpaperPreset.CUSTOM.key)
            cache[WallpaperPreset.CUSTOM.key] = scaled
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearCache() {
        cache.clear()
    }

    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeFile(filePath, decodeOptions)
    }
}
