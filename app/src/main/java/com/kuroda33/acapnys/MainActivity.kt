package com.kuroda33.acapnys

import android.Manifest
//import android.R
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.kuroda33.acapnys.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.times

class MainActivity : AppCompatActivity() , SensorEventListener {
    private val _helper = DatabaseHelper(this@MainActivity)
    private var videoURI: String ="no video"
    private var lastURI: String = ""
    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null

    private var quaternionSensor: Sensor? = null
    val gyroArrayList = ArrayList<String>()
    var recordingFlag:Boolean=false
    // arrayList.add(1)
    // arrayList.add(2)
    // 必要に応じて要素を追加
    // val intArray = arrayList.toIntArray()
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
    // arrayString = intArray.joinToString(","))
    //cq0 = nq0; cq3 = -nq3;
    fun saveData(name:String,data:String){
        val db = _helper.writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("data", data)
        }
        db.insert("headgyrodata", null, values)
        db.close()
    }
    fun getStringArray(name: String): Array<String> {
        val db = _helper.readableDatabase
        val cursor: Cursor = db.query(
            "headgyrodata",
            arrayOf("_id","name","data"),
            "name = ?",
            arrayOf(name),
            null,
            null,
            null
        )
        var stringArray:Array<String> = arrayOf("apple", "banana", "cherry")
        if (cursor.moveToFirst()) {
            val arrayString = cursor.getString(cursor.getColumnIndexOrThrow("data"))
            stringArray = arrayString.split(",").toTypedArray()
        }

        cursor.close()
        db.close()
        return stringArray
    }
    fun getData(name:String):String{
        val db = _helper.readableDatabase
        val cursor: Cursor = db.query(
            "headgyrodata", // テーブル名
            arrayOf("_id", "name", "data"), // 取得するカラム
            "name = ?", // WHERE句
            arrayOf(name), // WHERE句の引数
            null, // GROUP BY句
            null, // HAVING句
            null // ORDER BY句
        )
        var itemData:String=""
        with(cursor) {
            if(moveToFirst()){
                itemData = getString(getColumnIndexOrThrow("data"))
            }
        }
        cursor.close()
        db.close()
        return itemData
    }
    fun deleteData(name:String){
        val db = _helper.writableDatabase
        val selection = "name = ?"
        val selectionArgs = arrayOf(name)

        db.delete("headgyrodata", selection, selectionArgs)
        db.close()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        //   setContentView(R.layout.activity_main)
        videoURI="no video"
        getPara()
        viewBinding.myView.setCamera(cameraNum)

        //     val listView = findViewById<ListView>(R.id.listview)
        //  while(!allPermissionsGranted()) Thread.sleep(100)
        if (true||allPermissionsGranted()) {
            startCamera()
            setListView()

            viewBinding.zoomSeekBar.progress = zoom100
            // Set up the listeners for take photo and video capture buttons
            //  viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
            viewBinding.videoCaptureButton.setOnClickListener {
                captureVideo()
            }
            viewBinding.helpButton.setOnClickListener {
/*
                Log.e("data",getData(lastURI))
                val arrayS=getStringArray(lastURI)
                // if (array != null) {
                Log.e("data_size", arrayS?.size.toString())
                //}

                */
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

            viewBinding.cameraButton.setOnClickListener {
                changeCamera()
            }
            cameraExecutor = Executors.newSingleThreadExecutor()
            viewBinding.zoomSeekBar.setOnSeekBarChangeListener(
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
            sensorManager=getSystemService(SENSOR_SERVICE) as SensorManager
            gravitySensor=sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            rotationVectorSensor=sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)




     /*       sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST
            )*/
            viewBinding.myView.set_rpk_ppk()
            setPreviewSize(cameraNum)
            setButtons(true)
            viewBinding.videoCaptureButton.bringToFront()
            //val permissionText = findViewById<TextView>(R.id.permission)
            //permissionText.translationX(1000f)
            //    viewBinding.myView.alpha=0f
        }else{
            setButtons(false)
            viewBinding.myView.alpha=0.5f
            viewBinding.permission.visibility=View.INVISIBLE//とりあえず削除せず隠しておく。

            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        }

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
            // Log.e(TAG, "Video capture ends with error: $videoURI")
            // if (videoURI == "no video"){
            //     viewBinding.playButton.visibility=View.INVISIBLE
            //     Log.e(TAG, "Video capture no: $videoURI")
            // }else{
            //     viewBinding.playButton.visibility=View.VISIBLE
            //     Log.e(TAG, "Video capture yes: $videoURI")
            // }
            viewBinding.helpButton.visibility=View.VISIBLE
            viewBinding.cameraButton.visibility=View.VISIBLE
            viewBinding.zoomSeekBar.visibility=View.VISIBLE
            viewBinding.helpButton.visibility=View.VISIBLE
            viewBinding.zoomTextRight.visibility=View.VISIBLE
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
            viewBinding.zoomSeekBar.visibility=View.INVISIBLE
            //viewBinding.playButton.visibility=View.INVISIBLE
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
    fun createVideoFile(context: Context): File {
        val sdf = SimpleDateFormat("yyyy_MMdd_HHmm_ss", Locale.getDefault())
        val fileName = "${sdf.format(System.currentTimeMillis())}.mp4"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File(storageDir, fileName)
    }
    fun getAppSpecificAlbumStorageDir(context: Context, albumName: String): File? {
        // Get the videos directory that's inside the app-specific directory on
        // external storage.
        val file = File(context.getExternalFilesDir(
            Environment.DIRECTORY_MOVIES), albumName)
        if (file.mkdirs() == false) {
            Log.e("get MY Directory" ,"Directory not created")
        }
        return file
    }

    private fun setupRecordingSession(): MediaStoreOutputOptions {
        val name = SimpleDateFormat("yyyy-MMdd-HHmm-ss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CapNYS")
        }

        return MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.videoCaptureButton.isEnabled = false
        recordingFlag = false

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
            // if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/aCapNYS")
            // }
        }
        gyroArrayList.clear()
  //      gyroArrayAdd(viewBinding.myView.cq0,viewBinding.myView.cq1,viewBinding.myView.cq2,viewBinding.myView.cq3)
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

                        resetHead()
                        viewBinding.videoCaptureButton.apply {
                            text = "stop_capture"
                            //  text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                        recordingFlag=true
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            recordingFlag=false
                            videoURI=recordEvent.outputResults.outputUri.toString()

                            savePara()
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            //    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                            // viewBinding.playButton.visibility=View.VISIBLE
                            setListView()
                            //  Log.e("newest",videoPathList[0])
                            lastURI = videoPathList[0].substring(videoPathList[0].indexOf(")") + 1)
                            Log.e("newest",lastURI)
                            //var gyroArray = gyroArrayList.toIntArray()
                            saveData(lastURI, gyroArrayList.joinToString(","))
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureButton.apply {
                            text = "start_capture"//getString(R.string.start_capture)
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sensorManager != null) {
            sensorManager.unregisterListener(this)
        }
        cameraExecutor.shutdown()
        _helper.close()
    }

    companion object {
        private const val TAG = "aCapNYS"
        private const val FILENAME_FORMAT = "yyyy-MMdd-HHmm-ss"
        private const val REQUEST_CODE_PERMISSIONS = 5
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                // Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                //  Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
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
                //     finish()
                startCamera()
                setListView()
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
    var resetHeadCount:Int=0
    override fun onSensorChanged(event: SensorEvent) {
        event?.let{
            when(it.sensor.type){
                Sensor.TYPE_GRAVITY ->{
                    if(viewBinding.myView.degreeAtResetHead==-1){
                        val gravityZ=event.values[2]
                        if (gravityZ > 0){
                            if(viewBinding.myView.camera_num != 0)viewBinding.myView.degreeAtResetHead = 1
                            else viewBinding.myView.degreeAtResetHead = 0
                        }else{
                            if(viewBinding.myView.camera_num != 0)viewBinding.myView.degreeAtResetHead = 0
                            else viewBinding.myView.degreeAtResetHead = 1
                        }
                        // Log.e("gravity", String.format("%03f",gravityZ))//event[2])
                    }
                }
                Sensor.TYPE_GAME_ROTATION_VECTOR ->{
                    val nq0 = event.values[3]
                    val nq1 = event.values[0]
                    val nq2 = event.values[1]
                    val nq3 = event.values[2]

                    val currentTime =System.currentTimeMillis()
                    if(currentTime>tempTime+30) {

                        //    Log.e("counter",(currentTime-tempTime).toString())
                        tempTime=currentTime
                        if(resetHeadCount>0){
                            //  if(resetHeadCount==5){
                            //  viewBinding.myView.set_rpk_ppk()
                            // viewBinding.myView.resetHead()
                            //  sensorReset()
                            //    }else{// if(resetHeadCount>20){
                            // else if(resetHeadCount==19)sensorReset()
                            //     else{// if(resetHeadCount>1&&resetHeadCount<5){
                            viewBinding.myView.resetHead()
                            if(gyroArrayList.size>1) {
                                gyroArraySet(0,
                                    viewBinding.myView.cq0,
                                    viewBinding.myView.cq1,
                                    viewBinding.myView.cq2,
                                    viewBinding.myView.cq3
                                )
                                val str=String.format("%03d%03d%03d%03d",cameraNum,cameraNum,cameraNum,cameraNum)
                                gyroArrayList.set(1,str)

                                Log.e("arrayCount", gyroArrayList.size.toString())
                            }
                            // }
                            resetHeadCount -= 1
                        }
                        if(recordingFlag){
                            gyroArrayAdd(nq0,nq1,nq2,nq3)
//                val int0=(128F*(nq0+1.0F)).toInt()//0~256
                            //               val int1=(128F*(nq1+1.0F)).toInt()
                            //              val int2=(128F*(nq2+1.0F)).toInt()
                            //             val int3=(128F*(nq3+1.0F)).toInt()
                            //            val str03=String.format("%03d%03d%03d%03d",int0,int1,int2,int3)
                            //            gyroArrayList.add(str03)
                        }
                        viewBinding.myView.setQuats(nq0, nq1, nq2, nq3)
                }
            }
        }


        }
    }
    fun gyroArrayAdd(n0:Float,n1:Float,n2:Float,n3:Float){
        val int0=(128F*(n0+1.0F)).toInt()//0~256
        val int1=(128F*(n1+1.0F)).toInt()
        val int2=(128F*(n2+1.0F)).toInt()
        val int3=(128F*(n3+1.0F)).toInt()
        val str03=String.format("%03d%03d%03d%03d",int0,int1,int2,int3)
        gyroArrayList.add(str03)
    }
    fun gyroArraySet(cnt:Int,n0:Float,n1:Float,n2:Float,n3:Float){
        val int0=(128F*(n0+1.0F)).toInt()//0~256
        val int1=(128F*(n1+1.0F)).toInt()
        val int2=(128F*(n2+1.0F)).toInt()
        val int3=(128F*(n3+1.0F)).toInt()
        val str03=String.format("%03d%03d%03d%03d",int0,int1,int2,int3)
        gyroArrayList.set(cnt,str03)
    }
    private fun resetHead(){
        resetHeadCount=5
    }
    private fun sensorReset(){
        if (sensorManager != null) {
            sensorManager.unregisterListener(this)
        }
    //    sensorManager.unregisterListener(this)
 /*       sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )*/
        gravitySensor?.also{
                gravity ->
            sensorManager.registerListener(this, gravity,SensorManager.SENSOR_DELAY_FASTEST)
        }
        rotationVectorSensor?.also{
                rotation ->
            sensorManager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_FASTEST)
        }

        viewBinding.myView.initData()
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event != null) {
            when(event.action){
                MotionEvent.ACTION_DOWN ->  resetHead()// sensorReset()
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

        if (!allPermissionsGranted()) {

            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
/*        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )*/
        gravitySensor?.also{
                gravity ->
            sensorManager.registerListener(this, gravity,SensorManager.SENSOR_DELAY_FASTEST)
        }
        rotationVectorSensor?.also{
                rotation ->
            sensorManager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_FASTEST)
        }

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
    val videoPathList = mutableListOf(" ")
    private fun setListView(){
        //} else {
//        data.removeAt(0)
        videoPathList.clear()
        readContent()
        videoPathList.reverse()
        //}
        val lv: ListView = viewBinding.videoListView// findViewById(R.id.video_list_view)
        // val adapter = SimpleAdapter(this, lv,R.layout.layout.customlist, from, to)

        //3)アダプター
        val adapter = ArrayAdapter(this, R.layout.list, videoPathList)
        //val adapter = ArrayAdapter(this, R.layout.simple_spinner_item , data)

        //val adapter1= ArrayAdapter(this, R.layout.//list, data)
        //4)adapterをlistviewにセット
        // val adapter1 = ArrayAdapter(this, R.layout.list, R.id.textView, data)
        lv.adapter =adapter

        lv.setOnItemClickListener { adapterView, view, i, l->

            var str=videoPathList[i].substring(videoPathList[i].indexOf(")")+1)
            var fullPath=onePath.substring(0,onePath.indexOf("CapNYS")+7) + str + ".mp4"
            //    Toast.makeText(this,str,Toast.LENGTH_SHORT).show()
            val strcsv=getData(str)
            val intent = Intent(application, PlayActivity::class.java)
            intent.putExtra("videouri",fullPath)
            intent.putExtra("gyrodata",strcsv)
            startActivity(/* intent = */ intent)
        }
        lv.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { parent, view, i, id ->
                var str = videoPathList[i].substring(videoPathList[i].indexOf(")") + 1)
                var fullPath = onePath.substring(0, onePath.indexOf("CapNYS") + 7) + str + ".mp4"
//                val msg = i.toString() + "番目のアイテムが長押しされました"
                //              Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                showAlertDialog(videoPathList[i], fullPath)
                true
            }
    }

    private fun showAlertDialog(str:String,filePath:String) {
        Log.e("getMyDirectry",getAppSpecificAlbumStorageDir(this,"CapNYS").toString())
        val builder = AlertDialog.Builder(this)
        //  builder.setTitle("確認")
        val mess=str +" / Delete OK?"
        builder.setMessage(mess)

        // ポジティブボタンの設定
        builder.setPositiveButton("YES") { dialog, which ->
            //val filePath = "/path/to/your/file.jpg"

            //  val isDeleted = deleteVideoFile_DB(this,filePath)
            val isDeleted=File(filePath).delete()
            // val isDeleted = contentResolver.delete(Uri.fromFile(File(filePath)), null, null)
            if (isDeleted) {
                //            Toast.makeText(this,filePath + "削除に成功しました",Toast.LENGTH_SHORT).show()
                setListView()
            } else {
                Toast.makeText(this,filePath + "削除に失敗しました",Toast.LENGTH_SHORT).show()
            }
            // はいボタンがクリックされたときの処理
        }

        // ネガティブボタンの設定
        builder.setNegativeButton("NO") { dialog, which ->
            // いいえボタンがクリックされたときの処理
            dialog.dismiss()
        }

        // ダイアログの表示
        builder.show()
    }

    var onePath:String=""//fullPathに戻すために保存
    //@SuppressLint("Range")//"Range"に関連する警告を無視する
    @SuppressLint("Range")
    private fun readContent() {
        val contentResolver = contentResolver
        var cursor: Cursor? = null
        var cnt:Int=0
        // 例外を受け取る
        try {
            cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                if(cursor.getColumnIndex(MediaStore.Video.Media.DATA) > -1){
                    do {
                        onePath = cursor.getString(
                            cursor.getColumnIndex(
                                MediaStore.Video.Media.DATA
                            )
                        )
                        if (onePath.contains("aCapNYS") == true) {
                            val str1 = "aCapNYS"
                            val n = onePath.indexOf(str1)
                            val str2: String = onePath.substring(n + 8, n + 25)
                            cnt += 1
                            videoPathList += "(" + cnt.toString() + ")" + str2
//                        data +=onePath
                            // data += str
                        }
                    } while (cursor.moveToNext())
                }
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
    //arrayString = intArray.joinToString(","))
    //intArray = arrayString.split(",").map { it.toInt() }.toIntArray()

    fun saveIntArray(intArray: IntArray) {
        val db = _helper.writableDatabase

        val contentValues = ContentValues().apply {
            put("data", intArray.joinToString(","))
        }

        db.insert("headgyrodata", null, contentValues)
        db.close()
    }
}




