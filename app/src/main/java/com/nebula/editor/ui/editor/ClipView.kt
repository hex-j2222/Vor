package com.nebula.editor.ui.editor

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.ContextCompat
import com.nebula.editor.R
import com.nebula.editor.model.TimelineClip
import com.nebula.editor.model.TrackType
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom View that renders a single timeline clip:
 * - Colored gradient background per track type
 * - Waveform bars for audio clips
 * - Film-strip grid for video clips
 * - Filename label
 * - Left/right trim handle zones
 * - Selected / dragging visual states
 */
class ClipView(
    context: Context,
    val clip: TimelineClip,
    private val trackType: TrackType,
) : View(context) {

    val clipId: String = clip.id

    private val paintBg        = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBorder    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLabel     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintHandle    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintWave      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintFilmStrip = Paint(Paint.ANTI_ALIAS_FLAG)

    private var isDragging = false
    private var isSelectedClip = false

    // Pre-generated waveform heights (random seed from clip id for consistency)
    private val waveHeights: FloatArray by lazy {
        val rng = Random(clip.id.hashCode().toLong())
        FloatArray(120) { rng.nextFloat() * 0.8f + 0.1f }
    }

    init {
        paintLabel.apply {
            color     = Color.WHITE
            textSize  = dp(10f)
            typeface  = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        paintHandle.apply {
            color = Color.argb(180, 255, 255, 255)
        }
        paintWave.apply {
            color = Color.argb(140, 255, 255, 255)
        }
        paintFilmStrip.apply {
            color = Color.argb(50, 255, 255, 255)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = dp(4f)

        // ── Background ────────────────────────────────────────
        val (col1, col2) = trackColors(trackType)
        paintBg.shader = LinearGradient(0f, 0f, w, 0f, col1, col2, Shader.TileMode.CLAMP)
        val path = Path().apply { addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW) }
        canvas.drawPath(path, paintBg)

        // ── Film strip (video) ────────────────────────────────
        if (trackType == TrackType.VIDEO || trackType == TrackType.PIP) {
            drawFilmStrip(canvas, w, h)
        }

        // ── Waveform (audio) ─────────────────────────────────
        if (trackType == TrackType.AUDIO) {
            drawWaveform(canvas, w, h)
        }

        // ── Border ────────────────────────────────────────────
        paintBorder.apply {
            style       = Paint.Style.STROKE
            strokeWidth = if (isSelectedClip) dp(1.5f) else dp(0.75f)
            color       = if (isSelectedClip)
                Color.argb(230, 255, 255, 255)
            else
                Color.argb(80, 255, 255, 255)
            shader = null
        }
        canvas.drawPath(path, paintBorder)

        // ── Selection glow ────────────────────────────────────
        if (isSelectedClip) {
            val glowPaint = Paint(paintBorder).apply {
                strokeWidth = dp(3f)
                color       = Color.argb(60, 255, 255, 255)
                maskFilter  = BlurMaskFilter(dp(4f), BlurMaskFilter.Blur.OUTER)
            }
            canvas.drawPath(path, glowPaint)
        }

        // ── Trim handles ──────────────────────────────────────
        val handleW = dp(6f)
        // Left handle
        canvas.drawRoundRect(RectF(0f, h * 0.2f, handleW, h * 0.8f), dp(2f), dp(2f), paintHandle)
        // Right handle
        canvas.drawRoundRect(RectF(w - handleW, h * 0.2f, w, h * 0.8f), dp(2f), dp(2f), paintHandle)

        // ── Label ─────────────────────────────────────────────
        val label = clip.sourcePath.substringAfterLast('/').take(20)
        canvas.save()
        canvas.clipRect(dp(8f), 0f, w - dp(8f), h)
        canvas.drawText(label, dp(8f), h / 2f + paintLabel.textSize / 3f, paintLabel)
        canvas.restore()

        // ── Dragging dim ─────────────────────────────────────
        if (isDragging) {
            val dimPaint = Paint().apply { color = Color.argb(40, 0, 0, 0) }
            canvas.drawPath(path, dimPaint)
        }
    }

    private fun drawWaveform(canvas: Canvas, w: Float, h: Float) {
        val barCount = (w / dp(3f)).toInt().coerceAtMost(waveHeights.size)
        val barW     = dp(2f)
        val gap      = (w - barW * barCount) / (barCount + 1)
        val midY     = h / 2f

        for (i in 0 until barCount) {
            val barH  = waveHeights[i % waveHeights.size] * (h * 0.7f)
            val x     = gap + i * (barW + gap)
            canvas.drawRoundRect(
                RectF(x, midY - barH / 2f, x + barW, midY + barH / 2f),
                dp(1f), dp(1f), paintWave
            )
        }
    }

    private fun drawFilmStrip(canvas: Canvas, w: Float, h: Float) {
        val frameW = dp(2f)
        val gap    = dp(14f)
        var x = gap
        while (x < w - frameW) {
            canvas.drawRect(x, dp(2f), x + frameW, h - dp(2f), paintFilmStrip)
            x += gap
        }
    }

    private fun trackColors(type: TrackType): Pair<Int, Int> = when (type) {
        TrackType.VIDEO   -> Color.parseColor("#2549C0") to Color.parseColor("#3B6EEF")
        TrackType.AUDIO   -> Color.parseColor("#064E3B") to Color.parseColor("#059669")
        TrackType.TEXT    -> Color.parseColor("#92400E") to Color.parseColor("#D97706")
        TrackType.FX      -> Color.parseColor("#831843") to Color.parseColor("#DB2777")
        TrackType.STICKER -> Color.parseColor("#4C1D95") to Color.parseColor("#7C3AED")
        TrackType.PIP     -> Color.parseColor("#1E3A5F") to Color.parseColor("#2563EB")
    }

    // ── Public setters ────────────────────────────────────────

    fun setDragging(dragging: Boolean) {
        isDragging = dragging
        alpha = if (dragging) 0.75f else 1f
        invalidate()
    }

    fun setSelected(selected: Boolean) {
        isSelectedClip = selected
        invalidate()
    }

    // ── Utils ─────────────────────────────────────────────────

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
