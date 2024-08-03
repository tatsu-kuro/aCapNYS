package com.kuroda33.acapnys

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    }
}