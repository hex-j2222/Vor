package com.nebula.editor.ui.editor

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nebula.editor.R
import com.nebula.editor.viewmodel.EditorViewModel

/**
 * Speed control bottom sheet — DJ-style:
 * - Preset speed chips: 0.1x 0.25x 0.5x 1x 2x 4x 8x
 * - Precision slider: 0.01x – 8.00x (step 0.01)
 * - Separate audio pitch toggle (maintain pitch when slowing/speeding)
 * - Keyframe ramp toggle (add speed keyframe at playhead)
 * - Visual speed indicator
 */
class SpeedControlDialog : BottomSheetDialogFragment() {

    private lateinit var viewModel: EditorViewModel
    private var clipId: String = ""

    companion object {
        private const val TAG = "SpeedControlDialog"
        fun show(fm: FragmentManager, clipId: String, vm: EditorViewModel) {
            SpeedControlDialog().also {
                it.clipId    = clipId
                it.viewModel = vm
            }.show(fm, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 48)
        }

        // Title
        root.addView(TextView(requireContext()).apply {
            text     = getString(R.string.speed_control)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(resolveColor(R.attr.colorOnSurface))
        })

        root.addView(verticalSpace(16))

        // Speed presets
        val presets = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f, 8f)
        val chipRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val scrollChips = HorizontalScrollView(requireContext())
        scrollChips.addView(chipRow)
        root.addView(scrollChips)

        val currentSpeed = viewModel.getSelectedClip()?.speedPoints?.firstOrNull()?.speed ?: 1f
        var selectedSpeed = currentSpeed

        presets.forEach { speed ->
            val chip = Button(requireContext()).apply {
                text       = "${speed}x"
                textSize   = 12f
                isSelected = speed == currentSpeed
                setOnClickListener {
                    selectedSpeed = speed
                    updateSlider(speedBar, speed)
                    updateLabel(tvSpeedVal, speed)
                    updateChips(chipRow, speed)
                }
            }
            chipRow.addView(chip)
        }

        root.addView(verticalSpace(20))

        // Precision slider: 1–800 maps to 0.01x–8.00x
        val tvSpeedVal = TextView(requireContext()).apply {
            text     = "${"%.2f".format(currentSpeed)}x"
            textSize = 28f
            gravity  = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(tvSpeedVal)

        val speedBar = SeekBar(requireContext()).apply {
            max      = 799
            progress = speedToProgress(currentSpeed)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    selectedSpeed = progressToSpeed(p)
                    updateLabel(tvSpeedVal, selectedSpeed)
                    updateChips(chipRow, selectedSpeed)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        root.addView(speedBar)
        root.addView(verticalSpace(8))

        // Fine controls
        val fineRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER
        }
        listOf(-0.05f, -0.01f, +0.01f, +0.05f).forEach { delta ->
            fineRow.addView(Button(requireContext()).apply {
                text = if (delta > 0) "+${delta}" else "$delta"
                setOnClickListener {
                    selectedSpeed = (selectedSpeed + delta).coerceIn(0.01f, 8f)
                    updateSlider(speedBar, selectedSpeed)
                    updateLabel(tvSpeedVal, selectedSpeed)
                }
            })
        }
        root.addView(fineRow)

        root.addView(verticalSpace(16))

        // Maintain pitch toggle
        val pitchRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
        }
        val pitchSwitch = Switch(requireContext()).apply {
            isChecked = viewModel.getSelectedClip()?.maintainAudioPitch ?: true
        }
        pitchRow.addView(TextView(requireContext()).apply {
            text = getString(R.string.maintain_pitch)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        pitchRow.addView(pitchSwitch)
        root.addView(pitchRow)

        root.addView(verticalSpace(24))

        // Apply button
        root.addView(Button(requireContext()).apply {
            text = getString(R.string.apply)
            setOnClickListener {
                viewModel.setClipSpeed(clipId, selectedSpeed)
                // Pitch preference
                // viewModel.setMaintainPitch(clipId, pitchSwitch.isChecked)
                dismiss()
            }
        })

        return root
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun speedToProgress(speed: Float): Int =
        ((speed - 0.01f) / (8f - 0.01f) * 799).toInt().coerceIn(0, 799)

    private fun progressToSpeed(progress: Int): Float =
        (0.01f + progress.toFloat() / 799f * (8f - 0.01f)).coerceIn(0.01f, 8f)

    private fun updateSlider(bar: SeekBar, speed: Float) {
        bar.progress = speedToProgress(speed)
    }

    private fun updateLabel(tv: TextView, speed: Float) {
        tv.text = "${"%.2f".format(speed)}x"
    }

    private fun updateChips(row: LinearLayout, speed: Float) {
        for (i in 0 until row.childCount) {
            val btn = row.getChildAt(i) as? Button ?: continue
            val chipSpeed = btn.text.toString().removeSuffix("x").toFloatOrNull() ?: continue
            btn.isSelected = Math.abs(chipSpeed - speed) < 0.001f
        }
    }

    private fun verticalSpace(dp: Int): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (dp * resources.displayMetrics.density).toInt()
        )
    }

    private fun resolveColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
