package com.nebula.editor.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.nebula.editor.viewmodel.EditorViewModel

/**
 * Full-screen crop overlay drawn on top of the preview canvas.
 * Shows a crop rectangle with 8 resize handles + rule-of-thirds grid.
 * Calls viewModel.trimClip() on confirm.
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var clipId: String = ""
    private var viewModel: EditorViewModel? = null
    private var onDone: (() -> Unit)? = null

    // Crop rect in normalized 0..1 coords
    private var cropL = 0.1f
    private var cropT = 0.1f
    private var cropR = 0.9f
    private var cropB = 0.9f

    private val paintDim    = Paint().apply { color = Color.argb(140, 0, 0, 0) }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.WHITE
    }
    private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.7f; color = Color.argb(100, 255, 255, 255)
    }
    private val paintHandle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.WHITE
    }

    private val HANDLE_RADIUS = 14f
    private var activeHandle  = -1
    private var lastX = 0f; private var lastY = 0f

    // Handle index: 0=TL,1=TM,2=TR,3=ML,4=MR,5=BL,6=BM,7=BR
    private fun handlePos(w: Float, h: Float): Array<PointF> {
        val l = cropL * w; val t = cropT * h
        val r = cropR * w; val b = cropB * h
        val mx = (l + r) / 2f; val my = (t + b) / 2f
        return arrayOf(
            PointF(l, t), PointF(mx, t), PointF(r, t),
            PointF(l, my),               PointF(r, my),
            PointF(l, b), PointF(mx, b), PointF(r, b),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val l = cropL * w; val t = cropT * h
        val r = cropR * w; val b = cropB * h

        // Dim outside crop
        canvas.drawRect(0f, 0f, w, t, paintDim)
        canvas.drawRect(0f, t, l, b, paintDim)
        canvas.drawRect(r, t, w, b, paintDim)
        canvas.drawRect(0f, b, w, h, paintDim)

        // Rule-of-thirds grid
        val cw = r - l; val ch = b - t
        canvas.drawLine(l + cw / 3f, t, l + cw / 3f, b, paintGrid)
        canvas.drawLine(l + 2 * cw / 3f, t, l + 2 * cw / 3f, b, paintGrid)
        canvas.drawLine(l, t + ch / 3f, r, t + ch / 3f, paintGrid)
        canvas.drawLine(l, t + 2 * ch / 3f, r, t + 2 * ch / 3f, paintGrid)

        // Border
        canvas.drawRect(l, t, r, b, paintBorder)

        // Handles
        handlePos(w, h).forEach { pt ->
            canvas.drawCircle(pt.x, pt.y, HANDLE_RADIUS, paintHandle)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat(); val h = height.toFloat()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val handles = handlePos(w, h)
                activeHandle = handles.indexOfFirst {
                    hypot(event.x - it.x, event.y - it.y) < HANDLE_RADIUS * 2
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - lastX) / w
                val dy = (event.y - lastY) / h
                when (activeHandle) {
                    0 -> { cropL = (cropL + dx).coerceIn(0f, cropR - 0.05f); cropT = (cropT + dy).coerceIn(0f, cropB - 0.05f) }
                    1 -> cropT = (cropT + dy).coerceIn(0f, cropB - 0.05f)
                    2 -> { cropR = (cropR + dx).coerceIn(cropL + 0.05f, 1f); cropT = (cropT + dy).coerceIn(0f, cropB - 0.05f) }
                    3 -> cropL = (cropL + dx).coerceIn(0f, cropR - 0.05f)
                    4 -> cropR = (cropR + dx).coerceIn(cropL + 0.05f, 1f)
                    5 -> { cropL = (cropL + dx).coerceIn(0f, cropR - 0.05f); cropB = (cropB + dy).coerceIn(cropT + 0.05f, 1f) }
                    6 -> cropB = (cropB + dy).coerceIn(cropT + 0.05f, 1f)
                    7 -> { cropR = (cropR + dx).coerceIn(cropL + 0.05f, 1f); cropB = (cropB + dy).coerceIn(cropT + 0.05f, 1f) }
                    else -> {
                        // Drag whole crop rect
                        val rw = cropR - cropL; val rh = cropB - cropT
                        cropL = (cropL + dx).coerceIn(0f, 1f - rw)
                        cropT = (cropT + dy).coerceIn(0f, 1f - rh)
                        cropR = cropL + rw; cropB = cropT + rh
                    }
                }
                lastX = event.x; lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> activeHandle = -1
        }
        return true
    }

    fun setClipId(id: String, vm: EditorViewModel, done: () -> Unit) {
        clipId    = id
        viewModel = vm
        onDone    = done
        // Load current crop from clip
        vm.getSelectedClip()?.let {
            cropL = it.cropLeft; cropT = it.cropTop
            cropR = it.cropRight; cropB = it.cropBottom
        }
        invalidate()
    }

    fun confirm() {
        // Commit crop to viewModel
        val clip = viewModel?.getSelectedClip() ?: return
        val updated = clip.copy(
            cropLeft = cropL, cropTop = cropT, cropRight = cropR, cropBottom = cropB
        )
        viewModel?.setClipTransform(clipId, updated.transform)
        onDone?.invoke()
    }

    fun cancel() { onDone?.invoke() }
}
