package com.kuroda33.acapnys

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class PlayActivity : AppCompatActivity() {

    public lateinit var videoURI: Uri
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

    }
}