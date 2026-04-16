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
            helpText.text = buildJapaneseHelp()
        }else{
            helpText.text = buildEnglishHelp()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnGyro).setOnClickListener {
            startActivity(Intent(this, GyroActivity::class.java))
        }
    }

    private fun buildJapaneseHelp(): String = """
        1. 上写真のような装具でスマートホンを眼前に固定します。

        2. トップ画面：録画
        ・右上：最後の録画のサムネールです。タップすると再生します。
        ・サムネールの下：List、録画した動画の一覧を示します。
        ・右下：複数のバックカメラが利用可能の場合、その切り替えです。
        ・record ボタンを押すと録画開始します。
        ・もう一度 record ボタンを押すと録画終了します。

        3. 一覧
        ・録画一覧は録画時刻順に並びます。
        ・[merged] が付いている項目は合成済み動画です。
        ・項目をタップすると再生します。
        ・長押しすると削除できます。

        4. 再生
        ・再生画面では動画とジャイロ描画（頭部回転）を確認できます。
        ・save ボタンで動画とジャイロを合成します。
        ・mail ボタンで動画をメール送信できます。

        5. Gyro（このページの左下ボタン）
        ・Gyro 画面ではジャイロデータの送信設定を行います。
        ・このデータは Windows ソフト CapNYS で受け取れます。
        ・CapNYS の詳細は https://kuroda33.com/jibika です。
        
        
        
    """.trimIndent()

    private fun buildEnglishHelp(): String = """
        1. Fix the smartphone in front of your eyes using a brace like the one shown above.

        2. Top screen: Recording
        ・Upper right: This is the thumbnail of the latest recording. Tap it to play.
        ・Below the thumbnail: List, which shows the recorded videos.
        ・Lower right: Camera switching when multiple back cameras are available.
        ・Press the record button to start recording.
        ・Press the record button again to stop recording.

        3. List
        ・The recording list is sorted by recording time.
        ・Items marked with [merged] are already composited videos.
        ・Tap an item to play it.
        ・Long press to delete it.

        4. Playback
        ・You can review the video and gyro overlay together.
        ・Tap save to merge the video and gyro overlay.
        ・Tap mail to send the video by email.

        5. Gyro (lower-left button on this page)
        ・The Gyro screen is for gyro data transfer settings.
        ・This data can be received by the Windows app CapNYS.
        ・For details about CapNYS, visit https://kuroda33.com/jibika.
        
        
        
    """.trimIndent()
}
