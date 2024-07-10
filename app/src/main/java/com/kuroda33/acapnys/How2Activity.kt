package com.kuroda33.acapnys

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class How2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how2)
    //    val exitBtn=findViewById<Button>(R.id.exitButton)
    //    val sendBtn=findViewById<Button>(R.id.sendButton)
    //    sendBtn.setOnClickListener{
    //        val intent = Intent(/* packageContext = */ application,/* cls = */ GyroActivity::class.java)
    //        startActivity(/* intent = */ intent)
    //    }
    //    exitBtn.setOnClickListener(View.OnClickListener {
            //Log.d(TAG, "surfaceDestroyed...")
    //        finish()
    //    })
    }
   /* override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }*/
}