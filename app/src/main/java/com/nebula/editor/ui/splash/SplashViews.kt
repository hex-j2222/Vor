package com.nebula.editor.ui.splash

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * Animated starfield canvas — drawn in software for max compat (API 21+).
 * Stars twinkle and drift slowly.
 */
class StarfieldView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Star(
        var x: Float, var y: Float,
        val radius: Float,
        val baseAlpha: Float,
        val twinkleSpeed: Float,
        val twinkleOffset: Float,
        val color: Int,
    )

    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frame = 0L
    private val rng = Random(42)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        generateStars(w, h)
        postInvalidateOnAnimation()
    }

    private fun generateStars(w: Int, h: Int) {
        stars.clear()
        val count = (w * h / 3500).coerceIn(60, 300)
        val starColors = intArrayOf(
            Color.WHITE,
            Color.parseColor("#FF6C8FFF"),
            Color.parseColor("#FF22D3EE"),
            Color.parseColor("#FFA855F7"),
        )
        repeat(count) {
            stars.add(Star(
                x            = rng.nextFloat() * w,
                y            = rng.nextFloat() * h,
                radius       = rng.nextFloat() * 1.6f + 0.3f,
                baseAlpha    = rng.nextFloat() * 0.6f + 0.2f,
                twinkleSpeed = rng.nextFloat() * 0.04f + 0.01f,
                twinkleOffset= rng.nextFloat() * Math.PI.toFloat() * 2f,
                color        = if (rng.nextFloat() > 0.15f) Color.WHITE
                               else starColors[rng.nextInt(starColors.size)],
            ))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frame++

        stars.forEach { star ->
            val alpha = (sin(frame * star.twinkleSpeed + star.twinkleOffset) * 0.35f + star.baseAlpha)
                .coerceIn(0f, 1f)
            paint.color = star.color
            paint.alpha = (alpha * 255).toInt()
            canvas.drawCircle(star.x, star.y, star.radius, paint)
        }

        postInvalidateOnAnimation()
    }
}

/**
 * Nebula logo — animated orbiting rings + glowing core.
 */
class NebulaLogoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintCore   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRing1  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRing2  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintGlow   = Paint(Paint.ANTI_ALIAS_FLAG)
    private var angle1 = 0f
    private var angle2 = 0f

    init {
        paintCore.style  = Paint.Style.FILL
        paintRing1.style = Paint.Style.STROKE
        paintRing1.strokeWidth = 2.5f
        paintRing2.style = Paint.Style.STROKE
        paintRing2.strokeWidth = 1.5f
        paintGlow.style  = Paint.Style.FILL
        paintGlow.maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val r  = minOf(cx, cy) * 0.9f

        // Glow
        paintGlow.color = Color.parseColor("#446C8FFF")
        canvas.drawCircle(cx, cy, r * 0.55f, paintGlow)

        // Ring 1 (tilted ellipse rotating)
        canvas.save()
        canvas.rotate(angle1, cx, cy)
        paintRing1.color = Color.parseColor("#996C8FFF")
        canvas.drawOval(RectF(cx - r, cy - r * 0.35f, cx + r, cy + r * 0.35f), paintRing1)
        // Orbit dot
        val ox1 = cx + cos(Math.toRadians(angle1.toDouble())).toFloat() * r
        val oy1 = cy + sin(Math.toRadians(angle1.toDouble())).toFloat() * r * 0.35f
        paintCore.color = Color.parseColor("#FF22D3EE")
        canvas.drawCircle(ox1, oy1, 4f, paintCore)
        canvas.restore()

        // Ring 2 (different tilt)
        canvas.save()
        canvas.rotate(-angle2 * 0.7f, cx, cy)
        canvas.skew(0.2f, 0f)
        paintRing2.color = Color.parseColor("#88A855F7")
        canvas.drawOval(RectF(cx - r * 0.8f, cy - r * 0.28f, cx + r * 0.8f, cy + r * 0.28f), paintRing2)
        canvas.restore()

        // Core gradient
        val shader = RadialGradient(cx, cy, r * 0.3f,
            intArrayOf(
                Color.parseColor("#FF6C8FFF"),
                Color.parseColor("#FFA855F7"),
                Color.parseColor("#00000000"),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paintCore.shader = shader
        canvas.drawCircle(cx, cy, r * 0.35f, paintCore)
        paintCore.shader = null

        // Advance animation
        angle1 = (angle1 + 0.8f) % 360f
        angle2 = (angle2 + 0.5f) % 360f
        postInvalidateOnAnimation()
    }
}
