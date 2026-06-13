package com.nebula.editor.data.db

import androidx.room.*
import com.nebula.editor.data.db.entity.ExportJobEntity
import com.nebula.editor.data.db.entity.HistoryEntity
import com.nebula.editor.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE projectId = :projectId ORDER BY timestamp ASC")
    suspend fun getHistory(projectId: String): List<HistoryEntity>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    /** Keep only last N entries to avoid unbounded growth */
    @Query("""
        DELETE FROM history WHERE projectId = :projectId AND rowId NOT IN (
            SELECT rowId FROM history WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT :limit
        )
    """)
    suspend fun trimHistory(projectId: String, limit: Int = 200)

    @Query("DELETE FROM history WHERE projectId = :projectId")
    suspend fun clearHistory(projectId: String)
}

@Dao
interface ExportJobDao {
    @Query("SELECT * FROM export_jobs WHERE jobId = :jobId")
    suspend fun getJob(jobId: String): ExportJobEntity?

    @Query("SELECT * FROM export_jobs WHERE status IN ('PENDING','RUNNING') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveJob(): ExportJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: ExportJobEntity)

    @Query("UPDATE export_jobs SET status = :status, progressPercent = :progress WHERE jobId = :jobId")
    suspend fun updateProgress(jobId: String, status: String, progress: Int)

    @Query("UPDATE export_jobs SET resumeToken = :token WHERE jobId = :jobId")
    suspend fun updateResumeToken(jobId: String, token: String)
}
