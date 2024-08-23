package com.kuroda33.acapnys

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class PlayActivity : AppCompatActivity() {

    public lateinit var videoURI: Uri

    private lateinit var videoView: VideoView
    private lateinit var myView:com.kuroda33.acapnys.MyView
  //  private lateinit var timeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        val uri: Uri
        var stringUri = intent.getStringExtra("videouri")
        var csvData = intent.getStringExtra("gyrodata")
        //var stringArray:Array<String> = stringData!!.split(",").toTypedArray()
        val arrayData = csvData.toString().split(",").toTypedArray()
        val arrayCount = arrayData.size
        Log.e("arrayData.count",arrayCount.toString())
        uri = Uri.parse(stringUri)
        videoView = findViewById(R.id.videoView)
        myView = findViewById(R.id.myView)
        //       timeTextView = findViewById(R.id.timeTextView)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(uri)
        //    myView.setCamera(0)
        if (arrayData.size > 2) {
            val camstr = arrayData[1]
            val camNum = camstr.substring(0, 3).toInt()
            Log.e("camera_num", camNum.toString())
            myView.setCamera(camNum)//0:front 1:back

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
            myView.setCamera(0)//0:front
        }
        myView.set_rpk_ppk()
        videoView.start()
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val videoCurrent=videoView.currentPosition
                val videoDuration=videoView.duration
            //    Log.e("Current",videoCurrent.toString())
            //    Log.e("Duration",videoDuration.toString())
            //    Log.e("arrayCount",arrayCount.toString())

                handler.postDelayed(this, 33)
                var current:Int=0
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
                    myView.setQuats(f0, f1, f2, f3)
                }
            }
        }

        // 動画が再生されている間、経過時間を更新
        videoView.setOnPreparedListener {
            handler.post(updateTimeRunnable)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}