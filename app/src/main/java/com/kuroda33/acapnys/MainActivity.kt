

package com.kuroda33.acapnys
//import androidx.camera.core.ImageCapture
//import com.kuroda33.databinding.ActivityMainBinding
//import androidx.camera.core.ImageCaptureException
//import android.graphics.Color
//import android.graphics.ColorSpace.Rgb
//import android.net.Uri
//import android.widget.MediaController
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.view.LifecycleCameraController
//import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//import java.net.URI
//import java.nio.ByteBuffer
//import android.graphics.Color
//import android.util.DisplayMetrics
//import android.view.WindowInsetsController

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DateTimePatternGenerator.PatternInfo.OK
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.kuroda33.acapnys.databinding.ActivityMainBinding
import java.security.AccessController.getContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() , SensorEventListener{
    private var videoURI: String ="no video"
    private lateinit var sensorManager: SensorManager
//    private var quaternionSensor: Sensor? = null

    private lateinit var viewBinding: ActivityMainBinding

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService
    //    private lateinit var cameraController: LifecycleCameraController
    private var focusChangedInitFlag:Boolean=true
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (focusChangedInitFlag) {
            focusChangedInitFlag = false
            return
        }
        getPara()
        //       setPreviewSize(cameraNum)
        val cameraController = camera!!.cameraControl
        cameraController.setLinearZoom(zoom100 / 100f)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        // val width=viewBinding.root.maxWidth
        videoURI="no video"
        getPara()
        viewBinding.myView.setCamera(cameraNum)
        // Request camera permissions
        //   viewBinding.post {
        //       Log.i("", "width:" + binding.view.width + ", height:" + binding.view.height)
        //   }
        if (allPermissionsGranted()) {
            startCamera()
            setListView()

            seekBar.progress = zoom100
            // Set up the listeners for take photo and video capture buttons
            //  viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
            viewBinding.videoCaptureButton.setOnClickListener {
                captureVideo()
            }
            viewBinding.helpButton.setOnClickListener {
                val intent =
                    Intent(/* packageContext = */ application,/* cls = */ How2Activity::class.java)
                startActivity(/* intent = */ intent)
            }
            viewBinding.gyroButton.setOnClickListener {
                //         val intent = Intent(/* packageContext = */ application,/* cls = */ GridButtons::class.java)
                //         startActivity(/* intent = */ intent)
                val intent =
                    Intent(/* packageContext = */ application,/* cls = */ GyroActivity::class.java)
                startActivity(/* intent = */ intent)
            }
            viewBinding.playButton.setOnClickListener {
                val intent =
                    Intent(/* packageContext = */ application,/* cls = */ PlayActivity::class.java)
                if (videoURI != "no video") {
                    intent.putExtra("videouri", videoURI)
                    startActivity(/* intent = */ intent)
                }
            }
            viewBinding.cameraButton.setOnClickListener {
                changeCamera()
            }
            cameraExecutor = Executors.newSingleThreadExecutor()
            viewBinding.seekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    @SuppressLint("RestrictedApi")
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            zoom100 = progress
                            savePara()
                            val cameraController = camera!!.cameraControl
                            cameraController.setLinearZoom(zoom100 / 100f)
//                        startCamera()//   Log.d("kdkdkdkdkdk","$progress")
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST
            )
            viewBinding.myView.set_rpk_ppk()
            setPreviewSize(cameraNum)
            setButtons(true)
            viewBinding.videoCaptureButton.bringToFront()
            //val permissionText = findViewById<TextView>(R.id.permission)
            //permissionText.translationX(1000f)
        }else{
            setButtons(false)
            viewBinding.myView.alpha=0f
            viewBinding.permission.visibility=View.VISIBLE



            /*   val builder = AlertDialog.Builder(this)
                    builder.setTitle("title")
                   builder.setMessage("メッセージ")
                   builder.setPositiveButton(OK, null)
                   builder.show()*/
            //  AlertDialog.show(Test02_01.this, "Alert Test",
            //    "Hello, This is Alert Dialog.", "ok", false);
            //android.os.Process.killProcess(android.os.Process.myPid())
            //   requestPermissionLauncher.launch(
            //       Manifest.permission.READ_EXTERNAL_STORAGE
            //  )
            //    ActivityCompat.requestPermissions(
            //        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        //     setListView()
    }
    private fun setNavigationBar(flag:Boolean) {
        if (!flag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.decorView.windowInsetsController?.apply {
                    // ナビゲーションバーを非表示にする
                    hide(WindowInsets.Type.navigationBars())
                    // スワイプで一時的に表示する動作を設定
                    //systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    // systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            } else {
                // API 29以下の場合
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.decorView.windowInsetsController?.apply {
                    // ナビゲーションバーを表示にする
                    show(WindowInsets.Type.navigationBars())
//                    hide(WindowInsets.Type.navigationBars())
                    // スワイプで一時的に表示する動作を設定
                    //  systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // API 29以下の場合
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }

    private fun setPreviewSize(cameraN:Int){
        if(cameraN==0) {
            val size = Rect()
            window.decorView.getWindowVisibleDisplayFrame(size)
            val width=size.width()
            val height=size.height()

            viewBinding.viewFinder.scaleX = 0.2f
            viewBinding.viewFinder.scaleY = 0.2f
            if(width>height) {
                viewBinding.viewFinder.translationX = (-0.4 * width).toFloat() + 8
            }else{
                viewBinding.viewFinder.translationX = (-0.4 * height).toFloat() + 8
            }
        }else{
            viewBinding.viewFinder.scaleX = 1.0f
            viewBinding.viewFinder.scaleY = 1.0f
            viewBinding.viewFinder.translationX = 0f
        }
    }

    private fun setButtons(on:Boolean){
        if(on){
            Log.e(TAG, "Video capture ends with error: $videoURI")
            if (videoURI == "no video"){
                viewBinding.playButton.visibility=View.INVISIBLE
                Log.e(TAG, "Video capture no: $videoURI")
            }else{
                viewBinding.playButton.visibility=View.VISIBLE
                Log.e(TAG, "Video capture yes: $videoURI")
            }
            viewBinding.helpButton.visibility=View.VISIBLE
            viewBinding.cameraButton.visibility=View.VISIBLE
            viewBinding.seekBar.visibility=View.VISIBLE
            viewBinding.helpButton.visibility=View.VISIBLE
            viewBinding.zoomTextRight.visibility=View.INVISIBLE
            viewBinding.zoomTextLeft.visibility=View.VISIBLE
            viewBinding.gyroButton.visibility=View.VISIBLE
            viewBinding.myView.alpha=1f
            viewBinding.viewFinder.alpha=1f
            viewBinding.videoCaptureButton.alpha=0.1f
            viewBinding.permission.visibility=View.INVISIBLE
            viewBinding.videoListView.visibility=View.VISIBLE
            val windowAttributes = window.attributes
            windowAttributes.screenBrightness =
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = windowAttributes
        }else{
            viewBinding.helpButton.visibility=View.INVISIBLE
            viewBinding.cameraButton.visibility=View.INVISIBLE
            viewBinding.seekBar.visibility=View.INVISIBLE
            viewBinding.playButton.visibility=View.INVISIBLE
            viewBinding.helpButton.visibility=View.INVISIBLE
            viewBinding.zoomTextLeft.visibility=View.INVISIBLE
            viewBinding.zoomTextRight.visibility=View.INVISIBLE
            viewBinding.gyroButton.visibility=View.INVISIBLE
            viewBinding.videoCaptureButton.alpha=0.015f
            viewBinding.permission.visibility=View.INVISIBLE
            viewBinding.videoListView.visibility=View.INVISIBLE
            if(cameraNum==0){
                viewBinding.myView.alpha=0f
                viewBinding.viewFinder.alpha=0f
                val windowAttributes = window.attributes
                windowAttributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                window.attributes = windowAttributes
            }else{
                viewBinding.myView.alpha=1f
                viewBinding.viewFinder.alpha=1f
            }
        }
    }

    // Implements VideoCapture use case, including start and stop capturing.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // ナビゲーションバー表示
            setNavigationBar(true)
            curRecording.stop()
            recording = null
            setButtons(true)
            return
        }
        setButtons(false)
        setNavigationBar(false)
        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            videoURI=recordEvent.outputResults.outputUri.toString()
                            savePara()
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                            viewBinding.playButton.visibility=View.VISIBLE
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    private fun startCamera() {
        getPara()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)


            //  imageCapture = ImageCapture.Builder().build()
            /*
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                                    Log.d(TAG, "Average luminosity: $luma")
                                })
                            }
            */
            // Select back camera as a default
            var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            if(cameraNum==1) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider
                    .bindToLifecycle(this, cameraSelector, preview,videoCapture)//imageAnalyzer＆videocapture並存不能？
                val cameraController = camera!!.cameraControl
                cameraController.setLinearZoom(zoom100/100f)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    /*
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
  //      val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }*/
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        //  if (sensorManager != null) {
        sensorManager.unregisterListener(this)
        //}
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "aCapNYS"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            readContent()
        } else {
            // それでも拒否された時の対応
            val toast = Toast.makeText(
                this,
                "これ以上なにもできません", Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private var cameraNum:Int=0
    var zoom100:Int=0

    private fun savePara(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putInt("cameraNum",cameraNum)
        editor.putInt("zoom100",zoom100)
        editor.putString("videoURI",videoURI)
        editor.apply()
        viewBinding.myView.setCamera(cameraNum)
    }
    private fun getPara(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // val editor = sharedPreferences.edit()
        cameraNum = sharedPreferences.getInt("cameraNum",1)
        zoom100=sharedPreferences.getInt("zoom100",10)
        videoURI= sharedPreferences.getString("videoURI","no video").toString()
    }
    private var camera: Camera?= null
    // private var imageCapture: ImageCapture?= null
    //  private var videoCapture: VideoCapture?= null
    private fun changeCamera(){
        getPara()
        cameraNum = if (cameraNum == 1) {
            0//front
        }else{
            1//back
        }
        savePara()
        startCamera()
        sensorReset()
        setPreviewSize(cameraNum)
    }

    private var tempTime:Long = 0
    override fun onSensorChanged(event: SensorEvent) {
        val nq0 = event.values[3]
        val nq1 = event.values[0]
        val nq2 = event.values[1]
        val nq3 = event.values[2]
        val currentTime =System.currentTimeMillis()
        if(currentTime>tempTime+30) {
            tempTime=currentTime
            viewBinding.myView.setQuats(nq0, nq1, nq2, nq3)
            //   val str: String = "x=" + nq0 + "\n" + "y=" + nq1 + "\n" + "z=" + nq2 + "\n" + "w=" + nq3
            //    Log.e("tetetete",str)
        }
    }
    private fun sensorReset(){
        sensorManager.unregisterListener(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        viewBinding.myView.initData()
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event != null) {
            when(event.action){
                MotionEvent.ACTION_DOWN -> sensorReset()

            }
        }

        //再描画を実行させる呪文
        //   Log.e("kdiidiid","motion touch")
        return super.onTouchEvent(event)
    }
    //センサの精度が変更されたときに呼ばれる
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onResume() {
        super.onResume()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        //リスナーとセンサーオブジェクトを渡す
        //第一引数はインターフェースを継承したクラス、今回はthis
        //第二引数は取得したセンサーオブジェクト
        //第三引数は更新頻度 UIはUI表示向き、FASTはできるだけ早く、GAMEはゲーム向き
        //sensorManager.registerListener(this, quaternionSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    //アクティビティが閉じられたときにリスナーを解除する
    override fun onPause() {
        super.onPause()
        //リスナーを解除しないとバックグラウンドにいるとき常にコールバックされ続ける
        sensorManager.unregisterListener(this)
    }
    val data = mutableListOf(" ")
    private fun setListView(){
        //} else {
        data.removeAt(0)
        readContent()
        data.reverse()
        //}
        val lv: ListView =findViewById(R.id.video_list_view)

        //3)アダプター
        val adapter= ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            data
        )
        //4)adapterをlistviewにセット
        lv.adapter =adapter
        //5)クリックしてトースト表示
        lv.setOnItemClickListener { adapterView, view, i, l->
            Toast.makeText(this,data[i],Toast.LENGTH_SHORT).show()
        }
    }
    @SuppressLint("Range")
    private fun readContent() {
        val contentResolver = contentResolver
        var cursor: Cursor? = null

        // 例外を受け取る
        try {
            cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {

                    var str = cursor.getString(
                        cursor.getColumnIndex(
                            MediaStore.Video.Media.DATA
                        )
                    )

                    if (str.contains("aCapNYS")==true) {
                        data += str
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }
}

