package com.nebula.editor.util

import android.content.Context
import com.nebula.editor.data.repository.ProjectRepository
import com.nebula.editor.model.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Installs a global uncaught-exception handler that auto-saves the
 * current project state before the process dies.
 *
 * Also called explicitly from EditorActivity.onStop() and onTrimMemory()
 * so the project is always safe on background-kill or power-off.
 */
@Singleton
class CrashRecoveryManager @Inject constructor(
    private val repository: ProjectRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentProject: Project? = null
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception — saving project before crash")
            // Synchronous save on crash thread
            currentProject?.let { project ->
                runCatching {
                    kotlinx.coroutines.runBlocking {
                        repository.saveProject(project)
                    }
                }
            }
            // Delegate to default handler (shows crash dialog / restarts)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /** Call this every time the project state changes */
    fun updateProject(project: Project) {
        currentProject = project
    }

    /** Explicit save — call from onStop, onTrimMemory, onDestroy */
    fun saveNow() {
        currentProject?.let { project ->
            scope.launch {
                runCatching { repository.saveProject(project) }
                    .onFailure { Timber.e(it, "Auto-save failed") }
                    .onSuccess { Timber.d("Auto-save OK: ${project.name}") }
            }
        }
    }
}
