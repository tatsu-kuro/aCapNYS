package com.kuroda33.acapnys

import com.kuroda33.acapnys.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageButton
import android.widget.TextView
import java.util.Locale

class How2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how2)
        val helpText = findViewById<TextView>(R.id.textView)
        val lang= Locale.getDefault()
        if(lang.toString() == "ja_JP") {
            helpText.setText(R.string.large_text)
        }else{
            helpText.setText(R.string.eng_text)
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnGyro).setOnClickListener {
            startActivity(Intent(this, GyroActivity::class.java))
        }
    }
}
