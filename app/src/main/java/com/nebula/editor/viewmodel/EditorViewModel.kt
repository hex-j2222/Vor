package com.nebula.editor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.editor.data.repository.ProjectRepository
import com.nebula.editor.model.*
import com.nebula.editor.util.CrashRecoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val crashRecovery: CrashRecoveryManager,
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _magnetEnabled = MutableStateFlow(true)
    val magnetEnabled: StateFlow<Boolean> = _magnetEnabled.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    // ── Undo/Redo stacks ─────────────────────────────────────

    private val undoStack = ArrayDeque<HistoryEntry>()
    private val redoStack = ArrayDeque<HistoryEntry>()
    private val MAX_HISTORY = 200

    // ── Init / Load ──────────────────────────────────────────

    fun loadProject(projectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val proj = repository.getProject(projectId)
            if (proj != null) {
                _project.value = proj
                _playheadMs.value = proj.lastPlayheadMs
                _timelineZoom.value = proj.timelineZoom
                crashRecovery.updateProject(proj)

                // Restore history stacks from DB
                val history = repository.getHistory(projectId)
                undoStack.clear()
                redoStack.clear()
                undoStack.addAll(history.takeLast(MAX_HISTORY))
                updateHistoryFlags()
            }
        }
    }

    fun createNewProject(name: String, ratio: AspectRatio): String {
        val id = UUID.randomUUID().toString()
        val proj = Project(
            id = id,
            name = name,
            aspectRatio = ratio,
            tracks = listOf(
                TimelineTrack(UUID.randomUUID().toString(), TrackType.VIDEO, "Video 1", zIndex = 1),
                TimelineTrack(UUID.randomUUID().toString(), TrackType.AUDIO, "Audio 1", zIndex = 0),
            )
        )
        _project.value = proj
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
            crashRecovery.updateProject(proj)
        }
        return id
    }

    // ── Mutation helpers ──────────────────────────────────────

    private fun mutate(description: String, block: (Project) -> Project) {
        val current = _project.value ?: return
        // Push to undo before mutation
        pushUndo(HistoryEntry(description, current))
        val updated = block(current).copy(updatedAt = System.currentTimeMillis())
        _project.value = updated
        crashRecovery.updateProject(updated)
        // Persist asynchronously
        viewModelScope.launch(Dispatchers.IO) { repository.saveProject(updated) }
    }

    private fun pushUndo(entry: HistoryEntry) {
        undoStack.addLast(entry)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        updateHistoryFlags()
        viewModelScope.launch(Dispatchers.IO) { repository.pushHistory(entry) }
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    // ── Undo / Redo ───────────────────────────────────────────

    fun undo() {
        if (undoStack.isEmpty()) return
        val entry = undoStack.removeLast()
        val current = _project.value ?: return
        redoStack.addLast(HistoryEntry("Redo: ${entry.description}", current))
        _project.value = entry.snapshot
        crashRecovery.updateProject(entry.snapshot)
        viewModelScope.launch(Dispatchers.IO) { repository.saveProject(entry.snapshot) }
        updateHistoryFlags()
        viewModelScope.launch { _snackbar.emit("Undone: ${entry.description}") }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val entry = redoStack.removeLast()
        val current = _project.value ?: return
        undoStack.addLast(HistoryEntry("Undo: ${entry.description}", current))
        _project.value = entry.snapshot
        crashRecovery.updateProject(entry.snapshot)
        viewModelScope.launch(Dispatchers.IO) { repository.saveProject(entry.snapshot) }
        updateHistoryFlags()
        viewModelScope.launch { _snackbar.emit("Redone: ${entry.description}") }
    }

    // ── Playback ──────────────────────────────────────────────

    fun togglePlayback() { _isPlaying.value = !_isPlaying.value }
    fun setPlayhead(ms: Long) { _playheadMs.value = ms.coerceAtLeast(0) }
    fun seekToStart() { _playheadMs.value = 0L; _isPlaying.value = false }
    fun seekToEnd() {
        _playheadMs.value = _project.value?.durationMs ?: 0L
        _isPlaying.value = false
    }

    // ── Timeline Zoom ─────────────────────────────────────────

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.1f, 32f)
        mutate("Zoom") { it.copy(timelineZoom = _timelineZoom.value) }
    }

    fun zoomIn() = setTimelineZoom(_timelineZoom.value * 2f)
    fun zoomOut() = setTimelineZoom(_timelineZoom.value / 2f)

    // ── Magnet ────────────────────────────────────────────────

    fun toggleMagnet() { _magnetEnabled.value = !_magnetEnabled.value }

    // ── Clip selection ────────────────────────────────────────

    fun selectClip(clipId: String?) { _selectedClipId.value = clipId }

    fun getSelectedClip(): TimelineClip? {
        val clipId = _selectedClipId.value ?: return null
        return _project.value?.tracks
            ?.flatMap { it.clips }
            ?.firstOrNull { it.id == clipId }
    }

    // ── Tracks ────────────────────────────────────────────────

    fun addTrack(type: TrackType) {
        mutate("Add track") { proj ->
            val newTrack = TimelineTrack(
                id     = UUID.randomUUID().toString(),
                type   = type,
                label  = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ${proj.tracks.count { it.type == type } + 1}",
                zIndex = proj.tracks.size,
            )
            proj.copy(tracks = proj.tracks + newTrack)
        }
    }

    fun deleteTrack(trackId: String) {
        mutate("Delete track") { proj ->
            proj.copy(tracks = proj.tracks.filter { it.id != trackId })
        }
    }

    fun setTrackMute(trackId: String, muted: Boolean) {
        mutate("${if (muted) "Mute" else "Unmute"} track") { proj ->
            proj.copy(tracks = proj.tracks.map {
                if (it.id == trackId) it.copy(isMuted = muted) else it
            })
        }
    }

    // ── Clips ─────────────────────────────────────────────────

    fun addClip(trackId: String, clip: TimelineClip) {
        mutate("Add clip") { proj ->
            proj.copy(
                tracks = proj.tracks.map { track ->
                    if (track.id == trackId)
                        track.copy(clips = track.clips + clip)
                    else track
                },
                durationMs = maxOf(proj.durationMs, clip.endTimeMs)
            )
        }
    }

    fun deleteSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        mutate("Delete clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.filter { it.id != clipId })
            })
        }
        _selectedClipId.value = null
    }

    fun moveClip(clipId: String, newStartMs: Long) {
        mutate("Move clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        val dur = clip.durationMs
                        clip.copy(startTimeMs = newStartMs, endTimeMs = newStartMs + dur)
                    } else clip
                })
            })
        }
    }

    fun trimClip(clipId: String, newStart: Long, newEnd: Long) {
        mutate("Trim clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId)
                        clip.copy(startTimeMs = newStart, endTimeMs = newEnd)
                    else clip
                })
            })
        }
    }

    /** Split clip at current playhead */
    fun splitClipAtPlayhead() {
        val clipId = _selectedClipId.value ?: return
        val playhead = _playheadMs.value
        mutate("Split clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                val newClips = mutableListOf<TimelineClip>()
                track.clips.forEach { clip ->
                    if (clip.id == clipId && playhead > clip.startTimeMs && playhead < clip.endTimeMs) {
                        // Left part
                        newClips.add(clip.copy(
                            id = UUID.randomUUID().toString(),
                            endTimeMs = playhead,
                        ))
                        // Right part
                        newClips.add(clip.copy(
                            id = UUID.randomUUID().toString(),
                            startTimeMs = playhead,
                        ))
                    } else {
                        newClips.add(clip)
                    }
                }
                track.copy(clips = newClips)
            })
        }
    }

    fun duplicateClip(clipId: String) {
        mutate("Duplicate clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                val newClips = mutableListOf<TimelineClip>()
                track.clips.forEach { clip ->
                    newClips.add(clip)
                    if (clip.id == clipId) {
                        val offset = clip.durationMs
                        newClips.add(clip.copy(
                            id = UUID.randomUUID().toString(),
                            startTimeMs = clip.startTimeMs + offset,
                            endTimeMs   = clip.endTimeMs + offset,
                        ))
                    }
                }
                track.copy(clips = newClips)
            })
        }
    }

    fun reverseClip(clipId: String) {
        mutate("Reverse clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(isReversed = !clip.isReversed) else clip
                })
            })
        }
    }

    fun setClipVolume(clipId: String, volume: Float) {
        mutate("Set volume") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(volume = volume.coerceIn(0f, 5f)) else clip
                })
            })
        }
    }

    fun setClipSpeed(clipId: String, speed: Float) {
        mutate("Set speed") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        val newDuration = (clip.durationMs / speed).toLong()
                        clip.copy(
                            speedPoints = listOf(SpeedPoint(0L, speed)),
                            endTimeMs = clip.startTimeMs + newDuration
                        )
                    } else clip
                })
            })
        }
    }

    fun setClipTransform(clipId: String, transform: LayerTransform) {
        mutate("Transform clip") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(transform = transform) else clip
                })
            })
        }
    }

    fun freezeFrame(clipId: String) {
        val playhead = _playheadMs.value
        mutate("Freeze frame") { proj ->
            proj.copy(tracks = proj.tracks.map { track ->
                val newClips = mutableListOf<TimelineClip>()
                track.clips.forEach { clip ->
                    if (clip.id == clipId && playhead >= clip.startTimeMs && playhead <= clip.endTimeMs) {
                        // Split + insert freeze (2-second image clip)
                        if (playhead > clip.startTimeMs) {
                            newClips.add(clip.copy(
                                id = UUID.randomUUID().toString(),
                                endTimeMs = playhead
                            ))
                        }
                        // Freeze clip (2s still)
                        newClips.add(clip.copy(
                            id = UUID.randomUUID().toString(),
                            type = TrackType.VIDEO,
                            startTimeMs = playhead,
                            endTimeMs = playhead + 2000L,
                            trimStartMs = playhead - clip.startTimeMs,
                            trimEndMs   = playhead - clip.startTimeMs + 1L,
                        ))
                        if (playhead < clip.endTimeMs) {
                            newClips.add(clip.copy(
                                id = UUID.randomUUID().toString(),
                                startTimeMs = playhead + 2000L,
                                endTimeMs   = clip.endTimeMs + 2000L,
                            ))
                        }
                    } else {
                        newClips.add(clip)
                    }
                }
                track.copy(clips = newClips)
            })
        }
    }

    // ── Background ────────────────────────────────────────────

    fun setBackground(bg: CanvasBackground) {
        mutate("Set background") { it.copy(background = bg) }
    }

    // ── Aspect Ratio ──────────────────────────────────────────

    fun setAspectRatio(ratio: AspectRatio, customW: Int = 1080, customH: Int = 1920) {
        mutate("Set aspect ratio") { it.copy(aspectRatio = ratio, customWidth = customW, customHeight = customH) }
    }

    // ── Export ────────────────────────────────────────────────

    fun startExport(config: ExportConfig) {
        _exportState.value = ExportState.Preparing
        viewModelScope.launch(Dispatchers.IO) {
            val jobId = UUID.randomUUID().toString()
            val project = _project.value ?: return@launch
            repository.saveExportJob(jobId, project.id, config)
            _exportState.value = ExportState.Running(jobId, 0)
        }
    }

    fun updateExportProgress(progress: Int) {
        val current = _exportState.value
        if (current is ExportState.Running) {
            _exportState.value = current.copy(progress = progress)
        }
    }

    fun onExportComplete(outputPath: String) {
        _exportState.value = ExportState.Done(outputPath)
    }

    fun onExportError(error: String) {
        _exportState.value = ExportState.Error(error)
        viewModelScope.launch { _snackbar.emit("Export failed: $error") }
    }

    fun resetExportState() { _exportState.value = ExportState.Idle }

    // ── Save on background / destroy ─────────────────────────

    fun saveNow() { crashRecovery.saveNow() }

    override fun onCleared() {
        super.onCleared()
        saveNow()
    }
}

// ── Export state sealed class ─────────────────────────────────

sealed class ExportState {
    object Idle : ExportState()
    object Preparing : ExportState()
    data class Running(val jobId: String, val progress: Int) : ExportState()
    data class Done(val outputPath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}
