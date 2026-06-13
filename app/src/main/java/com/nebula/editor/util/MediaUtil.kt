package com.nebula.editor.util

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import timber.log.Timber
import java.io.File

object MediaUtil {

    fun getDurationMs(context: Context, uri: Uri): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.release()
            dur
        } catch (e: Exception) {
            Timber.e(e)
            0L
        }
    }

    fun getAudioDurationMs(path: String): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.release()
            dur
        } catch (e: Exception) {
            Timber.e(e)
            0L
        }
    }

    fun getVideoResolution(path: String): Pair<Int, Int> {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            mmr.release()
            w to h
        } catch (e: Exception) { 0 to 0 }
    }
}

object FileUtil {

    fun getPathFromUri(context: Context, uri: Uri): String? {
        // Handle content:// URIs
        if (uri.scheme == "content") {
            try {
                val cursor: Cursor? = context.contentResolver.query(
                    uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val colIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        val path   = it.getString(colIdx)
                        if (!path.isNullOrBlank()) return path
                    }
                }
            } catch (e: Exception) { Timber.e(e) }

            // Fallback: copy to cache
            return copyUriToCache(context, uri)
        }

        if (uri.scheme == "file") return uri.path
        return uri.path
    }

    private fun copyUriToCache(context: Context, uri: Uri): String? {
        return try {
            val ext     = getMimeExtension(context, uri)
            val outFile = File(context.cacheDir, "media_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy URI to cache")
            null
        }
    }

    private fun getMimeExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: "video/mp4"
        return when {
            mime.contains("mp4")  -> "mp4"
            mime.contains("webm") -> "webm"
            mime.contains("mkv")  -> "mkv"
            mime.contains("aac")  -> "aac"
            mime.contains("mp3")  -> "mp3"
            mime.contains("ogg")  -> "ogg"
            mime.contains("png")  -> "png"
            mime.contains("jpeg") -> "jpg"
            mime.contains("gif")  -> "gif"
            else -> "bin"
        }
    }

    fun getFileSizeMb(path: String): Float =
        try { File(path).length() / 1024f / 1024f } catch (e: Exception) { 0f }
}
