package com.kuroda33.acapnys

import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import java.util.Locale

class ListActivity : AppCompatActivity() {

    private val displayList = mutableListOf<String>()
    private val uriList = mutableListOf<Uri>()
    private lateinit var listView: ListView
    private val dbHelper by lazy { DatabaseHelper(this) }
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
                    setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                }
                return row
            }
        }
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val uri = uriList[position]
            selectedUriString = uri.toString()
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("selected_uri", selectedUriString).apply()
            val name = getDisplayName(uri)?.removeSuffix(".mp4") ?: ""
            val gyroData = getGyroData(name)
            val intent = Intent(this, PlayActivity::class.java).apply {
                putExtra("videouri", uri.toString())
                putExtra("gyrodata", gyroData)
            }
            startActivity(intent)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val uri = uriList[position]
            val name = getDisplayName(uri) ?: ""
            AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete this video?")
                .setPositiveButton("Delete") { _, _ ->
                    contentResolver.delete(uri, null, null)
                    deleteGyroData(name.removeSuffix(".mp4"))
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
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%aCapNYS%")

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            val items = mutableListOf<QuadrupleData>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val date = cursor.getLong(dateCol)
                val displayName = cursor.getString(nameCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                items.add(QuadrupleData(uri, date, getDurationMs(uri), displayName))
            }
            items.sortByDescending { it.date }

            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            val total = items.size
            items.forEachIndexed { index, item ->
                uriList.add(item.uri)
                displayList.add("(${total - index}) ${sdf.format(item.date * 1000L)} ${formatDuration(item.duration)}")
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun getDisplayName(uri: Uri): String? {
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

    private fun getGyroData(key: String): String {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "headgyrodata",
            arrayOf("data"),
            "name = ?",
            arrayOf(key),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return ""
    }

    private fun deleteGyroData(key: String) {
        val db = dbHelper.writableDatabase
        db.delete("headgyrodata", "name = ?", arrayOf(key))
    }

    private fun getDurationMs(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            retriever.release()
            duration
        } catch (_: Exception) {
            0L
        }
    }

    private data class QuadrupleData(
        val uri: Uri,
        val date: Long,
        val duration: Long,
        val name: String,
    )

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
}
