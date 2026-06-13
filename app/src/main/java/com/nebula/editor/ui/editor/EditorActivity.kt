package com.nebula.editor.ui.editor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.nebula.editor.R
import com.nebula.editor.databinding.ActivityEditorBinding
import com.nebula.editor.model.*
import com.nebula.editor.service.AudioRecordService
import com.nebula.editor.service.ExportService
import com.nebula.editor.ui.export.ExportBottomSheet
import com.nebula.editor.viewmodel.EditorViewModel
import com.nebula.editor.viewmodel.ExportState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

@AndroidEntryPoint
class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    val viewModel: EditorViewModel by viewModels()

    // Sub-views / controllers
    private lateinit var previewController: PreviewController
    private lateinit var timelineController: TimelineController
    private lateinit var toolbarController: ToolbarController
    private lateinit var propertiesController: PropertiesController

    // Recording receiver
    private val recordReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val path = intent.getStringExtra(AudioRecordService.EXTRA_RESULT_PATH) ?: return
            Timber.d("Recording done: $path")
            addAudioClipFromPath(path)
        }
    }

    // Permission launcher
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            Timber.d("All permissions granted")
        }
    }

    // Media picker
    private val mediaPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importMediaUri(it) }
    }

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during editing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()

        // Load or create project
        val projectId = intent.getStringExtra("project_id")
        if (projectId != null) {
            viewModel.loadProject(projectId)
        } else {
            viewModel.createNewProject("New Project", AspectRatio.REELS_9_16)
        }

        // Initialize controllers
        previewController  = PreviewController(this, binding, viewModel)
        timelineController = TimelineController(this, binding, viewModel)
        toolbarController  = ToolbarController(this, binding, viewModel)
        propertiesController = PropertiesController(this, binding, viewModel)

        observeViewModel()
        setupTopBar()
        applyTheme()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            recordReceiver,
            IntentFilter(AudioRecordService.BROADCAST_DONE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(recordReceiver)
        viewModel.saveNow()   // save to DB every time we go to background
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) viewModel.saveNow()
    }

    // ── Observers ─────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.project.collect { project ->
                        project ?: return@collect
                        binding.tvProjectName.text = project.name
                    }
                }

                launch {
                    viewModel.canUndo.collect { binding.btnUndo.isEnabled = it }
                }

                launch {
                    viewModel.canRedo.collect { binding.btnRedo.isEnabled = it }
                }

                launch {
                    viewModel.exportState.collect { state ->
                        when (state) {
                            is ExportState.Idle -> {}
                            is ExportState.Preparing ->
                                showSnack("Preparing export…")
                            is ExportState.Running ->
                                showSnack("Exporting: ${state.progress}%")
                            is ExportState.Done -> {
                                showSnack("Export complete! Saved to ${state.outputPath}")
                                viewModel.resetExportState()
                            }
                            is ExportState.Error -> {
                                showSnack("Export error: ${state.message}")
                                viewModel.resetExportState()
                            }
                        }
                    }
                }

                launch {
                    viewModel.snackbar.collect { showSnack(it) }
                }
            }
        }
    }

    // ── Top bar setup ─────────────────────────────────────────

    private fun setupTopBar() {
        binding.btnUndo.setOnClickListener    { viewModel.undo() }
        binding.btnRedo.setOnClickListener    { viewModel.redo() }

        binding.btnCaptureFrame.setOnClickListener {
            previewController.captureFrameAsPng()
        }

        binding.btnDeleteClip.setOnClickListener {
            viewModel.deleteSelectedClip()
        }

        binding.btnAddMedia.setOnClickListener {
            showAddMediaDialog()
        }

        binding.btnExport.setOnClickListener {
            ExportBottomSheet.show(supportFragmentManager, viewModel)
        }

        binding.btnKeyframeAnim.setOnClickListener {
            // Open keyframe/animation panel
            propertiesController.showKeyframePanel()
        }

        binding.tvProjectName.setOnClickListener {
            showRenameProjectDialog()
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun showAddMediaDialog() {
        AddMediaDialog.show(supportFragmentManager) { action ->
            when (action) {
                AddMediaDialog.Action.BLACK_SCREEN -> addBlackScreenClip()
                AddMediaDialog.Action.IMPORT       -> mediaPicker.launch("video/*")
                AddMediaDialog.Action.IMPORT_IMAGE -> mediaPicker.launch("image/*")
            }
        }
    }

    private fun importMediaUri(uri: android.net.Uri) {
        val path = FileUtil.getPathFromUri(this, uri) ?: return
        val trackId = viewModel.project.value?.tracks
            ?.firstOrNull { it.type == TrackType.VIDEO }?.id ?: return
        val durationMs = MediaUtil.getDurationMs(this, uri)
        val playhead = viewModel.playheadMs.value

        val clip = TimelineClip(
            id           = UUID.randomUUID().toString(),
            trackId      = trackId,
            type         = TrackType.VIDEO,
            sourcePath   = path,
            startTimeMs  = playhead,
            endTimeMs    = playhead + durationMs,
        )
        viewModel.addClip(trackId, clip)
    }

    private fun addBlackScreenClip() {
        val trackId = viewModel.project.value?.tracks
            ?.firstOrNull { it.type == TrackType.VIDEO }?.id ?: return
        val playhead = viewModel.playheadMs.value
        val clip = TimelineClip(
            id          = UUID.randomUUID().toString(),
            trackId     = trackId,
            type        = TrackType.VIDEO,
            sourcePath  = "black_screen",   // handled by FFmpegCommandBuilder as color=black
            startTimeMs = playhead,
            endTimeMs   = playhead + 3000L,
        )
        viewModel.addClip(trackId, clip)
    }

    private fun addAudioClipFromPath(path: String) {
        val trackId = viewModel.project.value?.tracks
            ?.firstOrNull { it.type == TrackType.AUDIO }?.id ?: return
        val playhead = viewModel.playheadMs.value
        val durationMs = MediaUtil.getAudioDurationMs(path)
        val clip = TimelineClip(
            id          = UUID.randomUUID().toString(),
            trackId     = trackId,
            type        = TrackType.AUDIO,
            sourcePath  = path,
            startTimeMs = playhead,
            endTimeMs   = playhead + durationMs,
        )
        viewModel.addClip(trackId, clip)
    }

    private fun showRenameProjectDialog() {
        val current = viewModel.project.value?.name ?: return
        // Simple inline edit — replace with custom dialog in production
        binding.tvProjectName.isEnabled = false
        val editText = android.widget.EditText(this).apply { setText(current) }
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_project))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.project.value?.let { proj ->
                        viewModel.setBackground(proj.background) // trigger a mutate with new name
                        // Properly: viewModel.renameProject(newName)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { binding.tvProjectName.isEnabled = true }
            .show()
    }

    private fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permLauncher.launch(needed.toTypedArray())
    }

    private fun applyTheme() {
        // Theme is applied via AppCompatDelegate — auto from system
    }

    private fun showSnack(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }
}
