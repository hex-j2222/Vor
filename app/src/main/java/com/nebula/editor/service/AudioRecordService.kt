package com.nebula.editor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class AudioRecordService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputPath: String = ""
    private var isRecording = false

    companion object {
        const val CHANNEL_ID     = "nebula_record"
        const val NOTIF_ID       = 2001
        const val EXTRA_OUT_PATH = "output_path"
        const val ACTION_START   = "START_RECORDING"
        const val ACTION_STOP    = "STOP_RECORDING"

        // Broadcast result
        const val BROADCAST_DONE  = "com.nebula.editor.RECORD_DONE"
        const val EXTRA_RESULT_PATH = "result_path"

        fun start(context: Context, outputPath: String) {
            val intent = Intent(context, AudioRecordService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OUT_PATH, outputPath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AudioRecordService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                outputPath = intent.getStringExtra(EXTRA_OUT_PATH)
                    ?: File(cacheDir, "rec_${System.currentTimeMillis()}.m4a").absolutePath
                startRecording()
            }
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        try {
            startForeground(NOTIF_ID, buildNotif("Recording…"))

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(this)
            else
                @Suppress("DEPRECATION") MediaRecorder()

            recorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // Original quality: AAC-LC, 256 kbps, 48 kHz, stereo
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(256_000)
                setAudioSamplingRate(48_000)
                setAudioChannels(2)
                setOutputFile(outputPath)
                prepare()
                start()
            }
            isRecording = true
            Timber.d("Recording started: $outputPath")
        } catch (e: Exception) {
            Timber.e(e, "Recording start failed")
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false

            // Broadcast result back to activity
            sendBroadcast(Intent(BROADCAST_DONE).apply {
                putExtra(EXTRA_RESULT_PATH, outputPath)
                setPackage(packageName)
            })

            Timber.d("Recording stopped: $outputPath")
        } catch (e: Exception) {
            Timber.e(e, "Recording stop failed")
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID, "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
    }

    private fun buildNotif(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nebula — Recording")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.presence_audio_online)
            .setOngoing(true)
            .setSilent(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isRecording) stopRecording()
        super.onDestroy()
    }
}
