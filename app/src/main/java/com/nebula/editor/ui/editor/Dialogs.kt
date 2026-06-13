package com.nebula.editor.ui.editor

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nebula.editor.R
import com.nebula.editor.databinding.ActivityEditorBinding
import com.nebula.editor.model.*
import com.nebula.editor.viewmodel.EditorViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// ============================================================
//  PropertiesController — right-side properties panel
// ============================================================

class PropertiesController(
    private val activity: EditorActivity,
    private val binding: ActivityEditorBinding,
    private val viewModel: EditorViewModel,
) {
    init { observe() }

    private fun observe() {
        val owner = activity as LifecycleOwner
        owner.lifecycleScope.launch {
            viewModel.selectedClipId.collect { clipId ->
                val clip = viewModel.project.value?.tracks
                    ?.flatMap { it.clips }
                    ?.firstOrNull { it.id == clipId }
                if (clip != null) showClipProperties(clip)
                else binding.propertiesPanel.visibility = View.GONE
            }
        }
    }

    private fun showClipProperties(clip: TimelineClip) {
        binding.propertiesPanel.visibility = View.VISIBLE

        // Volume
        binding.sliderVolume.value    = clip.volume * 100f
        binding.sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setClipVolume(clip.id, value / 100f)
        }

        // Opacity (alpha)
        val alpha = clip.transform.alpha.valueAt(viewModel.playheadMs.value)
        binding.sliderOpacity.value = alpha * 100f
        binding.sliderOpacity.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val newTransform = clip.transform.copy(
                alpha = AnimatedProperty(keyframes = listOf(Keyframe(0L, value / 100f)))
            )
            viewModel.setClipTransform(clip.id, newTransform)
        }

        // Scale
        val scale = clip.transform.scaleX.valueAt(0L)
        binding.sliderScale.value = (scale * 100f).coerceIn(10f, 300f)
        binding.sliderScale.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val s = value / 100f
            val newTransform = clip.transform.copy(
                scaleX = AnimatedProperty(keyframes = listOf(Keyframe(0L, s))),
                scaleY = AnimatedProperty(keyframes = listOf(Keyframe(0L, s))),
            )
            viewModel.setClipTransform(clip.id, newTransform)
        }
    }

    fun showKeyframePanel() {
        // TODO: open keyframe curve editor
    }
}

// ============================================================
//  AddMediaDialog
// ============================================================

class AddMediaDialog : BottomSheetDialogFragment() {

    enum class Action { BLACK_SCREEN, IMPORT, IMPORT_IMAGE }

    private var callback: ((Action) -> Unit)? = null

    companion object {
        fun show(fm: FragmentManager, callback: (Action) -> Unit) {
            AddMediaDialog().also { it.callback = callback }.show(fm, "AddMedia")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 64)
        }
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.add_media)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(space(16))

        val options = listOf(
            getString(R.string.black_screen)  to Action.BLACK_SCREEN,
            getString(R.string.import_video)  to Action.IMPORT,
            getString(R.string.import_image)  to Action.IMPORT_IMAGE,
        )
        options.forEach { (label, action) ->
            root.addView(Button(requireContext()).apply {
                text = label
                setOnClickListener { callback?.invoke(action); dismiss() }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            root.addView(space(8))
        }
        return root
    }

    private fun space(dp: Int) = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (dp * resources.displayMetrics.density).toInt()
        )
    }
}

// ============================================================
//  BackgroundDialog
// ============================================================

class BackgroundDialog : BottomSheetDialogFragment() {

    private lateinit var viewModel: EditorViewModel

    companion object {
        fun show(fm: FragmentManager, vm: EditorViewModel) {
            BackgroundDialog().also { it.viewModel = vm }.show(fm, "Background")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 64)
        }

        root.addView(TextView(requireContext()).apply {
            text     = getString(R.string.background)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(space(16))

        // Type tabs: Color | Gradient | Image | Video | Glow
        val typeNames = listOf("Color", "Gradient", "Image", "Video", "Glow")
        val typeRow   = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        typeNames.forEach { t ->
            typeRow.addView(Button(requireContext()).apply {
                text = t
                setOnClickListener { showBgOptions(root, BackgroundType.valueOf(t.uppercase())) }
            })
        }
        root.addView(HorizontalScrollView(requireContext()).apply { addView(typeRow) })
        root.addView(space(16))

        // Default: solid color row
        showBgOptions(root, BackgroundType.COLOR)

        return root
    }

    private fun showBgOptions(root: LinearLayout, type: BackgroundType) {
        // Remove previous option views (indices > 2)
        while (root.childCount > 3) root.removeViewAt(root.childCount - 1)

        when (type) {
            BackgroundType.COLOR, BackgroundType.GLOW -> {
                val colors = listOf("#000000","#1A0533","#050A1F","#0D1225","#6C8FFF","#A855F7","#22D3EE","#F472B6","#FFFFFF")
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
                colors.forEach { hex ->
                    row.addView(View(requireContext()).apply {
                        setBackgroundColor(android.graphics.Color.parseColor(hex))
                        layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply { marginEnd = dpToPx(8) }
                        setOnClickListener {
                            viewModel.setBackground(CanvasBackground(type = type, colorHex = hex))
                            dismiss()
                        }
                    })
                }
                root.addView(HorizontalScrollView(requireContext()).apply { addView(row) })
            }
            BackgroundType.GRADIENT -> {
                // Show angle slider + two color pickers
                root.addView(TextView(requireContext()).apply { text = "Gradient — coming soon" })
            }
            BackgroundType.IMAGE, BackgroundType.VIDEO -> {
                root.addView(Button(requireContext()).apply {
                    text = "Choose from Gallery"
                    setOnClickListener { /* launch picker */ dismiss() }
                })
            }
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun space(dp: Int) = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (dp * resources.displayMetrics.density).toInt()
        )
    }
}

// ============================================================
//  StickersBottomSheet
// ============================================================

class StickersBottomSheet : BottomSheetDialogFragment() {

    private var callback: ((String) -> Unit)? = null

    companion object {
        fun show(fm: FragmentManager, callback: (String) -> Unit) {
            StickersBottomSheet().also { it.callback = callback }.show(fm, "Stickers")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 64)
        }
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.stickers)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        // Search
        val search = EditText(requireContext()).apply { hint = getString(R.string.search) }
        root.addView(search)

        // Grid — categories loaded from assets/stickers/
        // In production: RecyclerView with GridLayoutManager
        root.addView(TextView(requireContext()).apply { text = "Sticker library loads from assets/stickers/" })

        return root
    }
}
