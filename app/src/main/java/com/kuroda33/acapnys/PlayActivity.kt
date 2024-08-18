package com.kuroda33.acapnys

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class PlayActivity : AppCompatActivity() {

    public lateinit var videoURI: Uri
   /* private lateinit var videoView: VideoView
    private lateinit var timeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_play)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val uri: Uri
        var stringUri = intent.getStringExtra("videouri")
        uri = Uri.parse(stringUri)
        videoView.setVideoURI(uri);
        videoView.setMediaController(MediaController(/* context = */ this));
        videoView.start()
  //      videoView.currentPosition

        // 経過時間を更新するRunnable
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val currentPosition = videoView.currentPosition / 1000
                val minutes = currentPosition / 60
                val seconds = currentPosition % 60
                timeTextView.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }*/


        private lateinit var videoView: VideoView
        private lateinit var myView:com.kuroda33.acapnys.MyView
        private lateinit var timeTextView: TextView
        private val handler = Handler(Looper.getMainLooper())

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_play)
            val uri: Uri
            var stringUri = intent.getStringExtra("videouri")
            uri = Uri.parse(stringUri)
            videoView = findViewById(R.id.videoView)
            myView=findViewById(R.id.myView)
            timeTextView = findViewById(R.id.timeTextView)
            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.setVideoURI(uri)
            // 動画のパスを設定
        //    videoView.setVideoPath("android.resource://" + packageName + "/" + R.raw.sample_video)
            videoView.start()
            myView.setCamera(1)
            myView.set_rpk_ppk()
            myView.initData()
            // 経過時間を更新するRunnable
            val updateTimeRunnable = object : Runnable {
                override fun run() {
                    val currentPosition = videoView.currentPosition / 1000
                    val minutes = currentPosition / 60
                    val seconds = currentPosition % 60
                    timeTextView.text = String.format("%02d:%02d", minutes, seconds)
                    handler.postDelayed(this, 1000)

                    myView.setQuats(0.99F, 0F,0F,0F)
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