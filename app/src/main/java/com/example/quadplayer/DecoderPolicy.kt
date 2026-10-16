package com.example.quadplayer

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaCodecList
import android.os.Build
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil

/**
 * TV chips usually allow only a couple of simultaneous HARDWARE video decoders (often 2), while a
 * tablet allows many. Extra players therefore use SOFTWARE decoders, which have no such limit.
 */
object DecoderPolicy {

    /** Orders the candidate decoders: hardware first, or software first for tiles over the limit. */
    fun selector(preferSoftware: Boolean) = MediaCodecSelector { mimeType, secure, tunneling ->
        val all = MediaCodecUtil.getDecoderInfos(mimeType, secure, tunneling)
        if (preferSoftware) all.sortedBy { if (it.softwareOnly) 0 else 1 }
        else all.sortedBy { if (it.softwareOnly) 1 else 0 }
    }

    /**
     * How many tiles should start on hardware decoders. Based on what the device reports for H.264;
     * if that is unknown, assume 2 on a TV and 4 elsewhere. A tile that still fails (devices often
     * over-report) is switched to software automatically by VideoTile.
     */
    fun hardwareLimit(isTv: Boolean): Int {
        var best = 0
        try {
            for (info in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos) {
                if (info.isEncoder) continue
                val software = if (Build.VERSION.SDK_INT >= 29) {
                    info.isSoftwareOnly
                } else {
                    info.name.startsWith("OMX.google.") || info.name.startsWith("c2.android.") ||
                            info.name.startsWith("c2.google.")
                }
                if (software) continue
                if ("video/avc" !in info.supportedTypes) continue
                best = maxOf(best, info.getCapabilitiesForType("video/avc").maxSupportedInstances)
            }
        } catch (_: Exception) { /* fall through to the default */ }
        return if (best <= 0) (if (isTv) 2 else 4) else best.coerceIn(1, 4)
    }

    fun isTv(context: Context): Boolean {
        val ui = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ||
                context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")
    }
}
