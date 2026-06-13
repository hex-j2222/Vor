package com.nebula.editor.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

// ============================================================
//  NEBULA EDITOR — CORE DATA MODELS
// ============================================================

/** Types of timeline tracks */
enum class TrackType { VIDEO, AUDIO, TEXT, FX, STICKER, PIP }

/** Supported export formats */
enum class ExportFormat { MP4, MKV, GIF }

/** Aspect ratio presets */
enum class AspectRatio(val w: Int, val h: Int, val label: String) {
    REELS_9_16(9, 16, "Reels 9:16"),
    YOUTUBE_16_9(16, 9, "YouTube 16:9"),
    SQUARE_1_1(1, 1, "Square 1:1"),
    TIKTOK_9_16(9, 16, "TikTok 9:16"),
    TWITTER_16_9(16, 9, "Twitter 16:9"),
    CINEMA_21_9(21, 9, "Cinema 21:9"),
    PORTRAIT_4_5(4, 5, "Portrait 4:5"),
    STANDARD_4_3(4, 3, "Standard 4:3"),
    WIDESCREEN_2_1(2, 1, "Widescreen 2:1"),
    VERTICAL_2_3(2, 3, "Vertical 2:3"),
    FILM_1_85(185, 100, "Film 1.85:1"),
    FILM_2_39(239, 100, "Anamorphic 2.39:1"),
    FACEBOOK_16_9(16, 9, "Facebook 16:9"),
    INSTAGRAM_4_5(4, 5, "Instagram 4:5"),
    SNAPCHAT_9_16(9, 16, "Snapchat 9:16"),
    LINKEDIN_16_9(16, 9, "LinkedIn 16:9"),
    PINTEREST_2_3(2, 3, "Pinterest 2:3"),
    YOUTUBE_SHORT_9_16(9, 16, "YT Shorts 9:16"),
    IGTV_9_16(9, 16, "IGTV 9:16"),
    CUSTOM(0, 0, "Custom"),
}

/** A single keyframe on an animated property */
@Parcelize
data class Keyframe(
    val timeMs: Long,
    val value: Float,
    /** Bezier control — 0=linear, 1=ease, etc. */
    val easeIn: Float = 0f,
    val easeOut: Float = 0f
) : Parcelable

/** Animated property that holds keyframes */
@Parcelize
data class AnimatedProperty(
    val keyframes: List<Keyframe> = emptyList(),
    val defaultValue: Float = 0f
) : Parcelable {
    /** Linear interpolation between keyframes at given time */
    fun valueAt(timeMs: Long): Float {
        if (keyframes.isEmpty()) return defaultValue
        val sorted = keyframes.sortedBy { it.timeMs }
        val before = sorted.lastOrNull { it.timeMs <= timeMs } ?: return sorted.first().value
        val after  = sorted.firstOrNull { it.timeMs > timeMs }  ?: return before.value
        val t = (timeMs - before.timeMs).toFloat() / (after.timeMs - before.timeMs)
        // Cubic bezier interpolation
        val easedT = cubicBezier(t, before.easeOut, after.easeIn)
        return before.value + (after.value - before.value) * easedT
    }

    private fun cubicBezier(t: Float, p1: Float, p2: Float): Float {
        // Simplified 1D cubic bezier
        return t * t * t + 3 * t * t * (1 - t) * p2 + 3 * t * (1 - t) * (1 - t) * p1
    }
}

/** Transform state of a clip/layer on canvas */
@Parcelize
data class LayerTransform(
    val x: AnimatedProperty = AnimatedProperty(defaultValue = 0f),
    val y: AnimatedProperty = AnimatedProperty(defaultValue = 0f),
    val scaleX: AnimatedProperty = AnimatedProperty(defaultValue = 1f),
    val scaleY: AnimatedProperty = AnimatedProperty(defaultValue = 1f),
    val rotation: AnimatedProperty = AnimatedProperty(defaultValue = 0f),
    val alpha: AnimatedProperty = AnimatedProperty(defaultValue = 1f),
) : Parcelable

/** Speed ramp point — allows precise ramping like DJ tool */
@Parcelize
data class SpeedPoint(
    val timeMs: Long,
    /** 0.01 = 1% speed, 8.0 = 800% speed */
    val speed: Float
) : Parcelable

/** Audio fade envelope point */
@Parcelize
data class FadePoint(
    val timeMs: Long,
    val volume: Float      // 0.0 – 5.0 (0–500%)
) : Parcelable

/** A single clip on the timeline */
@Parcelize
data class TimelineClip(
    val id: String,
    val trackId: String,
    val type: TrackType,

    /** Source file path */
    val sourcePath: String,

    /** Position on timeline */
    val startTimeMs: Long,
    val endTimeMs: Long,

    /** Trim within the source file */
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = -1L,   // -1 = use full duration

    /** Transform (position, scale, rotation, opacity) with keyframe support */
    val transform: LayerTransform = LayerTransform(),

    /** Audio */
    val volume: Float = 1f,      // 0.0 – 5.0
    val isMuted: Boolean = false,
    val fadeEnvelope: List<FadePoint> = emptyList(),

    /** Speed */
    val speedPoints: List<SpeedPoint> = listOf(SpeedPoint(0L, 1f)),
    val maintainAudioPitch: Boolean = true,  // keep pitch when slowing/speeding audio

    /** Flip / mirror */
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,

    /** Crop rect — normalized 0..1 */
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,

    /** For reversed clips */
    val isReversed: Boolean = false,
) : Parcelable {
    val durationMs: Long get() = endTimeMs - startTimeMs
}

/** A track (row) on the timeline */
@Parcelize
data class TimelineTrack(
    val id: String,
    val type: TrackType,
    val label: String,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val clips: List<TimelineClip> = emptyList(),
    /** Z-order — higher = on top */
    val zIndex: Int = 0,
) : Parcelable

/** Background of the canvas */
@Parcelize
data class CanvasBackground(
    val type: BackgroundType = BackgroundType.COLOR,
    val colorHex: String = "#000000",
    /** For gradient */
    val colorHex2: String? = null,
    val gradientAngle: Float = 135f,
    /** For image/video background */
    val mediaPath: String? = null,
    /** Blur amount (0 = no blur) */
    val blurRadius: Float = 0f,
) : Parcelable

enum class BackgroundType { COLOR, GRADIENT, IMAGE, VIDEO, GLOW }

/** Complete project state */
@Parcelize
data class Project(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    /** Canvas size */
    val aspectRatio: AspectRatio = AspectRatio.REELS_9_16,
    val customWidth: Int = 1080,
    val customHeight: Int = 1920,

    val tracks: List<TimelineTrack> = emptyList(),
    val background: CanvasBackground = CanvasBackground(),

    /** Total timeline duration in ms */
    val durationMs: Long = 0L,

    /** Last playhead position (for restore) */
    val lastPlayheadMs: Long = 0L,

    /** Last timeline zoom */
    val timelineZoom: Float = 1f,
) : Parcelable

/** Export configuration */
@Parcelize
data class ExportConfig(
    val format: ExportFormat = ExportFormat.MP4,
    /** 1–100 quality, 100 = original (lossless) */
    val quality: Int = 80,
    val fps: Int = 30,
    val useOriginalQuality: Boolean = false,
    val outputPath: String = "",
    /** Resume token for interrupted exports */
    val resumeToken: String? = null,
) : Parcelable

/** Undo/Redo history item */
data class HistoryEntry(
    val description: String,
    val snapshot: Project,
    val timestamp: Long = System.currentTimeMillis()
)
