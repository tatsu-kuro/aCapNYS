package com.kuroda33.acapnys

import com.kuroda33.acapnys.R

import android.content.ClipData
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast

import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import kotlin.concurrent.thread

class PlayActivity : AppCompatActivity() {

    private val syncOffsetMs = 0L



    private lateinit var videoView: VideoView
    private lateinit var myView:MyView
    //  private lateinit var timeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val data = mutableListOf<DataPoint>()
    private var currentIndex = 0
    private var lastTime = 0L
    private var sourceUriString = ""
    fun createImageFiles(directory: File, count: Int) {
        val paint = Paint().apply {
            color = Color.RED
            textSize = 50f
        }

        for (i in 0 until count) {
            val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawText("Frame $i", 100f, 100f, paint)

            val file = File(directory, "frame_$i.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
    fun show() {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        val mailButton: ImageButton = findViewById(R.id.btnMail)
        val saveButton: ImageButton = findViewById(R.id.btnSave)
        val uriString = intent.getStringExtra("videouri") ?: return
        sourceUriString = uriString
        val uri = Uri.parse(uriString)
        val displayName = getDisplayName(uriString, uri)
        val merged = isMergedName(displayName)
        saveButton.isEnabled = !merged
        saveButton.isClickable = !merged
        saveButton.isFocusable = !merged
        saveButton.alpha = if (merged) 0.4f else 1f
        if (!merged && displayName != null) {
            loadCSVByName(displayName)
        }

        mailButton.setOnClickListener { sendCurrentVideoByMail() }
        saveButton.setOnClickListener {
            if (merged) return@setOnClickListener
            exportCurrentVideo()
        }

        val backButton: ImageButton = findViewById(R.id.btnBack)
        backButton.setOnClickListener { finish() }
        videoView = findViewById(R.id.videoView)
        myView = findViewById(R.id.myView)
        myView.playMode = !merged
        myView.visibility = if (merged) View.GONE else View.VISIBLE
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(uri)

        if (!merged) {
            myView.alpha = 1f
            if (data.isNotEmpty()) {
                val first = data.first()
                myView.setReferenceQuat(first.q0, first.q1, first.q2, first.q3)
            }
            myView.setRpkPpk()
        }
        videoView.setOnPreparedListener {
            videoView.start()
            if (!merged) {
                currentIndex = 0
                lastTime = 0L
                startSync()
            }
        }

        videoView.setOnErrorListener { _, what, extra ->
            Log.e("PLAY", "video error what=$what extra=$extra uri=$uri")
            false
        }

    }

    private fun loadCSVByName(displayName: String) {
        data.clear()
        val csvName = displayName.removeSuffix(".mp4") + ".csv"
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection =
            "${MediaStore.Files.FileColumns.DISPLAY_NAME}=? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(csvName, "%Documents/aCapNYS%")
        val baseUri = MediaStore.Files.getContentUri("external")

        contentResolver.query(baseUri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val csvUri = ContentUris.withAppendedId(baseUri, id)
                contentResolver.openInputStream(csvUri)?.use { input ->
                    val reader = BufferedReader(InputStreamReader(input))
                    reader.readLine()
                    reader.forEachLine { line ->
                        val sp = line.split(",")
                        if (sp.size >= 5) {
                            data.add(
                                DataPoint(
                                    sp[0].toLongOrNull() ?: 0L,
                                    sp[1].toFloatOrNull() ?: 0f,
                                    sp[2].toFloatOrNull() ?: 0f,
                                    sp[3].toFloatOrNull() ?: 0f,
                                    sp[4].toFloatOrNull() ?: 0f
                                )
                            )
                        }
                    }
                }
            }
        }
        Log.e("CSV_DEBUG", "size=${data.size}")
    }

    private fun getDisplayName(source: String, uri: Uri): String? {
        return if (source.startsWith("content://")) {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } else {
            File(source).name
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    private fun isMergedName(name: String?): Boolean {
        return name?.contains("_merged", ignoreCase = true) == true
    }

    private fun exportCurrentVideo() {
        if (data.isEmpty() || sourceUriString.isBlank()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Exporting...", Toast.LENGTH_SHORT).show()
        val progressText: View = findViewById(R.id.exportProgressText)
        progressText.visibility = View.VISIBLE
        (progressText as? android.widget.TextView)?.text = "0%"
        val exportData = data.toList()
        val exporter = FfmpegExporter(applicationContext)
        thread(name = "video-export") {
            exporter.export(
                sourceUriString = sourceUriString,
                data = exportData,
                onProgress = { progress ->
                    runOnUiThread {
                        (progressText as? android.widget.TextView)?.text = "${(progress * 100).toInt()}%"
                    }
                },
                onSuccess = { savedUri, originalUri ->
                    runOnUiThread {
                        progressText.visibility = View.GONE
                        Toast.makeText(this, "Completed", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("last_video", savedUri.toString())
                        .apply()
                    exporter.deleteSourceMedia(originalUri)
                },
                onError = { message ->
                    runOnUiThread {
                        progressText.visibility = View.GONE
                        Toast.makeText(this, "Export failed: $message", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun sendCurrentVideoByMail() {
        if (sourceUriString.isBlank()) {
            Toast.makeText(this, "No video to send", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = buildShareUri(sourceUriString)
            val fileName = getFileName(uri) ?: "video.mp4"
            val subjectName = shortenSubjectName(fileName)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_SUBJECT, "EyeRec $subjectName")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("video", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "メールで送信"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send email: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildShareUri(source: String): Uri {
        return when {
            source.startsWith("content://") -> Uri.parse(source)
            source.startsWith("file://") -> {
                val file = File(Uri.parse(source).path ?: source)
                FileProvider.getUriForFile(this, "com.kuroda33.acapnys.fileprovider", file)
            }
            else -> {
                val file = File(source)
                FileProvider.getUriForFile(this, "com.kuroda33.acapnys.fileprovider", file)
            }
        }
    }

    private fun shortenSubjectName(fileName: String): String {
        val stem = fileName.removeSuffix(".mp4").removeSuffix("_merged")
        return try {
            val date = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).parse(stem)
            if (date != null) {
                java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(date)
            } else {
                stem
            }
        } catch (_: Exception) {
            stem
        }
    }

    private fun startSync() {
        handler.post(object : Runnable {
            override fun run() {
                if (data.isEmpty()) {
                    handler.postDelayed(this, 16)
                    return
                }

                val t = videoView.currentPosition + syncOffsetMs
                if (t < lastTime) {
                    currentIndex = 0
                }
                lastTime = t

                while (currentIndex + 1 < data.size && data[currentIndex + 1].time <= t) {
                    currentIndex++
                }

                val current = data[currentIndex]
                val next = if (currentIndex + 1 < data.size) data[currentIndex + 1] else null

                if (next == null || next.time <= current.time) {
                    myView.setQuats(current.q0, current.q1, current.q2, current.q3)
                } else {
                    val alpha = ((t - current.time).toFloat() / (next.time - current.time).toFloat())
                        .coerceIn(0f, 1f)
                    val q0 = current.q0 + (next.q0 - current.q0) * alpha
                    val q1 = current.q1 + (next.q1 - current.q1) * alpha
                    val q2 = current.q2 + (next.q2 - current.q2) * alpha
                    val q3 = current.q3 + (next.q3 - current.q3) * alpha
                    myView.setQuats(q0, q1, q2, q3)
                }

                handler.postDelayed(this, 16)
            }
        })
    }
    fun saveCanvasAsImage(context: Context, r:Int) {
        // 画像のサイズを指定
        val width = 500
        val height = 500

        // Bitmapを作成
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 描画するペイントを設定
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
           // 図形を描画（例として円を描画）
        canvas.drawCircle(width / 2f, height / 2f, r.toFloat()*3F, paint)

        val dir = filesDir
        val directory = File(dir, "temp")

        // temp フォルダが存在しない場合は作成
        if (!directory.exists()) {
            directory.mkdir()
        }

        val pngFile = String.format("%05d.png", r)

        val file = File(directory, pngFile)
        Log.e("kkkkkk:",file.toString())
        // 画像をファイルに保存
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        myView.playMode=false
    }
}
