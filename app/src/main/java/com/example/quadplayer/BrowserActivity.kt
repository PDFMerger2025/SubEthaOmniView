package com.example.quadplayer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

/**
 * In-app file explorer for videos. Works with a D-pad remote and needs no system file picker,
 * so it also works on Android TV / Fire TV. Returns the chosen video as result extra "uri".
 */
class BrowserActivity : AppCompatActivity() {

    private class Row(val type: Int, val file: File?, val title: String, val subtitle: String)
    private class Built(val rows: List<Row>, val videoCount: Int, val emptyMessage: String)
    private class Holder(v: View) {
        val thumb: ImageView = v.findViewById(R.id.thumb)
        val icon: ImageView = v.findViewById(R.id.icon)
        val title: TextView = v.findViewById(R.id.title)
        val sub: TextView = v.findViewById(R.id.sub)
        @Volatile var path: String? = null
    }

    private companion object {
        const val UP = 0
        const val ROOT = 1
        const val DIR = 2
        const val VIDEO = 3
        val CYAN = 0xFF00E5FF.toInt()
    }

    private lateinit var pathView: TextView
    private lateinit var emptyView: TextView
    private lateinit var permPanel: View
    private lateinit var listView: ListView

    private val rows = mutableListOf<Row>()
    private val adapter = RowAdapter()
    private var currentDir: File? = null
    private var roots: List<StorageRoot> = emptyList()
    private var navToken = 0
    private var browserShown = false
    private val lister = Executors.newSingleThreadExecutor()

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showBrowser() else showPermissionPanel()
    }

    private fun permission() =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, permission()) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        findViewById<TextView>(R.id.browserTitle).text = intent.getStringExtra("title") ?: "BROWSE VIDEOS"
        pathView = findViewById(R.id.browserPath)
        emptyView = findViewById(R.id.browserEmpty)
        permPanel = findViewById(R.id.permPanel)
        listView = findViewById(R.id.browserList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val r = rows.getOrNull(position) ?: return@setOnItemClickListener
            when (r.type) {
                UP -> goUp()
                ROOT, DIR -> navigate(r.file, null)
                VIDEO -> r.file?.let { finishWith(it) }
            }
        }

        findViewById<View>(R.id.grantBtn).setOnClickListener { permLauncher.launch(permission()) }
        findViewById<View>(R.id.settingsBtn).setOnClickListener {
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Open this app's settings and allow storage / video access.", Toast.LENGTH_LONG).show()
            }
        }

        // Back goes up one folder; at the storage list it leaves the browser
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (browserShown && currentDir != null) goUp() else finish()
            }
        })

        if (hasPermission()) showBrowser() else {
            showPermissionPanel()
            permLauncher.launch(permission())
        }
    }

    override fun onResume() {
        super.onResume()
        if (!browserShown && hasPermission()) showBrowser()   // e.g. granted in Settings
    }

    private fun showPermissionPanel() {
        permPanel.visibility = View.VISIBLE
        findViewById<View>(R.id.grantBtn).requestFocus()
    }

    private fun showBrowser() {
        browserShown = true
        permPanel.visibility = View.GONE
        roots = StorageRoots.find(this)
        navigate(null, null)
    }

    private fun finishWith(file: File) {
        setResult(RESULT_OK, Intent().putExtra("uri", Uri.fromFile(file).toString()))
        finish()
    }

    private fun goUp() {
        val cur = currentDir ?: return
        val isRoot = roots.any { it.dir.absolutePath == cur.absolutePath }
        navigate(if (isRoot) null else cur.parentFile, cur)
    }

    // ---------- navigation ----------

    private fun navigate(dir: File?, select: File?) {
        val token = ++navToken
        currentDir = dir
        emptyView.text = "Loading…"
        emptyView.visibility = View.VISIBLE
        pathView.text = if (dir == null) "Choose a storage location" else pretty(dir)

        lister.execute {
            val built = if (dir == null) rootRows() else dirRows(dir)
            runOnUiThread {
                if (token != navToken || isFinishing) return@runOnUiThread
                rows.clear()
                rows.addAll(built.rows)
                adapter.notifyDataSetChanged()

                val onlyUp = rows.none { it.type != UP }
                emptyView.text = built.emptyMessage
                emptyView.visibility = if (onlyUp) View.VISIBLE else View.GONE
                if (dir != null) {
                    val n = built.videoCount
                    pathView.text = pretty(dir) + "   •   " + (if (n == 1) "1 video" else "$n videos")
                }

                var sel = select?.let { s -> rows.indexOfFirst { it.file == s } } ?: -1
                if (sel < 0) sel = rows.indexOfFirst { it.type != UP }.coerceAtLeast(0)
                listView.setSelection(sel)
                listView.requestFocus()          // no-op in touch mode; gives the remote a target
            }
        }
    }

    private fun pretty(dir: File): String {
        val root = roots
            .filter { dir.absolutePath.startsWith(it.dir.absolutePath) }
            .maxByOrNull { it.dir.absolutePath.length } ?: return dir.path
        val rel = dir.absolutePath.removePrefix(root.dir.absolutePath).trim('/')
        return if (rel.isEmpty()) root.label else root.label + " › " + rel.replace("/", " › ")
    }

    private fun rootRows(): Built {
        val list = mutableListOf<Row>()
        for (r in roots) list += Row(ROOT, r.dir, r.label, r.dir.absolutePath)
        val primary = roots.firstOrNull()?.dir
        if (primary != null) {
            for (name in listOf("Movies", "Download", "DCIM", "Documents")) {
                val d = File(primary, name)
                if (d.isDirectory && d.canRead()) list += Row(DIR, d, name, "Internal storage › $name")
            }
        }
        return Built(list, 0, "No storage found")
    }

    private fun dirRows(dir: File): Built {
        val isRoot = roots.any { it.dir.absolutePath == dir.absolutePath }
        val parent = dir.parentFile
        val up = Row(UP, null, "Up one level", if (isRoot || parent == null) "Storage list" else pretty(parent))

        val kids = dir.listFiles()
            ?: return Built(listOf(up), 0, "This folder can't be read")
        val byName = Comparator<File> { a, b -> VideoFiles.naturalCompare(a.name, b.name) }
        val dirs = kids.filter { it.isDirectory && !it.name.startsWith(".") && it.canRead() }.sortedWith(byName)
        val vids = kids.filter { VideoFiles.isVideo(it) }.sortedWith(byName)

        val out = mutableListOf(up)
        dirs.forEach { out += Row(DIR, it, it.name, "Folder") }
        vids.forEach { out += Row(VIDEO, it, it.name, "") }
        return Built(out, vids.size, "No videos or folders here")
    }

    // ---------- list rows ----------

    private fun baseInfo(f: File): String =
        VideoFiles.formatSize(f.length()) + "  •  " +
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(f.lastModified()))

    private fun applyMeta(h: Holder, f: File, m: VideoMetaLoader.Meta) {
        h.thumb.setImageBitmap(m.thumb)
        val parts = mutableListOf<String>()
        if (m.durationMs > 0) parts += VideoFiles.formatDuration(m.durationMs)
        if (m.width > 0 && m.height > 0) parts += "${m.width}×${m.height}"
        parts += baseInfo(f)
        h.sub.text = parts.joinToString("  •  ")
    }

    private fun bind(h: Holder, r: Row) {
        h.title.text = r.title
        h.path = null
        if (r.type == VIDEO && r.file != null) {
            val f = r.file
            val path = f.absolutePath
            h.path = path
            h.icon.visibility = View.GONE
            val cached = VideoMetaLoader.cached(path)
            if (cached != null) {
                applyMeta(h, f, cached)
            } else {
                h.thumb.setImageDrawable(null)
                h.sub.text = baseInfo(f)
                VideoMetaLoader.load(f, wanted = { h.path == path }) { meta ->
                    if (h.path == path) applyMeta(h, f, meta)
                }
            }
        } else {
            h.thumb.setImageDrawable(null)
            h.icon.visibility = View.VISIBLE
            h.icon.setImageResource(if (r.type == UP) R.drawable.ic_up else R.drawable.ic_folder)
            h.icon.setColorFilter(CYAN)
            h.sub.text = r.subtitle
        }
    }

    private inner class RowAdapter : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(position: Int): Any = rows[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(this@BrowserActivity)
                .inflate(R.layout.item_browse, parent, false)
                .also { it.tag = Holder(it) }
            bind(v.tag as Holder, rows[position])
            return v
        }
    }
}
