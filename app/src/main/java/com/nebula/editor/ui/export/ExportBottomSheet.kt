package com.nebula.editor.ui.export

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nebula.editor.R
import com.nebula.editor.model.ExportConfig
import com.nebula.editor.model.ExportFormat
import com.nebula.editor.service.ExportService
import com.nebula.editor.viewmodel.EditorViewModel

class ExportBottomSheet : BottomSheetDialogFragment() {

    private lateinit var viewModel: EditorViewModel

    companion object {
        fun show(fm: FragmentManager, vm: EditorViewModel) {
            ExportBottomSheet().also { it.viewModel = vm }.show(fm, "ExportSheet")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 64)
        }

        // Title
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.export_video)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(space(16))

        // Format
        root.addView(label(getString(R.string.format)))
        root.addView(space(8))
        var selectedFormat = ExportFormat.MP4
        val formatRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val formatBtns = ExportFormat.entries.map { fmt ->
            Button(requireContext()).apply {
                text       = fmt.name
                isSelected = fmt == ExportFormat.MP4
                setOnClickListener {
                    selectedFormat = fmt
                    formatBtns.forEach { b -> b.isSelected = b.text == fmt.name }
                    updateEstimate(tvEstimate, selectedFormat, qBar.progress, fpsBar.progress)
                }
            }.also { formatRow.addView(it) }
        }
        root.addView(formatRow)
        root.addView(space(20))

        // Quality
        root.addView(label(getString(R.string.quality)))
        root.addView(space(8))
        val tvQualityVal = TextView(requireContext()).apply { text = "80%" }
        val qBar = SeekBar(requireContext()).apply {
            max      = 100
            progress = 80
            setOnSeekBarChangeListener(simpleChange { p ->
                tvQualityVal.text = "$p%"
                updateEstimate(tvEstimate, selectedFormat, p, fpsBar.progress)
            })
        }
        root.addView(qBar)
        root.addView(tvQualityVal)
        root.addView(space(20))

        // FPS
        root.addView(label(getString(R.string.frame_rate)))
        root.addView(space(8))
        val fpsList = listOf(24, 30, 60, 120, 240)
        val fpsRow  = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        var selectedFps = 30
        val fpsBtns = fpsList.map { fps ->
            Button(requireContext()).apply {
                text       = "$fps"
                isSelected = fps == 30
                setOnClickListener {
                    selectedFps = fps
                    fpsBtns.forEach { b -> b.isSelected = b.text.toString().toInt() == fps }
                    updateEstimate(tvEstimate, selectedFormat, qBar.progress, fps)
                }
            }.also { fpsRow.addView(it) }
        }
        root.addView(fpsRow)
        root.addView(space(20))

        // FPS fine slider (for 1–240)
        val fpsBar = SeekBar(requireContext()).apply {
            max      = 239
            progress = 29
            setOnSeekBarChangeListener(simpleChange { p ->
                selectedFps = p + 1
                fpsBtns.forEach { b -> b.isSelected = b.text.toString().toInt() == selectedFps }
            })
        }
        root.addView(fpsBar)
        root.addView(space(20))

        // Size estimate
        val tvEstimate = TextView(requireContext()).apply {
            text      = "Estimated size: ~128 MB"
            textSize  = 12f
            gravity   = android.view.Gravity.CENTER
        }
        root.addView(tvEstimate)
        root.addView(space(16))

        // Original quality button
        root.addView(buildOriginalQualityButton {
            launchExport(ExportConfig(
                format               = selectedFormat,
                quality              = 100,
                fps                  = selectedFps,
                useOriginalQuality   = true,
            ))
        })
        root.addView(space(12))

        // Start export
        root.addView(Button(requireContext()).apply {
            text = getString(R.string.start_export)
            setOnClickListener {
                launchExport(ExportConfig(
                    format             = selectedFormat,
                    quality            = qBar.progress,
                    fps                = selectedFps,
                    useOriginalQuality = false,
                ))
            }
        })

        return root
    }

    private fun launchExport(config: ExportConfig) {
        val project = viewModel.project.value ?: return
        viewModel.startExport(config)
        val jobId = java.util.UUID.randomUUID().toString()
        ExportService.start(requireContext(), jobId, project, config)
        dismiss()
    }

    private fun buildOriginalQualityButton(onClick: () -> Unit): View {
        return Button(requireContext()).apply {
            text = "\u2605  ${getString(R.string.original_quality_export)}\n${getString(R.string.original_quality_sub)}"
            setOnClickListener { onClick() }
        }
    }

    private fun updateEstimate(tv: TextView, format: ExportFormat, quality: Int, fps: Int) {
        val project     = viewModel.project.value ?: return
        val durationSec = project.durationMs / 1000.0
        // Rough estimate: bitrate (kbps) * duration / 8 / 1024 = MB
        val bitrateKbps = when {
            format == ExportFormat.GIF -> 500.0
            quality >= 90 -> 8_000.0
            quality >= 70 -> 4_000.0
            quality >= 50 -> 2_000.0
            else -> 1_000.0
        } * (fps / 30.0)
        val sizeMb = (bitrateKbps * durationSec / 8 / 1024).toInt().coerceAtLeast(1)
        tv.text = "Estimated size: ~$sizeMb MB · Duration: ${formatMs(project.durationMs)}"
    }

    private fun formatMs(ms: Long): String {
        val s  = ms / 1000
        val m  = s / 60
        val ss = s % 60
        return "%02d:%02d".format(m, ss)
    }

    private fun label(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize  = 11f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setAllCaps(true)
        setTextColor(android.graphics.Color.GRAY)
    }

    private fun space(dp: Int) = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (dp * resources.displayMetrics.density).toInt()
        )
    }

    private fun simpleChange(block: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) block(p) }
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }
}
