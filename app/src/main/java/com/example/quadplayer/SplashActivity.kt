package com.example.quadplayer

import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    private var count = 2
    private val modeForCount = mutableMapOf<Int, LayoutMode>()   // remembers the choice per count
    private val uris = arrayOfNulls<Uri>(4)
    private val names = arrayOfNulls<String>(4)
    private var pendingSlot = 0
    private var pendingBrowseSlot = -1       // -1 = "Browse videos" mode (play one video)
    private var restoreFocusTag: String? = null

    private lateinit var chips: List<TextView>
    private lateinit var layoutChips: LinearLayout
    private lateinit var layoutSection: View
    private lateinit var preview: LayoutPreview
    private lateinit var slots: LinearLayout
    private lateinit var startBtn: TextView

    private fun currentMode(): LayoutMode =
        modeForCount[count] ?: LayoutMode.forCount(count).first()

    /** Android TV / Fire TV: no touchscreen, usually no system file picker. */
    private fun isTvDevice(): Boolean {
        val ui = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ||
                packageManager.hasSystemFeature("amazon.hardware.fire_tv")
    }

    // System file picker (Files / Drive / Gallery / USB) - phones and tablets
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onPicked(uri, persist = true)
    }
    private val legacyPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onPicked(uri, persist = false)
    }

    // In-app browser - used on TVs, as a fallback, and for "Browse videos"
    private val browser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = res.data?.getStringExtra("uri")?.let { Uri.parse(it) }
        if (res.resultCode == RESULT_OK && uri != null) {
            if (pendingBrowseSlot >= 0) {
                pendingSlot = pendingBrowseSlot
                onPicked(uri, persist = false)
            } else {
                // Browse mode: play just this one video, full screen
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra("mode", LayoutMode.SINGLE.name)
                        .putStringArrayListExtra("uris", arrayListOf(uri.toString()))
                )
            }
        }
    }

    private fun openBrowser(slot: Int) {
        pendingBrowseSlot = slot
        browser.launch(
            Intent(this, BrowserActivity::class.java)
                .putExtra("title", if (slot >= 0) "SELECT VIDEO ${slot + 1}" else "BROWSE VIDEOS")
        )
    }

    private fun onPicked(uri: Uri?, persist: Boolean) {
        if (uri == null) return
        if (persist && uri.scheme == "content") {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { /* not offered by this provider; fine for this session */ }
        }
        uris[pendingSlot] = uri
        names[pendingSlot] = displayName(uri)
        restoreFocusTag = "slot:$pendingSlot"
        refresh()
    }

    private fun launchPicker(slot: Int) {
        pendingSlot = slot
        if (isTvDevice()) { openBrowser(slot); return }
        try {
            picker.launch(arrayOf("video/*"))
        } catch (_: ActivityNotFoundException) {
            try {
                legacyPicker.launch("video/*")
            } catch (_: ActivityNotFoundException) {
                openBrowser(slot)                 // no system picker at all: use the built-in one
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setupCreditLink()
        findViewById<View>(R.id.browseBtn).setOnClickListener { openBrowser(-1) }

        chips = listOf(R.id.chip1, R.id.chip2, R.id.chip3, R.id.chip4).map { findViewById<TextView>(it) }
        chips.forEachIndexed { i, chip ->
            chip.setOnClickListener { count = i + 1; refresh() }
        }
        layoutChips = findViewById(R.id.layoutChips)
        layoutSection = findViewById(R.id.splitSection)
        preview = findViewById(R.id.preview)
        slots = findViewById(R.id.slots)
        startBtn = findViewById(R.id.startBtn)

        startBtn.setOnClickListener {
            val chosen = uris.take(count).filterNotNull().map { it.toString() }
            if (chosen.size == count) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra("mode", currentMode().name)
                        .putStringArrayListExtra("uris", ArrayList(chosen))
                )
            }
        }
        refresh()
        findViewById<View>(R.id.browseBtn).requestFocus()   // remote starts on Browse
    }

    private fun setupCreditLink() {
        val link = findViewById<TextView>(R.id.creditLink)
        val full = "Created by https://burnsymbol.com"
        val start = full.indexOf("https")
        link.text = SpannableString(full).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@SplashActivity, R.color.cyan)),
                start, full.length, 0)
            setSpan(UnderlineSpan(), start, full.length, 0)
        }
        // A plain click handler (not autoLink) so it also works with a TV remote's OK button
        link.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://burnsymbol.com")))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No web browser found on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refresh() {
        // Remember which control had remote focus: lists are rebuilt below
        val focusTag = restoreFocusTag ?: (currentFocus?.tag as? String)
        restoreFocusTag = null

        chips.forEachIndexed { i, c -> c.isSelected = (i + 1 == count) }

        // Layout options for this video count, two chips per row
        val mode = currentMode()
        val options = LayoutMode.forCount(count)
        layoutSection.visibility = if (options.size > 1) View.VISIBLE else View.GONE
        layoutChips.removeAllViews()
        for (group in options.chunked(2)) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (opt in group) {
                val chip = LayoutInflater.from(this).inflate(R.layout.item_chip, row, false) as TextView
                chip.text = opt.label
                chip.tag = "layout:${opt.name}"
                chip.isSelected = (opt == mode)
                chip.setOnClickListener { modeForCount[count] = opt; refresh() }
                row.addView(chip)
            }
            layoutChips.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
        preview.set(mode)

        slots.removeAllViews()
        for (i in 0 until count) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_slot, slots, false)
            val picked = uris[i] != null
            row.tag = "slot:$i"
            row.findViewById<TextView>(R.id.slotNum).apply { text = "${i + 1}"; isSelected = picked }
            row.findViewById<TextView>(R.id.slotTitle).text = "Video ${i + 1}"
            row.findViewById<TextView>(R.id.slotName).text = names[i] ?: "No file selected"
            row.findViewById<TextView>(R.id.slotAction).text = if (picked) "CHANGE" else "SELECT"
            row.setOnClickListener { launchPicker(i) }
            slots.addView(row)
        }

        val missing = (0 until count).count { uris[it] == null }
        startBtn.isEnabled = missing == 0
        startBtn.text = when (missing) {
            0 -> "START PLAYING"
            1 -> "SELECT 1 MORE VIDEO"
            else -> "SELECT $missing MORE VIDEOS"
        }

        if (focusTag != null) window.decorView.findViewWithTag<View>(focusTag)?.requestFocus()
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?: uri.lastPathSegment
            ?: "Video"
}
