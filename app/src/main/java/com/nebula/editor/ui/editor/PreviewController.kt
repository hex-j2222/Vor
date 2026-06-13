package com.nebula.editor.ui.editor

import android.content.ContentValues
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nebula.editor.databinding.ActivityEditorBinding
import com.nebula.editor.model.TimelineClip
import com.nebula.editor.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Controls the preview canvas:
 * - Pinch-to-zoom + pan on the preview frame
 * - Drag layers (text, stickers, images) with snap guide lines
 * - Multi-touch layer selection
 * - Capture current frame as PNG at full resolution
 */
class PreviewController(
    private val activity: EditorActivity,
    private val binding: ActivityEditorBinding,
    private val viewModel: EditorViewModel,
) {
    private val context = activity

    // Canvas transform state
    private var canvasScale       = 1f
    private var canvasTranslateX  = 0f
    private var canvasTranslateY  = 0f
    private var lastFocusX        = 0f
    private var lastFocusY        = 0f

    // Layer drag state
    private var draggingLayerId: String? = null
    private var layerDragStartX  = 0f
    private var layerDragStartY  = 0f
    private var layerOrigX       = 0f
    private var layerOrigY       = 0f

    // Snap threshold in dp
    private val SNAP_DP = 8f

    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    init {
        scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    canvasScale = (canvasScale * detector.scaleFactor).coerceIn(0.2f, 6f)
                    lastFocusX = detector.focusX
                    lastFocusY = detector.focusY
                    applyCanvasTransform()
                    updateZoomLabel()
                    return true
                }
            })

        gestureDetector = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent?, e2: MotionEvent,
                    distanceX: Float, distanceY: Float
                ): Boolean {
                    canvasTranslateX -= distanceX
                    canvasTranslateY -= distanceY
                    applyCanvasTransform()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Reset zoom on double tap
                    canvasScale      = 1f
                    canvasTranslateX = 0f
                    canvasTranslateY = 0f
                    applyCanvasTransform()
                    updateZoomLabel()
                    return true
                }
            })

        setupPreviewTouch()
        setupLayerDrag()
        setupFullscreenButton()
        observe()
    }

    // ── Preview touch ─────────────────────────────────────────

    private fun setupPreviewTouch() {
        binding.previewContainer.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    private fun applyCanvasTransform() {
        binding.previewCanvas.apply {
            scaleX      = canvasScale
            scaleY      = canvasScale
            translationX = canvasTranslateX
            translationY = canvasTranslateY
        }
    }

    private fun updateZoomLabel() {
        binding.tvZoomPercent.text = "${(canvasScale * 100).toInt()}%"
    }

    // ── Layer drag on canvas ──────────────────────────────────

    private fun setupLayerDrag() {
        binding.canvasLayersContainer.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val hit = findLayerAt(event.x, event.y)
                    if (hit != null) {
                        draggingLayerId = hit.id
                        layerDragStartX = event.rawX
                        layerDragStartY = event.rawY
                        layerOrigX      = hit.translationX
                        layerOrigY      = hit.translationY
                        viewModel.selectClip(hit.tag as? String)
                    }
                    hit != null
                }
                MotionEvent.ACTION_MOVE -> {
                    val layerView = draggingLayerId?.let { id ->
                        binding.canvasLayersContainer.findViewWithTag<View>(id)
                    } ?: return@setOnTouchListener false

                    val dx  = event.rawX - layerDragStartX
                    val dy  = event.rawY - layerDragStartY
                    var newX = layerOrigX + dx / canvasScale
                    var newY = layerOrigY + dy / canvasScale

                    // Snap to center
                    val midX = binding.previewCanvas.width / 2f
                    val midY = binding.previewCanvas.height / 2f
                    val snapPx = dpToPx(SNAP_DP)

                    val snappedX = if (Math.abs(newX) < snapPx) { showGuideV(true); 0f } else { showGuideV(false); newX }
                    val snappedY = if (Math.abs(newY) < snapPx) { showGuideH(true); 0f } else { showGuideH(false); newY }

                    layerView.translationX = snappedX
                    layerView.translationY = snappedY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    showGuideH(false)
                    showGuideV(false)
                    // Commit transform to viewmodel
                    draggingLayerId?.let { id ->
                        val layerView = binding.canvasLayersContainer.findViewWithTag<View>(id)
                        if (layerView != null) {
                            commitLayerTransform(id, layerView.translationX, layerView.translationY)
                        }
                    }
                    draggingLayerId = null
                    true
                }
                else -> false
            }
        }
    }

    private fun findLayerAt(x: Float, y: Float): View? {
        val container = binding.canvasLayersContainer
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            val rect  = Rect()
            child.getHitRect(rect)
            if (rect.contains(x.toInt(), y.toInt())) return child
        }
        return null
    }

    private fun showGuideH(show: Boolean) {
        binding.snapGuideH.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showGuideV(show: Boolean) {
        binding.snapGuideV.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun commitLayerTransform(clipId: String, tx: Float, ty: Float) {
        val clip = viewModel.project.value?.tracks
            ?.flatMap { it.clips }
            ?.firstOrNull { it.id == clipId } ?: return

        val canvasW = binding.previewCanvas.width.toFloat()
        val canvasH = binding.previewCanvas.height.toFloat()

        // Normalized position
        val normX = (tx / canvasW) * 2f
        val normY = (ty / canvasH) * 2f

        val newTransform = clip.transform.copy(
            x = clip.transform.x.copy(keyframes = listOf(com.nebula.editor.model.Keyframe(0L, normX))),
            y = clip.transform.y.copy(keyframes = listOf(com.nebula.editor.model.Keyframe(0L, normY))),
        )
        viewModel.setClipTransform(clipId, newTransform)
    }

    // ── Fullscreen ────────────────────────────────────────────

    private fun setupFullscreenButton() {
        binding.btnFullscreen.setOnClickListener {
            val isFs = binding.fullscreenOverlay.visibility == View.VISIBLE
            if (isFs) {
                binding.fullscreenOverlay.visibility = View.GONE
            } else {
                binding.fullscreenOverlay.visibility = View.VISIBLE
            }
        }
    }

    // ── Frame capture ─────────────────────────────────────────

    fun captureFrameAsPng() {
        try {
            val canvas   = binding.previewCanvas
            val bmp      = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            val cvs      = Canvas(bmp)

            // Draw at 1:1 scale ignoring current zoom
            canvas.draw(cvs)

            saveBitmapToPng(bmp)
        } catch (e: Exception) {
            Timber.e(e, "Frame capture failed")
        }
    }

    private fun saveBitmapToPng(bmp: Bitmap) {
        val filename = "nebula_frame_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Nebula")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Nebula")
            dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        Timber.d("Frame saved: $filename")
    }

    // ── Observe ───────────────────────────────────────────────

    private fun observe() {
        val owner = activity as LifecycleOwner
        owner.lifecycleScope.launch {
            viewModel.project.collect { project ->
                // In a real implementation, ExoPlayer renders the video frame here.
                // The canvas layers (text, sticker, PiP) are laid out as child Views.
            }
        }
    }

    // ── Utils ─────────────────────────────────────────────────

    private fun dpToPx(dp: Float): Float =
        dp * context.resources.displayMetrics.density
}
