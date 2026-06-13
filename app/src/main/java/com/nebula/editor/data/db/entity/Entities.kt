package com.nebula.editor.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stored project row — project JSON is serialized via Gson */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Full Project serialized as JSON */
    val projectJson: String,
    /** Thumbnail path */
    val thumbnailPath: String? = null,
)

/** Persisted undo history item */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val projectId: String,
    val description: String,
    val snapshotJson: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Export jobs — for resume on crash */
@Entity(tableName = "export_jobs")
data class ExportJobEntity(
    @PrimaryKey val jobId: String,
    val projectId: String,
    val configJson: String,
    val status: String,        // PENDING | RUNNING | DONE | FAILED
    val progressPercent: Int = 0,
    val outputPath: String? = null,
    val resumeToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
