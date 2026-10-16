package com.example.quadplayer

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.io.File
import java.util.concurrent.Executors

/** Loads duration, resolution and a thumbnail for a video file in the background. */
object VideoMetaLoader {
    class Meta(val durationMs: Long, val width: Int, val height: Int, val thumb: Bitmap?)

    private val cache = LruCache<String, Meta>(100)
    private val pool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    fun cached(path: String): Meta? = cache.get(path)

    /** [wanted] is checked right before work starts, so rows scrolled past are skipped. */
    fun load(file: File, wanted: () -> Boolean, onDone: (Meta) -> Unit) {
        pool.execute {
            if (!wanted()) return@execute
            val meta = extract(file)
            cache.put(file.absolutePath, meta)
            main.post { onDone(meta) }
        }
    }

    private fun extract(file: File): Meta {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file.absolutePath)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) { val t = w; w = h; h = t }

            val atUs = minOf(dur / 10, 10_000L) * 1000L
            val dstW = 320
            val dstH = if (w > 0 && h > 0) (dstW * h / w).coerceIn(90, 640) else 180
            val bmp: Bitmap? =
                if (Build.VERSION.SDK_INT >= 27) {
                    r.getScaledFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, dstW, dstH)
                } else {
                    r.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { src ->
                        Bitmap.createScaledBitmap(src, dstW, dstH, true).also { if (it !== src) src.recycle() }
                    }
                }
            return Meta(dur, w, h, bmp)
        } catch (_: Exception) {
            return Meta(0, 0, 0, null)
        } finally {
            try { r.release() } catch (_: Exception) { }
        }
    }
}
