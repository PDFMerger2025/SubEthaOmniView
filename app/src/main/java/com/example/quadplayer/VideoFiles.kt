package com.example.quadplayer

import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

/** Helpers shared by the in-app browser and the per-tile folder loop. */
object VideoFiles {
    private val extensions = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "3g2", "ts", "m2ts", "mts",
        "mpg", "mpeg", "wmv", "flv", "f4v", "ogv", "vob", "divx", "asf", "rm", "rmvb"
    )

    fun isVideo(f: File): Boolean {
        if (!f.isFile || f.name.startsWith(".")) return false
        val ext = f.extension.lowercase(Locale.ROOT)
        if (ext in extensions) return true
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        return mime != null && mime.startsWith("video/")
    }

    /** "clip2" sorts before "clip10". */
    fun naturalCompare(a: String, b: String): Int {
        val split = Regex("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)")
        val pa = a.split(split)
        val pb = b.split(split)
        for (i in 0 until minOf(pa.size, pb.size)) {
            val x = pa[i]
            val y = pb[i]
            val c = if (x.isNotEmpty() && y.isNotEmpty() && x[0].isDigit() && y[0].isDigit()) {
                val xs = x.trimStart('0')
                val ys = y.trimStart('0')
                if (xs.length != ys.length) xs.length - ys.length else xs.compareTo(ys)
            } else x.compareTo(y, ignoreCase = true)
            if (c != 0) return c
        }
        return pa.size - pb.size
    }

    fun formatDuration(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024L * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.0f MB", bytes / 1048576.0)
        else -> String.format(Locale.US, "%.2f GB", bytes / 1073741824.0)
    }
}
