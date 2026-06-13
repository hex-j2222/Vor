package com.nebula.editor.data.repository

import com.google.gson.Gson
import com.nebula.editor.data.db.ExportJobDao
import com.nebula.editor.data.db.HistoryDao
import com.nebula.editor.data.db.ProjectDao
import com.nebula.editor.data.db.entity.ExportJobEntity
import com.nebula.editor.data.db.entity.HistoryEntity
import com.nebula.editor.data.db.entity.ProjectEntity
import com.nebula.editor.model.ExportConfig
import com.nebula.editor.model.HistoryEntry
import com.nebula.editor.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val historyDao: HistoryDao,
    private val exportJobDao: ExportJobDao,
    private val gson: Gson,
) {

    // ── Projects ──────────────────────────────────────────────

    fun getAllProjects(): Flow<List<Project>> =
        projectDao.getAllProjects().map { list ->
            list.map { gson.fromJson(it.projectJson, Project::class.java) }
        }

    suspend fun getProject(id: String): Project? =
        projectDao.getProject(id)?.let { gson.fromJson(it.projectJson, Project::class.java) }

    suspend fun saveProject(project: Project) {
        projectDao.upsert(
            ProjectEntity(
                id           = project.id,
                name         = project.name,
                createdAt    = project.createdAt,
                updatedAt    = System.currentTimeMillis(),
                projectJson  = gson.toJson(project),
            )
        )
    }

    suspend fun deleteProject(id: String) {
        projectDao.delete(id)
        historyDao.clearHistory(id)
    }

    // ── History (undo/redo) ───────────────────────────────────

    suspend fun pushHistory(entry: HistoryEntry) {
        historyDao.insert(
            HistoryEntity(
                projectId    = entry.snapshot.id,
                description  = entry.description,
                snapshotJson = gson.toJson(entry.snapshot),
                timestamp    = entry.timestamp,
            )
        )
        // Keep max 200 history items per project
        historyDao.trimHistory(entry.snapshot.id, 200)
    }

    suspend fun getHistory(projectId: String): List<HistoryEntry> =
        historyDao.getHistory(projectId).map {
            HistoryEntry(
                description = it.description,
                snapshot    = gson.fromJson(it.snapshotJson, Project::class.java),
                timestamp   = it.timestamp,
            )
        }

    // ── Export jobs ───────────────────────────────────────────

    suspend fun saveExportJob(jobId: String, projectId: String, config: ExportConfig) {
        exportJobDao.upsert(
            ExportJobEntity(
                jobId      = jobId,
                projectId  = projectId,
                configJson = gson.toJson(config),
                status     = "PENDING",
            )
        )
    }

    suspend fun updateExportProgress(jobId: String, status: String, progress: Int) {
        exportJobDao.updateProgress(jobId, status, progress)
    }

    suspend fun updateResumeToken(jobId: String, token: String) {
        exportJobDao.updateResumeToken(jobId, token)
    }

    suspend fun getActiveExportJob(): ExportJobEntity? = exportJobDao.getActiveJob()

    suspend fun getExportJob(jobId: String): ExportJobEntity? = exportJobDao.getJob(jobId)
}
