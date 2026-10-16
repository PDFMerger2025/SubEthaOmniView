package com.example.quadplayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView

/** ONE = repeat this file, FOLDER = play the folder's videos in order and wrap around, OFF = play once. */
enum class LoopMode { ONE, FOLDER, OFF }

/**
 * One grid cell: its own ExoPlayer, controller, volume slider and buttons (loop, speed, rotate,
 * fill/fit, swap). Controls float over the video and auto-hide 5 s after the last touch or
 * remote key. Fully usable with a D-pad remote (Android TV / Fire TV).
 */
@SuppressLint("ClickableViewAccessibility")
class VideoTile(
    private val context: Context,
    parent: ViewGroup,
    private val uri: Uri,
    private val host: TileHost,
    canSwap: Boolean
) {
    val root: View = LayoutInflater.from(context).inflate(R.layout.tile, parent, false)

    private val frame = root as TileFrame
    private val playerView = root.findViewById<PlayerView>(R.id.playerView)
    private val overlay = root.findViewById<View>(R.id.overlay)
    private val focusRing = root.findViewById<View>(R.id.focusRing)
    private val controls = root.findViewById<PlayerControlView>(R.id.controls)
    private val volumeSeek = root.findViewById<SeekBar>(R.id.volumeSeek)
    private val volumeLabel = root.findViewById<TextView>(R.id.volumeLabel)
    private val muteBtn = root.findViewById<ImageButton>(R.id.muteBtn)
    private val loopBtn = root.findViewById<ImageButton>(R.id.loopBtn)
    private val speedBtn = root.findViewById<TextView>(R.id.speedBtn)
    private val rotateBtn = root.findViewById<ImageButton>(R.id.rotateBtn)
    private val zoomBtn = root.findViewById<ImageButton>(R.id.zoomBtn)
    private val swapBtn = root.findViewById<ImageButton>(R.id.swapBtn)
    private val swapTarget = root.findViewById<TextView>(R.id.swapTarget)

    private var player: ExoPlayer? = null
    private var savedPosition = 0L
    private var savedPlayWhenReady = true

    // What is playing right now (changes when looping through a folder)
    private var currentUri: Uri = uri
    private var currentName: String = displayName(uri)

    // Volume: starts at 0% so four soundtracks don't collide
    private var volumePercent = 0
    private var lastAudiblePercent = 100

    // Speed, rotation, fill
    private val speeds = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    private var speedIndex = 2
    private var rotationDeg = 0
    private var fillMode = false

    // Looping
    private var loopMode = LoopMode.ONE
    private var folderTree: Uri? = null
    private var folderEntries: List<FolderEntry>? = null

    // Pinch zoom / pan
    private var scale = 1f
    private var tx = 0f
    private var ty = 0f
    private var gestureArmed = false

    // Auto-hide / remote focus / swap mode
    private val handler = Handler(Looper.getMainLooper())
    private var touching = false
    private var parkingFocus = false
    private var swapTargetMode = false
    private var compactPlay: Boolean? = null
    private val hideRunnable = Runnable { hideOverlay() }

    /** True once this tile uses software decoders (set up front by the screen, or after a failure). */
    var softwareMode = false
    private var firstFrameRendered = false
    private var errorShown = false
    private val watchdog = Runnable { checkStalledDecoder() }

    private class FolderEntry(val name: String, val uri: Uri)

    private val itemListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            currentName = mediaItem.mediaId
            mediaItem.localConfiguration?.uri?.let { currentUri = it }
        }

        override fun onRenderedFirstFrame() {
            firstFrameRendered = true
            handler.removeCallbacks(watchdog)
        }

        override fun onPlayerError(error: PlaybackException) {
            handler.removeCallbacks(watchdog)
            if (!softwareMode) {
                switchToSoftware()                 // hardware decoder ran out / failed: retry in software
            } else if (!errorShown) {
                errorShown = true
                Toast.makeText(
                    context,
                    "This video can't be played on this device (${error.errorCodeName})",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                applyZoom(scale * d.scaleFactor, tx, ty)
                return true
            }
        })

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (scale > 1f && !scaleDetector.isInProgress) applyZoom(scale, tx - dx, ty - dy)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                applyZoom(1f, 0f, 0f)   // double-tap resets pinch zoom
                return true
            }
        })

    init {
        // ---- touch: pinch-zoom, pan, double-tap on the bare video ----
        frame.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev)
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) gestureArmed = true
            if (gestureArmed) gestureDetector.onTouchEvent(ev)
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
                gestureArmed = false
            }
            true
        }
        frame.touchObserver = { onTileTouch(it) }

        // ---- remote: keys + focus ----
        frame.keyObserver = { onTileKey(it) }
        frame.frameFocusGained = {
            if (!parkingFocus && !swapTargetMode) { showOverlay(); focusPrimary() }
        }
        frame.viewTreeObserver.addOnGlobalFocusChangeListener { _, _ -> updateFocusRing() }

        // ---- icon tint ----
        for (b in listOf(muteBtn, rotateBtn, zoomBtn, swapBtn)) b.setColorFilter(CYAN)
        rotateBtn.setImageResource(R.drawable.ic_rotate)
        swapBtn.setImageResource(R.drawable.ic_swap)
        swapBtn.visibility = if (canSwap) View.VISIBLE else View.GONE

        // ---- volume slider + mute ----
        volumeSeek.keyProgressIncrement = 5          // D-pad left/right = 5% per press
        volumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) applyVolume(progress)
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        muteBtn.setOnClickListener {
            applyVolume(if (volumePercent > 0) 0 else lastAudiblePercent)
        }
        applyVolume(0)

        // ---- speed ----
        speedBtn.setOnClickListener {
            speedIndex = (speedIndex + 1) % speeds.size
            player?.setPlaybackSpeed(speeds[speedIndex])
            updateSpeedLabel()
        }
        updateSpeedLabel()

        // ---- rotate ----
        rotateBtn.setOnClickListener {
            rotationDeg = (rotationDeg + 90) % 360
            applyRotation()
        }

        // ---- fill / fit ----
        zoomBtn.setOnClickListener { fillMode = !fillMode; updateFillMode() }
        updateFillMode()

        // ---- loop ----
        loopBtn.setOnClickListener { cycleLoop() }
        updateLoopButton()

        // ---- swap ----
        swapBtn.setOnClickListener { host.requestSwap(this) }

        // Fit controls and rotation to the tile size
        frame.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            adaptControls(right - left, bottom - top)
            applyRotation()
        }
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    /** Looks up a view inside the Media3 controller layout by its resource name. */
    private fun controllerView(name: String): View? {
        val id = context.resources.getIdentifier(name, "id", context.packageName)
        return if (id != 0) controls.findViewById(id) else null
    }

    fun focusPrimary() { controllerView("exo_play_pause")?.requestFocus() }

    /** Used by the swap hint when opening the folder picker. */
    fun hintUri(): Uri = currentUri

    // ---------- adapt to tile size ----------

    private fun adaptControls(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        // volume slider
        val labelVisible = w >= dp(150)
        volumeLabel.visibility = if (labelVisible) View.VISIBLE else View.GONE
        val labelW = if (labelVisible) dp(34) else 0
        val sliderW = (w * 0.5f).toInt().coerceIn(dp(70), dp(220))
            .coerceAtMost(w - dp(12) - labelW).coerceAtLeast(dp(40))
        if (volumeSeek.layoutParams.width != sliderW) {
            volumeSeek.layoutParams = volumeSeek.layoutParams.apply { width = sliderW }
        }

        // Would the button cluster collide with a centered play button? If so, tuck play bottom-left.
        val count = listOf(muteBtn, loopBtn, speedBtn, rotateBtn, zoomBtn, swapBtn).count { it.visibility != View.GONE }
        val unit = dp(40)
        val perRow = (((w - dp(12) + dp(4)) / unit).coerceAtLeast(1)).coerceAtMost(count.coerceAtLeast(1))
        val rows = (count + perRow - 1) / perRow
        val clusterW = maxOf(sliderW + labelW, perRow * unit - dp(4))
        val clusterBottom = dp(6) + dp(36) + dp(2) + rows * unit
        val half = dp(26)
        val overlaps = (w - dp(6) - clusterW) < (w / 2 + half) && clusterBottom > (h / 2 - half)
        placePlayButton(overlaps)

        // Very narrow tile: drop the time labels so the progress bar keeps some width
        val vis = if (w < dp(150)) View.GONE else View.VISIBLE
        controllerView("exo_position")?.visibility = vis
        controllerView("exo_duration")?.visibility = vis
    }

    private fun placePlayButton(compact: Boolean) {
        if (compactPlay == compact) return
        val btn = controllerView("exo_play_pause") ?: return
        val lp = btn.layoutParams as? FrameLayout.LayoutParams ?: return
        if (compact) {
            lp.width = dp(40); lp.height = dp(40)
            lp.gravity = Gravity.BOTTOM or Gravity.START
            lp.setMargins(dp(8), 0, 0, dp(32))
        } else {
            lp.width = dp(52); lp.height = dp(52)
            lp.gravity = Gravity.CENTER
            lp.setMargins(0, 0, 0, 0)
        }
        btn.layoutParams = lp
        compactPlay = compact
    }

    // ---------- rotation ----------

    /** Rotates the video in 90-degree steps; for 90/270 the view is sized to the tile's swapped
     *  dimensions so the rotated picture exactly fills the tile. */
    private fun applyRotation() {
        val w = frame.width
        val h = frame.height
        if (w == 0 || h == 0) return
        val swap = rotationDeg % 180 != 0
        val lp = playerView.layoutParams as? FrameLayout.LayoutParams ?: return
        val tw = if (swap) h else ViewGroup.LayoutParams.MATCH_PARENT
        val th = if (swap) w else ViewGroup.LayoutParams.MATCH_PARENT
        if (lp.width != tw || lp.height != th || lp.gravity != Gravity.CENTER) {
            lp.width = tw; lp.height = th; lp.gravity = Gravity.CENTER
            playerView.layoutParams = lp
        }
        playerView.rotation = rotationDeg.toFloat()
    }

    // ---------- speed ----------

    private fun updateSpeedLabel() {
        val s = speeds[speedIndex]
        speedBtn.text = if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"
    }

    // ---------- fill / fit ----------

    private fun updateFillMode() {
        playerView.resizeMode =
            if (fillMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
        zoomBtn.setImageResource(if (fillMode) R.drawable.ic_fit else R.drawable.ic_fill)
        zoomBtn.contentDescription = if (fillMode) "Fit video (show letterbox)" else "Fill screen (remove letterbox)"
    }

    // ---------- looping ----------

    private fun cycleLoop() {
        when (loopMode) {
            LoopMode.ONE -> {
                // Videos from the in-app browser are plain files: the folder is just the parent directory
                if (currentUri.scheme == "file") { useFileFolder(); return }
                // Re-read the folder each time so newly added videos are picked up
                val tree = folderTree
                if (tree != null && tryUseFolder(tree)) return
                host.requestFolder(this)           // asks for folder access; result arrives in tryUseFolder
            }
            LoopMode.FOLDER -> setLoop(LoopMode.OFF)
            LoopMode.OFF -> setLoop(LoopMode.ONE)
        }
    }

    private fun setLoop(mode: LoopMode) {
        loopMode = mode
        applyLoop()
        updateLoopButton()
        val msg = when (mode) {
            LoopMode.ONE -> "Loop: this video"
            LoopMode.FOLDER -> "Loop: whole folder (${folderEntries?.size ?: 0} videos)"
            LoopMode.OFF -> "Loop: off"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Called by the host with a folder (document tree) it has access to.
     * @return true if this tile's video lives in that folder and folder-looping is now on.
     */
    fun tryUseFolder(tree: Uri): Boolean {
        val entries = listFolderVideos(tree)
        if (entries.none { it.name == currentName }) return false
        folderTree = tree
        folderEntries = entries
        setLoop(LoopMode.FOLDER)
        return true
    }

    /** Local file (in-app browser): loop the videos sitting in the same directory. */
    private fun useFileFolder() {
        val dir = currentUri.path?.let { java.io.File(it).parentFile }
        val entries = dir?.listFiles()
            ?.filter { VideoFiles.isVideo(it) }
            ?.sortedWith { a, b -> VideoFiles.naturalCompare(a.name, b.name) }
            ?.map { FolderEntry(it.name, Uri.fromFile(it)) }
            ?: emptyList()
        if (entries.none { it.name == currentName }) {
            Toast.makeText(context, "Couldn't read this video's folder", Toast.LENGTH_SHORT).show()
            return
        }
        folderEntries = entries
        setLoop(LoopMode.FOLDER)
    }

    private fun updateLoopButton() {
        loopBtn.setImageResource(if (loopMode == LoopMode.ONE) R.drawable.ic_loop_one else R.drawable.ic_loop_all)
        loopBtn.setColorFilter(if (loopMode == LoopMode.OFF) 0x66FFFFFF else CYAN)
        loopBtn.contentDescription = when (loopMode) {
            LoopMode.ONE -> "Loop this video. Press for folder loop"
            LoopMode.FOLDER -> "Loop whole folder. Press to turn loop off"
            LoopMode.OFF -> "Loop off. Press to loop this video"
        }
    }

    private fun repeatModeFor(mode: LoopMode) = when (mode) {
        LoopMode.ONE -> Player.REPEAT_MODE_ONE
        LoopMode.FOLDER -> Player.REPEAT_MODE_ALL
        LoopMode.OFF -> Player.REPEAT_MODE_OFF
    }

    private fun applyLoop() {
        val p = player ?: return
        val entries = folderEntries
        if (loopMode == LoopMode.FOLDER && entries != null) {
            val idx = entries.indexOfFirst { it.name == currentName }.coerceAtLeast(0)
            val pos = if (p.mediaItemCount > 0) p.currentPosition else 0L
            p.setMediaItems(entries.map { itemFor(it.uri, it.name) }, idx, pos)
        } else if (p.mediaItemCount > 1) {
            // Leaving folder mode: keep just the video that is playing, at its current position
            p.currentMediaItem?.let { p.setMediaItem(it, p.currentPosition) }
        }
        p.repeatMode = repeatModeFor(loopMode)
    }

    private fun itemFor(u: Uri, name: String) =
        MediaItem.Builder().setUri(u).setMediaId(name).build()

    private fun listFolderVideos(tree: Uri): List<FolderEntry> {
        val out = mutableListOf<FolderEntry>()
        try {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                tree, DocumentsContract.getTreeDocumentId(tree)
            )
            context.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val mime = c.getString(2) ?: ""
                    if (mime.startsWith("video/")) {
                        out += FolderEntry(
                            c.getString(1) ?: continue,
                            DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                        )
                    }
                }
            }
        } catch (_: Exception) { /* no access / not a tree: treated as "not in this folder" */ }
        return out.sortedWith { a, b -> VideoFiles.naturalCompare(a.name, b.name) }
    }

    private fun displayName(u: Uri): String = try {
        context.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (_: Exception) { null } ?: u.lastPathSegment ?: "video"

    // ---------- swap ----------

    /** While choosing a swap destination, every other tile shows a big "SWAP HERE" pad. */
    fun setSwapTarget(show: Boolean, onChoose: ((VideoTile) -> Unit)?) {
        swapTargetMode = show
        if (show) {
            handler.removeCallbacks(hideRunnable)
            overlay.animate().cancel()
            overlay.visibility = View.GONE
            controls.hide()
            swapTarget.visibility = View.VISIBLE
            swapTarget.setOnClickListener { onChoose?.invoke(this) }
        } else {
            swapTarget.visibility = View.GONE
            swapTarget.setOnClickListener(null)
        }
    }

    fun focusSwapTarget() { swapTarget.requestFocus() }

    /** After a swap (or cancel) put the controls and remote focus back on the swap button. */
    fun refocusAfterSwap() {
        showOverlay()
        swapBtn.requestFocus()
    }

    // ---------- focus ring (remote only) ----------

    private fun updateFocusRing() {
        focusRing.visibility =
            if (frame.hasFocus() && !frame.isInTouchMode && !swapTargetMode) View.VISIBLE else View.GONE
    }

    // ---------- touch / key -> auto-hide ----------

    private fun onTileTouch(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { touching = true; showOverlay() }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { touching = false; showOverlay() }
        }
    }

    /** @return true if the key was consumed here */
    private fun onTileKey(ev: KeyEvent): Boolean {
        if (swapTargetMode) return false                       // the SWAP HERE pad handles OK itself
        val code = ev.keyCode

        when (code) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                if (ev.action == KeyEvent.ACTION_DOWN) { handleMediaKey(code); showOverlay() }
                return true
            }
        }

        val isNav = code in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (!isNav) return false                               // BACK etc. pass straight through

        if (overlay.visibility != View.VISIBLE) {
            // Controls hidden: the first press only wakes them (and focuses play/pause)
            if (ev.action == KeyEvent.ACTION_DOWN) { showOverlay(); focusPrimary() }
            return true
        }
        if (ev.action == KeyEvent.ACTION_DOWN) showOverlay()   // keep them up while navigating
        return false
    }

    private fun handleMediaKey(code: Int) {
        val p = player ?: return
        when (code) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (p.playWhenReady) p.pause() else p.play()
            KeyEvent.KEYCODE_MEDIA_PLAY -> p.play()
            KeyEvent.KEYCODE_MEDIA_PAUSE -> p.pause()
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> p.seekTo(p.currentPosition + 10_000L)
            KeyEvent.KEYCODE_MEDIA_REWIND -> p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
        }
    }

    private fun showOverlay() {
        if (swapTargetMode) return
        handler.removeCallbacks(hideRunnable)
        overlay.animate().cancel()
        overlay.alpha = 1f
        overlay.visibility = View.VISIBLE
        if (controls.visibility != View.VISIBLE) controls.show()
        updateFocusRing()
        handler.postDelayed(hideRunnable, HIDE_DELAY_MS)
    }

    private fun hideOverlay() {
        if (touching) {                       // finger still down (e.g. dragging a slider)
            handler.postDelayed(hideRunnable, 1000)
            return
        }
        overlay.animate().alpha(0f).setDuration(300).withEndAction {
            // If a control had remote focus, park it on the tile itself so the remote keeps working
            if (frame.hasFocus()) {
                parkingFocus = true
                frame.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                frame.requestFocus()
                frame.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                parkingFocus = false
            }
            overlay.visibility = View.GONE    // GONE so touches reach the video again
            controls.hide()
        }.start()
    }

    // ---------- volume ----------

    // ---------- decoders ----------

    /** A silent decoder (no error, no picture) is how some TVs fail when out of hardware decoders. */
    private fun checkStalledDecoder() {
        val p = player ?: return
        val state = p.playbackState
        if (!softwareMode && !firstFrameRendered &&
            p.currentTracks.containsType(C.TRACK_TYPE_VIDEO) &&
            (state == Player.STATE_READY || state == Player.STATE_BUFFERING)
        ) switchToSoftware()
    }

    private fun switchToSoftware() {
        if (softwareMode) return
        softwareMode = true
        // Restart this tile's player (position and settings are kept by release()/start())
        handler.post { if (player != null) { release(); start() } }
    }

    /** With volume at 0% the audio decoder isn't needed: skipping it saves a codec instance. */
    private fun applyAudioTrack(p: ExoPlayer?) {
        val pl = p ?: return
        val disable = volumePercent == 0
        if (pl.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO) != disable) {
            pl.trackSelectionParameters = pl.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, disable)
                .build()
        }
    }

    private fun applyVolume(percent: Int) {
        volumePercent = percent.coerceIn(0, 100)
        if (volumePercent > 0) lastAudiblePercent = volumePercent
        player?.volume = volumePercent / 100f
        applyAudioTrack(player)
        if (volumeSeek.progress != volumePercent) volumeSeek.progress = volumePercent
        volumeLabel.text = "$volumePercent%"
        muteBtn.setImageResource(
            if (volumePercent == 0) android.R.drawable.ic_lock_silent_mode
            else android.R.drawable.ic_lock_silent_mode_off
        )
    }

    // ---------- pinch zoom ----------

    private fun applyZoom(newScale: Float, x: Float, y: Float) {
        scale = newScale.coerceIn(1f, 5f)
        val maxX = (scale - 1f) * frame.width / 2f
        val maxY = (scale - 1f) * frame.height / 2f
        tx = x.coerceIn(-maxX, maxX)
        ty = y.coerceIn(-maxY, maxY)
        playerView.scaleX = scale
        playerView.scaleY = scale
        playerView.translationX = tx
        playerView.translationY = ty
    }

    // ---------- player lifecycle ----------

    fun start() {
        if (player != null) return
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)                      // try the next decoder if one won't start
            .setMediaCodecSelector(DecoderPolicy.selector(softwareMode))
        val p = ExoPlayer.Builder(context, renderers).build()
        // handleAudioFocus = false, otherwise the players would pause each other
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ false
        )
        p.volume = volumePercent / 100f
        p.setPlaybackSpeed(speeds[speedIndex])
        p.addListener(itemListener)

        val entries = folderEntries
        if (loopMode == LoopMode.FOLDER && entries != null) {
            val idx = entries.indexOfFirst { it.name == currentName }.coerceAtLeast(0)
            p.setMediaItems(entries.map { itemFor(it.uri, it.name) }, idx, savedPosition)
        } else {
            p.setMediaItem(itemFor(currentUri, currentName), savedPosition)
        }
        p.repeatMode = repeatModeFor(loopMode)
        p.playWhenReady = savedPlayWhenReady
        p.prepare()
        applyAudioTrack(p)

        firstFrameRendered = false
        handler.removeCallbacks(watchdog)
        if (!softwareMode) handler.postDelayed(watchdog, STALL_TIMEOUT_MS)

        playerView.player = p
        controls.player = p
        player = p
        showOverlay()
    }

    fun release() {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(watchdog)
        val p = player ?: return
        savedPosition = p.currentPosition
        savedPlayWhenReady = p.playWhenReady
        playerView.player = null
        controls.player = null
        p.removeListener(itemListener)
        p.release()
        player = null
    }

    private companion object {
        const val HIDE_DELAY_MS = 5000L
        const val STALL_TIMEOUT_MS = 8000L
        val CYAN = 0xFF00E5FF.toInt()
    }
}
