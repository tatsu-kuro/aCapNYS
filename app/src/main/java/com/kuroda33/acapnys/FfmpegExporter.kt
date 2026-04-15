package com.kuroda33.acapnys

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.daasuu.mp4compose.composer.Mp4Composer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FfmpegExporter(private val context: Context) {

    private val syncOffsetMs = 0L

    fun export(
        sourceUriString: String,
        data: List<DataPoint>,
        onProgress: (Float) -> Unit = {},
        onSuccess: (Uri, Uri?) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        try {
            val sourceFile = resolveSourceFile(sourceUriString)
            val durationMs = getDurationMs(sourceFile)
            if (durationMs <= 0L) throw IllegalStateException("video duration is invalid")

            val workDir = File(context.cacheDir, "video_export")
            if (workDir.exists()) workDir.deleteRecursively()
            workDir.mkdirs()

            val outputTemp = File(workDir, "output.mp4")
            val outputDisplayName = buildOutputDisplayName(sourceUriString)
            val overlayFilter = GyroOverlayFilter(
                context,
                data,
                durationMs,
                syncOffsetMs,
                0,
                onProgress
            )

            Mp4Composer(sourceFile.absolutePath, outputTemp.absolutePath)
                .filter(overlayFilter)
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) = Unit
                    override fun onCurrentWrittenVideoTime(timeUs: Long) = Unit
                    override fun onCompleted() {
                        try {
                            val savedUri = saveToMediaStore(outputTemp, outputDisplayName)
                            onSuccess(savedUri, originalVideoUri(sourceUriString))
                        } catch (e: Exception) {
                            onError(e.message ?: e.toString())
                        } finally {
                            workDir.deleteRecursively()
                        }
                    }

                    override fun onCanceled() {
                        workDir.deleteRecursively()
                        onError("export canceled")
                    }

                    override fun onFailed(exception: Exception) {
                        workDir.deleteRecursively()
                        onError(exception.message ?: exception.toString())
                    }
                })
                .start()
        } catch (e: Exception) {
            onError(e.message ?: e.toString())
        }
    }

    private fun resolveSourceFile(sourceUriString: String): File {
        return when {
            sourceUriString.startsWith("content://") -> copyContentUriToTempFile(Uri.parse(sourceUriString))
            sourceUriString.startsWith("file://") -> File(Uri.parse(sourceUriString).path ?: sourceUriString)
            else -> File(sourceUriString)
        }
    }

    private fun copyContentUriToTempFile(uri: Uri): File {
        val temp = File(context.cacheDir, "video_input_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("cannot open input uri")
        return temp
    }

    private fun getDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    private fun saveToMediaStore(outputTemp: File, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/aCapNYS")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IllegalStateException("failed to create output uri")

        context.contentResolver.openOutputStream(uri)?.use { output ->
            FileInputStream(outputTemp).use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("failed to open output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pending = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, pending, null, null)
        }

        return uri
    }

    private fun originalVideoUri(sourceUriString: String): Uri? {
        return when {
            sourceUriString.startsWith("content://") -> Uri.parse(sourceUriString)
            sourceUriString.startsWith("file://") -> {
                val file = File(Uri.parse(sourceUriString).path ?: sourceUriString)
                findMediaStoreUriByDisplayName(file.name)
            }
            else -> {
                val file = File(sourceUriString)
                findMediaStoreUriByDisplayName(file.name)
            }
        }
    }

    private fun findMediaStoreUriByDisplayName(displayName: String): Uri? {
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME}=? AND ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(displayName, "%aCapNYS%")
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    fun deleteSourceMedia(uri: Uri?) {
        if (uri == null) return
        try {
            val name = queryDisplayName(uri)
            context.contentResolver.delete(uri, null, null)
            if (!name.isNullOrBlank()) {
                deleteCsvByVideoName(name)
            }
        } catch (_: Exception) {
        }
    }

    private fun deleteCsvByVideoName(videoName: String) {
        val csvName = normalizeCsvName(videoName)
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME}=? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(csvName, "%Documents/aCapNYS%")
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val csvUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                context.contentResolver.delete(csvUri, null, null)
            }
        }
    }

    private fun buildOutputDisplayName(sourceUriString: String): String {
        val baseName = when {
            sourceUriString.startsWith("content://") -> queryDisplayName(Uri.parse(sourceUriString))
            sourceUriString.startsWith("file://") -> File(Uri.parse(sourceUriString).path ?: sourceUriString).name
            else -> File(sourceUriString).name
        } ?: "EyeRec_${System.currentTimeMillis()}"

        val stem = baseName.removeSuffix(".mp4")
        return "${stem}_merged.mp4"
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }

    private fun normalizeCsvName(videoName: String): String {
        return videoName.removeSuffix(".mp4") + ".csv"
    }

    private fun dpToPx(dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()
}
