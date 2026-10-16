package com.example.quadplayer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Player screen. Receives 1-4 video URIs and a LayoutMode from SplashActivity. */
class MainActivity : AppCompatActivity(), TileHost {

    private val tiles = mutableListOf<VideoTile>()
    private var swapSource: VideoTile? = null
    private var folderRequester: VideoTile? = null

    // Back cancels an in-progress swap choice instead of leaving the player
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() { cancelSwap() }
    }

    // Folder access, used by the per-tile "loop whole folder" mode
    private val treePicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        val tile = folderRequester
        folderRequester = null
        if (tree != null && tile != null) {
            try {
                contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { /* fine for this session */ }
            if (!tile.tryUseFolder(tree)) {
                Toast.makeText(
                    this,
                    "That folder doesn't contain this video. Pick the folder where it is stored.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, backCallback)

        val uris = intent.getStringArrayListExtra("uris")?.map { Uri.parse(it) }?.take(4)
        if (uris.isNullOrEmpty()) { finish(); return }
        val mode = LayoutMode.parse(intent.getStringExtra("mode"), uris.size)

        // Fullscreen so the videos really fill the whole display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        fun addTile(parent: LinearLayout, uri: Uri) {
            val t = VideoTile(this, parent, uri, this, canSwap = uris.size > 1)
            tiles += t
            parent.addView(t.root, weighted(parent))
        }

        fun addGroup(parent: LinearLayout, orientation: Int): LinearLayout {
            val g = LinearLayout(this).apply { this.orientation = orientation }
            parent.addView(g, weighted(parent))
            return g
        }

        val H = LinearLayout.HORIZONTAL
        val V = LinearLayout.VERTICAL

        // HORIZONTAL root = children side by side (vertical dividers)
        // VERTICAL root   = children stacked (horizontal dividers)
        val root = LinearLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            orientation = when (mode) {
                LayoutMode.STACKED, LayoutMode.BIG_TOP, LayoutMode.GRID_2X2 -> V
                else -> H
            }
        }

        when (mode) {
            LayoutMode.SINGLE,
            LayoutMode.SIDE_BY_SIDE,
            LayoutMode.STACKED,
            LayoutMode.COLUMNS_3,
            LayoutMode.COLUMNS_4 -> uris.forEach { addTile(root, it) }

            // 50% left, the other two share the right half (25% each)
            LayoutMode.BIG_LEFT -> {
                addTile(root, uris[0])
                val g = addGroup(root, V)
                addTile(g, uris[1]); addTile(g, uris[2])
            }
            // 50% top, the other two share the bottom half
            LayoutMode.BIG_TOP -> {
                addTile(root, uris[0])
                val g = addGroup(root, H)
                addTile(g, uris[1]); addTile(g, uris[2])
            }
            // two 25% tiles stacked on the left, video 1 takes 50% on the right
            LayoutMode.BIG_RIGHT -> {
                val g = addGroup(root, V)
                addTile(g, uris[1]); addTile(g, uris[2])
                addTile(root, uris[0])
            }
            LayoutMode.GRID_2X2 -> {
                val top = addGroup(root, H)
                addTile(top, uris[0]); addTile(top, uris[1])
                val bottom = addGroup(root, H)
                addTile(bottom, uris[2]); addTile(bottom, uris[3])
            }
        }

        // Only as many tiles as the device has hardware video decoders start on hardware;
        // the rest use software decoders (TVs commonly allow just 2 hardware decoders).
        val hardwareLimit = DecoderPolicy.hardwareLimit(DecoderPolicy.isTv(this))
        tiles.forEachIndexed { i, t -> t.softwareMode = i >= hardwareLimit }

        setContentView(root)
    }

    // Tiles sit flush against each other: no margins, no borders
    private fun weighted(parent: LinearLayout) =
        if (parent.orientation == LinearLayout.HORIZONTAL)
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        else
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)

    // ---------------- swap ----------------

    override fun requestSwap(tile: VideoTile) {
        when {
            swapSource === tile -> cancelSwap()                       // pressed again = cancel
            tiles.size < 2 -> Unit
            tiles.size == 2 -> swapViews(tile, tiles.first { it !== tile })   // only one possible target
            else -> {
                // 3 or 4 videos: let the user pick the destination tile
                swapSource = tile
                backCallback.isEnabled = true
                for (t in tiles) {
                    if (t !== tile) t.setSwapTarget(true) { target -> onTargetChosen(target) }
                }
                tiles.first { it !== tile }.focusSwapTarget()
            }
        }
    }

    private fun onTargetChosen(target: VideoTile) {
        val src = swapSource ?: return
        finishSwapMode()
        // Post so the tap that chose the target finishes before views are moved
        window.decorView.post { swapViews(src, target) }
    }

    private fun cancelSwap() {
        val src = swapSource
        finishSwapMode()
        src?.refocusAfterSwap()
    }

    private fun finishSwapMode() {
        swapSource = null
        backCallback.isEnabled = false
        tiles.forEach { it.setSwapTarget(false, null) }
    }

    /** Exchanges the two tiles' positions in the grid. Their players keep playing. */
    private fun swapViews(a: VideoTile, b: VideoTile) {
        val pa = a.root.parent as? LinearLayout ?: return
        val pb = b.root.parent as? LinearLayout ?: return
        val ia = pa.indexOfChild(a.root)
        val ib = pb.indexOfChild(b.root)
        pa.removeView(a.root)
        pb.removeView(b.root)
        if (pa === pb) {
            if (ia < ib) {
                pa.addView(b.root, ia, weighted(pa)); pa.addView(a.root, ib, weighted(pa))
            } else {
                pa.addView(a.root, ib, weighted(pa)); pa.addView(b.root, ia, weighted(pa))
            }
        } else {
            pa.addView(b.root, ia, weighted(pa))
            pb.addView(a.root, ib, weighted(pb))
        }
        a.refocusAfterSwap()
    }

    // ---------------- folder looping ----------------

    override fun requestFolder(tile: VideoTile) {
        // Reuse folders the user has already granted (this session or earlier ones)
        val known = contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
            .map { it.uri }
        for (tree in known) if (tile.tryUseFolder(tree)) return

        folderRequester = tile
        Toast.makeText(this, "Choose the folder that contains this video", Toast.LENGTH_LONG).show()
        try {
            treePicker.launch(tile.hintUri())
        } catch (_: ActivityNotFoundException) {
            folderRequester = null
            Toast.makeText(this, "Folder access isn't available on this device.", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------- lifecycle ----------------

    override fun onStart() {
        super.onStart()
        tiles.forEach { it.start() }
        tiles.firstOrNull()?.focusPrimary()      // remote starts on the first video's play/pause
    }

    override fun onStop() {
        super.onStop()
        tiles.forEach { it.release() }
    }
}
