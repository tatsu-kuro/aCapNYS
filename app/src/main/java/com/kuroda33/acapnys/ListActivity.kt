package com.kuroda33.acapnys

import com.kuroda33.acapnys.R
import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListActivity : AppCompatActivity() {

    private data class VideoItem(
        val uri: Uri,
        val dateMs: Long,
        val recordedMs: Long,
        val durationMs: Long,
        val name: String
    )

    private val displayList = mutableListOf<String>()
    private val uriList = mutableListOf<Uri>()
    private lateinit var listView: ListView
    private var selectedUriString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById(R.id.listView)
        val backButton = findViewById<ImageButton>(R.id.btnBack)

        backButton.setOnClickListener { finish() }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayList) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val row = super.getView(position, convertView, parent)
                val isSelected = uriList.getOrNull(position)?.toString() == selectedUriString
                (row as TextView).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    setTextColor(if (isSelected) Color.parseColor("#FFB000") else Color.BLACK)
                    setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                }
                return row
            }
        }
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val uri = uriList.getOrNull(position) ?: return@setOnItemClickListener
            selectedUriString = uri.toString()
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("selected_uri", selectedUriString).apply()
            val intent = Intent(this, PlayActivity::class.java).apply {
                putExtra("videouri", uri.toString())
            }
            startActivity(intent)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val uri = uriList.getOrNull(position) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete this video?")
                .setPositiveButton("Delete") { _, _ ->
                    contentResolver.delete(uri, null, null)
                    deleteCsvIfExists(getFileName(uri))
                    reloadList(adapter)
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        reloadList(adapter)
    }

    override fun onResume() {
        super.onResume()
        selectedUriString = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("selected_uri", selectedUriString)
        val adapter = listView.adapter as ArrayAdapter<String>
        reloadList(adapter)
    }

    private fun reloadList(adapter: ArrayAdapter<String>) {
        displayList.clear()
        uriList.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Movies/aCapNYS%")
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val items = mutableListOf<VideoItem>()
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val name = it.getString(nameCol)
                val recordedMs = parseRecordedTimeFromName(name) ?: it.getLong(dateCol) * 1000L
                items.add(VideoItem(uri, it.getLong(dateCol) * 1000L, recordedMs, it.getLong(durCol), name))
            }
        }

        items.sortedByDescending { it.recordedMs }.forEachIndexed { index, item ->
            uriList.add(item.uri)
            val mergedTag = if (item.name.contains("_merged", ignoreCase = true)) " [merged]" else ""
            val recordedTime = formatRecordedTimeFromName(item.name)
                ?: sdf.format(Date(item.recordedMs))
            displayList.add("(${items.size - index}) $recordedTime ${formatDuration(item.durationMs)}$mergedTag")
        }

        adapter.notifyDataSetChanged()
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteCsvIfExists(videoName: String?) {
        try {
            if (videoName == null) return
            val csvName = videoName.removeSuffix(".mp4") + ".csv"
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection =
                "${MediaStore.Files.FileColumns.DISPLAY_NAME}=? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(csvName, "%Documents/aCapNYS%")
            val baseUri = MediaStore.Files.getContentUri("external")

            contentResolver.query(baseUri, projection, selection, selectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val csvUri = ContentUris.withAppendedId(baseUri, id)
                    contentResolver.delete(csvUri, null, null)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun getDurationMs(filePath: String?): Long {
        return try {
            if (filePath.isNullOrBlank()) return 0L
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            retriever.release()
            duration
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val hour = totalSec / 3600
        val min = (totalSec % 3600) / 60
        val sec = totalSec % 60
        return if (hour > 0) {
            String.format("[%02d:%02d:%02d]", hour, min, sec)
        } else {
            String.format("[%02d:%02d]", min, sec)
        }
    }

    private fun formatRecordedTimeFromName(name: String): String? {
        val stem = name.removeSuffix("_merged").removeSuffix(".mp4")
        return try {
            val parsed = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).parse(stem)
                ?: return null
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(parsed)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRecordedTimeFromName(name: String): Long? {
        val stem = name.removeSuffix("_merged").removeSuffix(".mp4")
        return try {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).parse(stem)?.time
        } catch (_: Exception) {
            null
        }
    }
}
