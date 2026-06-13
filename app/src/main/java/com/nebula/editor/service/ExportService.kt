package com.nebula.editor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nebula.editor.R
import com.nebula.editor.data.repository.ProjectRepository
import com.nebula.editor.model.ExportConfig
import com.nebula.editor.model.ExportFormat
import com.nebula.editor.model.Project
import com.nebula.editor.util.FFmpegCommandBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ExportService : Service() {

    @Inject lateinit var repository: ProjectRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var exportJob: Job? = null

    companion object {
        const val CHANNEL_ID   = "nebula_export"
        const val NOTIF_ID     = 1001
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_CONFIG = "export_config"
        const val EXTRA_PROJECT = "project_json"

        fun start(context: Context, jobId: String, project: Project, config: ExportConfig) {
            val intent = Intent(context, ExportService::class.java).apply {
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_CONFIG, config)
                putExtra(EXTRA_PROJECT, project)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ExportService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId   = intent?.getStringExtra(EXTRA_JOB_ID) ?: return START_NOT_STICKY
        val config  = intent.getParcelableExtra<ExportConfig>(EXTRA_CONFIG) ?: return START_NOT_STICKY
        val project = intent.getParcelableExtra<Project>(EXTRA_PROJECT) ?: return START_NOT_STICKY

        startForeground(NOTIF_ID, buildNotification(0, "Preparing export…"))

        exportJob = scope.launch {
            runExport(jobId, project, config)
        }

        return START_REDELIVER_INTENT   // re-deliver intent on process kill → resume
    }

    private suspend fun runExport(jobId: String, project: Project, config: ExportConfig) {
        try {
            repository.updateExportProgress(jobId, "RUNNING", 0)

            val outputFile = resolveOutputPath(config)
            val command = FFmpegCommandBuilder.build(project, config, outputFile)

            Timber.d("FFmpeg command: $command")

            // Store resume token (output path prefix) so we can seek forward on resume
            repository.updateResumeToken(jobId, outputFile)

            var lastProgress = 0
            FFmpegKit.executeAsync(
                command,
                { session ->
                    scope.launch {
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            repository.updateExportProgress(jobId, "DONE", 100)
                            notifyDone(outputFile)
                        } else {
                            val err = session.failStackTrace ?: "Unknown error"
                            Timber.e("FFmpeg failed: $err")
                            repository.updateExportProgress(jobId, "FAILED", lastProgress)
                            notifyError(err)
                        }
                        stopSelf()
                    }
                },
                { _ -> /* log callback — ignored */ },
                { stats ->
                    val totalMs = project.durationMs.coerceAtLeast(1)
                    val progress = ((stats.time.toFloat() / totalMs) * 100).toInt().coerceIn(0, 99)
                    if (progress != lastProgress) {
                        lastProgress = progress
                        scope.launch {
                            repository.updateExportProgress(jobId, "RUNNING", progress)
                            updateNotification(progress)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Export error")
            repository.updateExportProgress(jobId, "FAILED", 0)
            notifyError(e.message ?: "Unknown error")
            stopSelf()
        }
    }

    private fun resolveOutputPath(config: ExportConfig): String {
        if (config.outputPath.isNotBlank()) return config.outputPath
        val dir = File(getExternalFilesDir(null), "Exports").apply { mkdirs() }
        val ext = when (config.format) {
            ExportFormat.MP4 -> "mp4"
            ExportFormat.MKV -> "mkv"
            ExportFormat.GIF -> "gif"
        }
        return File(dir, "nebula_export_${System.currentTimeMillis()}.$ext").absolutePath
    }

    // ── Notifications ─────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID, "Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Nebula Editor export progress" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
    }

    private fun buildNotification(progress: Int, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nebula Editor — Exporting")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun updateNotification(progress: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(progress, "$progress% complete"))
    }

    private fun notifyDone(path: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID + 1,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Export complete!")
                .setContentText("Saved to: $path")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun notifyError(error: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID + 2,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Export failed")
                .setContentText(error)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build()
        )
    }

    override fun onDestroy() {
        exportJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
