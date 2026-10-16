package com.example.quadplayer

import android.content.Context
import android.os.Environment
import java.io.File

class StorageRoot(val label: String, val dir: File)

/** Finds internal storage plus any mounted USB drives / SD cards. */
object StorageRoots {
    fun find(context: Context): List<StorageRoot> {
        val out = mutableListOf<StorageRoot>()
        val primary = Environment.getExternalStorageDirectory()
        out += StorageRoot("Internal storage", primary)

        fun addExtra(dir: File) {
            val path = dir.absolutePath
            if (path == primary.absolutePath || out.any { it.dir.absolutePath == path }) return
            out += StorageRoot("USB / SD card (${dir.name})", dir)
        }

        // Volumes that report an app-specific directory: strip back to the volume root
        try {
            context.getExternalFilesDirs(null)?.forEach { f ->
                if (f == null) return@forEach
                val p = f.absolutePath
                val idx = p.indexOf("/Android/data")
                if (idx > 0) addExtra(File(p.substring(0, idx)))
            }
        } catch (_: Exception) { }

        // Anything else mounted under /storage (some USB drives are not reported above)
        try {
            File("/storage").listFiles()?.forEach { f ->
                if (f.isDirectory && f.canRead() && f.name != "emulated" && f.name != "self") addExtra(f)
            }
        } catch (_: Exception) { }

        return out
    }
}
