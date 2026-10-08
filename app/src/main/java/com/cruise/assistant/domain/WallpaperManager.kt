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
            // 用户相册图可能几十兆像素，若全尺寸解码极易 OOM（catch(Exception) 拦不住
            // OutOfMemoryError），所以先按 MAX_DIMENSION×MAX_DIMENSION 采样解码。
            val original = decodeSampledStream(context, uri) ?: return false

            // 采样值是 2 的幂、不精确，解码后仍超出 MAX_DIMENSION 时再做等比缩放兜底
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
            // 不主动 recycle original：scaled 可能就是 original 本身（无需缩放时），
            // 且 scaled 随后进了缓存，强行回收容易引入竞态，简单交给 GC。

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

    /**
     * decodeSampledBitmap 的流版：ContentResolver 的流只能读一次，
     * 两遍解码（先量边界、再带 inSampleSize 真解码）必须各自重新 openInputStream，
     * 并用 use{} 确保无论解码成败都关闭，避免 fd 泄漏。
     */
    private fun decodeSampledStream(context: Context, uri: Uri): Bitmap? {
        // 第一遍：inJustDecodeBounds 只解析图片头量出宽高，不分配像素内存
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        // 与 decodeSampledBitmap 相同的循环：inSampleSize 只能取 2 的幂，
        // 采样到"再翻倍就会小于目标尺寸"为止；不精确由调用方 createScaledBitmap 兜底
        var inSampleSize = 1
        if (bounds.outHeight > MAX_DIMENSION || bounds.outWidth > MAX_DIMENSION) {
            val halfHeight = bounds.outHeight / 2
            val halfWidth = bounds.outWidth / 2
            while (halfHeight / inSampleSize >= MAX_DIMENSION && halfWidth / inSampleSize >= MAX_DIMENSION) {
                inSampleSize *= 2
            }
        }

        // 第二遍：上一遍已把流读耗尽，重新打开并带采样真解码；损坏图片返回 null
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }
        val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
        return decodeStream.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }
}
