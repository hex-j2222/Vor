package com.nebula.editor.ui.editor

import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nebula.editor.R
import com.nebula.editor.databinding.ActivityEditorBinding
import com.nebula.editor.model.*
import com.nebula.editor.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Controls all timeline interactions:
 * - Renders tracks and clips dynamically
 * - Handles clip drag, trim (left/right handles), snap-to-magnet
 * - Manages playhead drag
 * - Zoom in/out (frame-level precision)
 * - 100+ track support with auto-collapse
 */
class TimelineController(
    private val activity: EditorActivity,
    private val binding: ActivityEditorBinding,
    private val viewModel: EditorViewModel,
) {
    private val context: Context = activity

    // px per millisecond at zoom=1
    private val BASE_PX_PER_MS = 0.05f
    private var pxPerMs = BASE_PX_PER_MS

    private val TRACK_HEIGHT_DP = 44
    private val LABEL_WIDTH_DP  = 88
    private val trackHeightPx   get() = dpToPx(TRACK_HEIGHT_DP)
    private val labelWidthPx    get() = dpToPx(LABEL_WIDTH_DP)

    // Magnet snap distance in px
    private val SNAP_THRESHOLD_PX = dpToPx(10)

    private var isDraggingClip   = false
    private var isDraggingHandle = false // left/right trim
    private var isDraggingPlayhead = false

    private var draggingClipId: String? = null
    private var draggingTrackView: ClipView? = null
    private var dragStartX = 0f
    private var dragOriginalStartMs = 0L

    init {
        setup()
        observe()
    }

    // ── Setup ─────────────────────────────────────────────────

    private fun setup() {
        binding.btnZoomIn.setOnClickListener  { viewModel.zoomIn() }
        binding.btnZoomOut.setOnClickListener { viewModel.zoomOut() }
        binding.btnMagnet.setOnClickListener  { viewModel.toggleMagnet() }
        binding.btnAddTrack.setOnClickListener {
            showAddTrackMenu()
        }

        // Playhead drag on ruler
        binding.timelineRuler.setOnTouchListener { _, event ->
            handlePlayheadTouch(event)
            true
        }
    }

    private fun observe() {
        val owner = activity as LifecycleOwner

        owner.lifecycleScope.launch {
            viewModel.project.collect { project ->
                project ?: return@collect
                pxPerMs = BASE_PX_PER_MS * viewModel.timelineZoom.value
                rebuildTimeline(project)
            }
        }

        owner.lifecycleScope.launch {
            viewModel.timelineZoom.collect { zoom ->
                pxPerMs = BASE_PX_PER_MS * zoom
                viewModel.project.value?.let { rebuildTimeline(it) }
                binding.tvZoomLabel.text = "${zoom}x"
            }
        }

        owner.lifecycleScope.launch {
            viewModel.playheadMs.collect { ms ->
                updatePlayheadPosition(ms)
            }
        }

        owner.lifecycleScope.launch {
            viewModel.magnetEnabled.collect { enabled ->
                binding.btnMagnet.isSelected = enabled
            }
        }

        owner.lifecycleScope.launch {
            viewModel.selectedClipId.collect { clipId ->
                highlightClip(clipId)
            }
        }
    }

    // ── Rebuild timeline ──────────────────────────────────────

    private fun rebuildTimeline(project: Project) {
        val container = binding.timelineTracksContainer
        container.removeAllViews()

        val totalMs     = project.durationMs.coerceAtLeast(30_000L)
        val totalWidthPx = (totalMs * pxPerMs).toInt() + labelWidthPx

        // Set ruler width
        binding.timelineRuler.post {
            (binding.timelineRuler.layoutParams as? ViewGroup.LayoutParams)?.width = totalWidthPx
            buildRuler(totalMs, totalWidthPx)
        }

        // Build each track row
        project.tracks.forEachIndexed { _, track ->
            val row = buildTrackRow(track, totalWidthPx)
            container.addView(row)
        }

        // Playhead overlay
        updatePlayheadPosition(viewModel.playheadMs.value)
    }

    private fun buildTrackRow(track: TimelineTrack, totalWidthPx: Int): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                trackHeightPx
            )
        }

        // Label
        val label = buildTrackLabel(track)
        row.addView(label)

        // Track content area
        val content = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                totalWidthPx - labelWidthPx,
                trackHeightPx
            )
        }

        // Add clips
        track.clips.forEach { clip ->
            val clipView = ClipView(context, clip, track.type)
            val leftPx   = (clip.startTimeMs * pxPerMs).toInt()
            val widthPx  = ((clip.endTimeMs - clip.startTimeMs) * pxPerMs).toInt().coerceAtLeast(dpToPx(20))

            val lp = FrameLayout.LayoutParams(widthPx, trackHeightPx - dpToPx(8))
            lp.leftMargin = leftPx
            lp.topMargin  = dpToPx(4)
            clipView.layoutParams = lp

            setupClipTouch(clipView, clip)
            content.addView(clipView)
        }

        row.addView(content)
        return row
    }

    private fun buildTrackLabel(track: TimelineTrack): View {
        val label = LayoutInflater.from(context)
            .inflate(R.layout.item_track_label, null) as ViewGroup
        label.layoutParams = LinearLayout.LayoutParams(labelWidthPx, trackHeightPx)

        label.findViewById<View>(R.id.trackColorDot)
            .setBackgroundColor(trackColor(track.type))

        label.findViewById<android.widget.TextView>(R.id.tvTrackName).text = track.label

        label.findViewById<android.widget.ImageButton>(R.id.btnTrackMute).apply {
            isSelected = track.isMuted
            setOnClickListener { viewModel.setTrackMute(track.id, !track.isMuted) }
        }

        return label
    }

    // ── Clip touch / drag / trim ──────────────────────────────

    private fun setupClipTouch(clipView: ClipView, clip: TimelineClip) {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                viewModel.selectClip(clip.id)
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Double tap → split at playhead
                viewModel.splitClipAtPlayhead()
                return true
            }
        })

        clipView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            handleClipDrag(v as ClipView, clip, event)
            true
        }
    }

    private fun handleClipDrag(view: ClipView, clip: TimelineClip, event: MotionEvent) {
        val HANDLE_ZONE_PX = dpToPx(16)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragOriginalStartMs = clip.startTimeMs
                draggingClipId = clip.id

                isDraggingHandle = event.x < HANDLE_ZONE_PX || event.x > view.width - HANDLE_ZONE_PX
                isDraggingClip   = !isDraggingHandle
                view.setDragging(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - dragStartX
                val deltaMs = (dx / pxPerMs).toLong()

                if (isDraggingClip) {
                    val newStartMs = (dragOriginalStartMs + deltaMs).coerceAtLeast(0)
                    val snapped = if (viewModel.magnetEnabled.value)
                        snapToNeighbor(newStartMs, clip.id)
                    else newStartMs

                    // Move view visually
                    val lp = view.layoutParams as FrameLayout.LayoutParams
                    lp.leftMargin = (snapped * pxPerMs).toInt()
                    view.layoutParams = lp

                    if (snapped != newStartMs) vibrate()

                } else {
                    // Trim left handle
                    if (event.x < HANDLE_ZONE_PX) {
                        val newStart = (dragOriginalStartMs + deltaMs).coerceAtLeast(0)
                            .coerceAtMost(clip.endTimeMs - 500)
                        val lp = view.layoutParams as FrameLayout.LayoutParams
                        val oldLeft = lp.leftMargin
                        lp.leftMargin = (newStart * pxPerMs).toInt()
                        lp.width = lp.width + (oldLeft - lp.leftMargin)
                        view.layoutParams = lp
                    } else {
                        // Trim right handle
                        val newEnd = (clip.startTimeMs + (view.width / pxPerMs).toLong() + deltaMs)
                            .coerceAtLeast(clip.startTimeMs + 500)
                        val lp = view.layoutParams as FrameLayout.LayoutParams
                        lp.width = ((newEnd - clip.startTimeMs) * pxPerMs).toInt().coerceAtLeast(dpToPx(20))
                        view.layoutParams = lp
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.setDragging(false)
                isDraggingClip   = false
                isDraggingHandle = false

                // Commit position to ViewModel
                val lp = view.layoutParams as FrameLayout.LayoutParams
                val newStartMs = (lp.leftMargin / pxPerMs).toLong()
                val newEndMs   = newStartMs + (lp.width / pxPerMs).toLong()

                if (newStartMs != clip.startTimeMs || newEndMs != clip.endTimeMs) {
                    viewModel.trimClip(clip.id, newStartMs, newEndMs)
                }
                draggingClipId = null
            }
        }
    }

    // ── Magnet snap ───────────────────────────────────────────

    private fun snapToNeighbor(timeMs: Long, excludeClipId: String): Long {
        val allClips = viewModel.project.value?.tracks
            ?.flatMap { it.clips }
            ?.filter { it.id != excludeClipId }
            ?: return timeMs

        val snapTargets = allClips.flatMap { listOf(it.startTimeMs, it.endTimeMs) }
        val thresholdMs = (SNAP_THRESHOLD_PX / pxPerMs).toLong()

        val nearest = snapTargets.minByOrNull { kotlin.math.abs(it - timeMs) } ?: return timeMs
        return if (kotlin.math.abs(nearest - timeMs) <= thresholdMs) nearest else timeMs
    }

    // ── Playhead ──────────────────────────────────────────────

    private fun handlePlayheadTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val scrollX = (binding.timelineScrollView as? HorizontalScrollView)?.scrollX ?: 0
                val rawX    = event.x + scrollX - labelWidthPx
                val ms      = (rawX / pxPerMs).toLong().coerceAtLeast(0)
                viewModel.setPlayhead(ms)
            }
        }
    }

    private fun updatePlayheadPosition(ms: Long) {
        val xPx = labelWidthPx + (ms * pxPerMs).toInt()
        binding.playheadView.post {
            (binding.playheadView.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin = xPx
            binding.playheadView.requestLayout()
        }
    }

    // ── Ruler ─────────────────────────────────────────────────

    private fun buildRuler(totalMs: Long, totalWidthPx: Int) {
        val ruler = binding.timelineRuler
        ruler.removeAllViews()

        // Decide tick interval based on zoom
        val tickIntervalMs: Long = when {
            pxPerMs >= 0.5f  -> 500L
            pxPerMs >= 0.1f  -> 1_000L
            pxPerMs >= 0.02f -> 5_000L
            else             -> 10_000L
        }
        val majorEvery = 5

        var tick = 0L
        var count = 0
        while (tick <= totalMs) {
            val xPx = labelWidthPx + (tick * pxPerMs).toInt()
            val isMajor = count % majorEvery == 0

            val tickView = View(context).apply {
                setBackgroundColor(if (isMajor)
                    ContextCompat.getColor(context, R.color.border_mid)
                else
                    ContextCompat.getColor(context, R.color.border_dim)
                )
                layoutParams = FrameLayout.LayoutParams(
                    dpToPx(1),
                    if (isMajor) dpToPx(10) else dpToPx(5)
                ).apply {
                    leftMargin = xPx
                    gravity    = android.view.Gravity.BOTTOM
                }
            }
            ruler.addView(tickView)

            if (isMajor) {
                val sec  = (tick / 1000).toInt()
                val min  = sec / 60
                val secR = sec % 60
                val label = android.widget.TextView(context).apply {
                    text    = "%02d:%02d".format(min, secR)
                    textSize = 9f
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin  = xPx - dpToPx(10)
                        topMargin   = dpToPx(2)
                    }
                }
                ruler.addView(label)
            }

            tick += tickIntervalMs
            count++
        }
    }

    // ── Clip highlight ────────────────────────────────────────

    private fun highlightClip(clipId: String?) {
        val container = binding.timelineTracksContainer
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            val content = row.getChildAt(1) as? FrameLayout ?: continue
            for (j in 0 until content.childCount) {
                val cv = content.getChildAt(j) as? ClipView ?: continue
                cv.setSelected(cv.clipId == clipId)
            }
        }
    }

    // ── Add track menu ────────────────────────────────────────

    private fun showAddTrackMenu() {
        val items = arrayOf("Video", "Audio", "FX")
        android.app.AlertDialog.Builder(context)
            .setTitle("Add Track")
            .setItems(items) { _, which ->
                val type = when (which) {
                    0 -> TrackType.VIDEO
                    1 -> TrackType.AUDIO
                    else -> TrackType.FX
                }
                viewModel.addTrack(type)
            }
            .show()
    }

    // ── Vibrate (magnet snap feedback) ────────────────────────

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vm = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vm?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vm?.vibrate(18)
                }
            }
        } catch (e: Exception) { /* not critical */ }
    }

    // ── Utils ─────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun trackColor(type: TrackType): Int = ContextCompat.getColor(context, when (type) {
        TrackType.VIDEO   -> R.color.track_video
        TrackType.AUDIO   -> R.color.track_audio
        TrackType.TEXT    -> R.color.track_text
        TrackType.FX      -> R.color.track_fx
        TrackType.STICKER -> R.color.track_sticker
        TrackType.PIP     -> R.color.track_video
    })
}
