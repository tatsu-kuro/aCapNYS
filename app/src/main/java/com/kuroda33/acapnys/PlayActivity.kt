package com.kuroda33.acapnys


import android.content.Context
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
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController

import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PlayActivity : AppCompatActivity() {



    private lateinit var videoView: VideoView
    private lateinit var myView:MyView
    //  private lateinit var timeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    fun cropVideo(inputFilePath: String, outputFilePath: String) {
        val cropCommand = "-i $inputFilePath -vf crop=in_w/4:in_h/4:0:0 -c:a copy $outputFilePath"
        FFmpegKit.execute(cropCommand).apply {
            if (returnCode.isSuccess) {
                println("command exe Cropping successful!")
            } else {
                println("command exe Cropping failed with state ${state} and rc ${returnCode}.")
            }
        }
    }
    /*
    fun resizeVideo(inputPath: String, outputPath: String) {
        // 元の解像度が1920x1080の場合、1/4のサイズに設定
        val command = "-i $inputPath -vf scale=iw/10:ih/10 $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (returnCode.isSuccess) {
                println("Command execution resize completed successfully.")
            } else {
                println("Command execution resixe failed with returnCode=$returnCode.")
            }
        }
    }*/
    fun overlayVideos(inputPath1: String, inputPath2: String, outputPath: String) {
        val command = "-i $inputPath1 -i $inputPath2 -filter_complex \"overlay=x=(W-w)/2:y=0\" -c:a copy $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            val output = session.output
            val logs = session.allLogsAsString
            if (returnCode.isSuccess) {
                println("Overlay completed successfully.")
            } else {
                println("Overlay failed with returnCode=$returnCode.")
                println("Output: $output")
                println("Logs: $logs")
            }
        }
    }

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
    fun deleteVideoFile(filePath: String) {
         val file= File(filePath)
        if (file.exists()){
            // FFmpegコマンドを実行（例：動画の情報を取得）
            val command = "-i $filePath -c copy -an output.mp4"
            val session = FFmpegKit.execute(command)

            if (session.returnCode.isSuccess) {
                // FFmpegコマンドが成功した場合、ファイルを削除
                val success = file.delete()
                if (success) {
                    println("deleteファイルが削除されました。")
                } else {
                    println("ファイルの削除に失敗しました。")
                }
            } else {
                println("FFmpegコマンドの実行に失敗しました。")
            }
        } else {
            println("ファイルが存在しません。")
        }
    }
    fun createVideoFromImages(imageDirectory: File, outputPath: String) {
        val command = "-framerate 30 -i ${imageDirectory.path}/frame_%d.png -c:v libx264 -pix_fmt yuv420p $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (returnCode.isSuccess) {
                println("Command execution completed successfully.")
            } else {
                println("Command execution failed with returnCode=$returnCode.")
            }
        }
    }
    fun show() {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        val uri: Uri
        //val path_temp = intent.getStringExtra("videouri")
        val path = intent.getStringExtra("videouri")
         val csvData = intent.getStringExtra("gyrodata")
       // val path_temp = intent.getStringExtra("videouri")
        //   val Path = path_temp!!.replace(".mp4","_crop.mp4")//crop.mp4を見るとき
        //   val path = path_temp!!.replace(".mp4", "_overlay.mp4")//overlay.mp4を見るとき
        //   cropVideo(path!!, cropPath!!)//成功、使う可能性はないができた
      //  overlayVideos(path,cropPath,overlayPath)//成功

        /////////////////////////
        val dir=getOutputDirectory()
        val directory = File(dir, "temp")
        // temp フォルダが存在しない場合は作成
        if (!directory.exists()) {
            directory.mkdir()
        }
        //val directory = filesDir
        val pngFile = String.format("%05d.png", 5)
        val file = File(directory, pngFile)
        Log.e("kkkkkkexist:", file.toString())
        val imageView:ImageView=findViewById(R.id.imageView)
        // 画像ファイルが存在する場合、ImageViewに表示
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    Log.e("kkkkkkkk", "Bitmap is null")
                }
            } catch (e: Exception) {
                Log.e("kkkkkkkkk", "Error setting image resource", e)
            }
        } else {
            Log.e("kkkkkkkkk", "File does not exist")
        }

        val sendButton: Button = findViewById(R.id.sendBtton)
        sendButton.setOnClickListener {
         //   saveCanvasAsImage(this, 10)
            for(r in 0..100) {
                saveCanvasAsImage(this, r)
            }
            val videoUri=Uri.parse(path)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "video/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)//Uri.fromFile(videoUri))
         //   shareIntent.putExtra(Intent.EXTRA_SUBJECT, "動画を共有します")
            //      intentMode=1
            //      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            startActivity(Intent.createChooser(shareIntent, "share"))
        }


        //var stringArray:Array<String> = stringData!!.split(",").toTypedArray()
        val arrayData = csvData.toString().split(",").toTypedArray()
        val arrayCount = arrayData.size
        Log.e("arrayData.count",arrayCount.toString())
        uri = Uri.parse(path)
        videoView = findViewById(R.id.videoView)
        myView = findViewById(R.id.myView)
        myView.playMode=true
        //       timeTextView = findViewById(R.id.timeTextView)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(uri)
        //    myView.setCamera(0)
        if (arrayData.size > 2) {
            val camstr = arrayData[1]
            myView.cameraNum = camstr.substring(0, 3).toInt()
            myView.gravityZ=camstr.substring(3,6).toInt()
            //if(gravityz<0){
            //    if(camNum==0)camNum=1
            //    else camNum=0
            // }
            Log.e("camera_num", String.format("%3d,%d3",myView.cameraNum,myView.gravityZ))
            myView.setCamera(myView.cameraNum)//0:front 1:back

            val str03 = arrayData[0]
            val str0 = str03.substring(0, 3)
            val str1 = str03.substring(3, 6)
            val str2 = str03.substring(6, 9)
            val str3 = str03.substring(9, 12)
            myView.cq0 = (str0.toFloat() - 128F) / 128F
            myView.cq1 = (str1.toFloat() - 128F) / 128F
            myView.cq2 = (str2.toFloat() - 128F) / 128F
            myView.cq3 = (str3.toFloat() - 128F) / 128F
        } else {
            myView.alpha=0f
//            myView.setCamera(0)//0:front
        }
        myView.setRpkPpk()
        videoView.start()
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val videoCurrent=videoView.currentPosition
                val videoDuration=videoView.duration
                //    Log.e("Current",videoCurrent.toString())
                //    Log.e("Duration",videoDuration.toString())
                //    Log.e("arrayCount",arrayCount.toString())

                handler.postDelayed(this, 33)
                var current=0
                if(videoDuration>0) {
                    current = arrayCount * videoCurrent / videoDuration
                }
                if (arrayCount>1 && current < arrayCount){// && current>1) {//0: set cq0-3
                    if(current<2)current=2
                    val str03 = arrayData[current]
                    val str0 = str03.substring(0, 3)
                    val str1 = str03.substring(3, 6)
                    val str2 = str03.substring(6, 9)
                    val str3 = str03.substring(9, 12)
                    val f0 = (str0.toFloat() - 128F) / 128F
                    val f1 = (str1.toFloat() - 128F) / 128F
                    val f2 = (str2.toFloat() - 128F) / 128F
                    val f3 = (str3.toFloat() - 128F) / 128F
                    myView.setQuats(f0, f1, f2, f3)//これはないとだめだが、数値は何でもかまわん、以下の4個が大事、わけわからんが出来た
                    myView.mnq0=f0
                    myView.mnq1=f1
                    myView.mnq2=f2
                    myView.mnq3=f3
                }
            }
        }

        // 動画が再生されている間、経過時間を更新
        videoView.setOnPreparedListener {
            handler.post(updateTimeRunnable)
        }

    }
    //下記ではアプリ固有のexternal storage 外部ストレージが得られる。
    //filesDirではアプリ固有のinternal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
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

        val dir=getOutputDirectory()
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