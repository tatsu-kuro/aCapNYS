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

//import android.R
//import kotlinx.coroutines.flow.internal.NoOpContinuation.context
//import android.content.Context
import android.Manifest
import android.annotation.SuppressLint
//import android.app.RecoverableSecurityException
//import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
//import android.net.Uri
import android.net.Uri.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
//import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SeekBar
import android.widget.Toast
//import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
//import androidx.camera.view.video.OutputFileOptions
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.core.content.PermissionChecker
//import androidx.lifecycle.LifecycleOwner
import com.kuroda33.acapnys.databinding.ActivityMainBinding
//import kotlinx.coroutines.DefaultExecutor.thread
import kotlinx.coroutines.delay
import java.io.File
//import java.io.IOException
//import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context


//typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {//}, SensorEventListener{
    private var videoURI: String ="no video"
 //  private lateinit var sensorManager: SensorManager
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
     //   setContentView(R.layout.activity_main)
        videoURI="no video"
        getPara()
    //    viewBinding.myView.setCamera(cameraNum)

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
          /*  viewBinding.playButton.setOnClickListener {
                val intent =
                    Intent(/* packageContext = */ application,/* cls = */ PlayActivity::class.java)
                if (videoURI != "no video") {
                    Toast.makeText(baseContext, videoURI, Toast.LENGTH_SHORT).show()
                    intent.putExtra("videouri", videoURI)
                    startActivity(/* intent = */ intent)
                }
            }*/
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

     /*       sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST
            )*/
           // viewBinding.myView.set_rpk_ppk()
            setPreviewSize(cameraNum)
            setButtons(true)
            viewBinding.videoCaptureButton.bringToFront()
            //val permissionText = findViewById<TextView>(R.id.permission)
            //permissionText.translationX(1000f)
        //    viewBinding.myView.alpha=0f
        }else{
            setButtons(false)
          //  viewBinding.myView.alpha=0f
            viewBinding.permission.visibility=View.INVISIBLE//とりあえず削除せず隠しておく。
           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val Environment =
                    Intent().setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        .setData(parse("package:$packageName"))

                if (Environment.resolveActivity(packageManager) != null) {
                    startActivity(Environment)
                }
                  if (allPermissionsGranted()) {
            }*/
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)


            //   requestPermissionLauncher.launch(
              //     Manifest.permission.READ_EXTERNAL_STORAGE
             // )
             //   ActivityCompat.requestPermissions(
              //      this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        }
        //     setListView()*/
    }
 /*   private fun playVideo(){
        val path = intent.getStringExtra("path")
        videoView = findViewById<View>(R.id.videoView)
        videoView.setVideoPath(path)

        videoView.setOnPreparedListener(OnPreparedListener { mediaPlayer ->
            if (mediaPlayer.videoWidth > mediaPlayer.videoHeight) {
                // 横表示にする
                this@Mp4Activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            } else {
                // 縦表示にする
                this@Mp4Activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            }
            videoView.start()
        })
    }*/
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
         //   viewBinding.myView.alpha=1f
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
            //    viewBinding.myView.alpha=0f
                viewBinding.viewFinder.alpha=0f
                val windowAttributes = window.attributes
                windowAttributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                window.attributes = windowAttributes
            }else{
              //  viewBinding.myView.alpha=1f
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
    // Implements VideoCapture use case, including start and stop capturing.
   // @RequiresApi(Build.VERSION_CODES.O)
    /*fun startVideoCapture(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val recorder = Recorder.Builder().build()
            val videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview, videoCapture)

            val videoFile = createVideoFile(context)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(videoFile).build()

            videoCapture.output.prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            // 録画開始時の処理
                        }
                        is VideoRecordEvent.Finalize -> {
                            // 録画終了時の処理
                        }
                    }
                }
        }, ContextCompat.getMainExecutor(context))
    }*/
    /*
        // 新しい録画セッションの設定
        val outputOptions = setupRecordingSession()
        recording = videoCapture.output.prepareRecording(this, outputOptions).start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            handleVideoCaptureEvent(recordEvent)
        }
    }
    */
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
                            text = "stop_capture"
                            //  text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            videoURI=recordEvent.outputResults.outputUri.toString()
                            savePara()
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            //    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                            // viewBinding.playButton.visibility=View.VISIBLE
                            setListView()
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
/*
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
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        // create and start a new recording session
/*        Log.e("getMyDirectry",getAppSpecificAlbumStorageDir(this,"CapNYS").toString())
//        /storage/emulated/0/Android/data/com.kuroda33.acapnys/files/Movies/CapNYS
        val contentValues1 = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val outputOptions = OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues1
        ).build()

*/
        val videoFile = createVideoFile(this)
        val outputOptions = ImageCapture.OutputFileOptions.androidx.compose.foundation.layout.Box {
            builder(videoFile)
        }.build()// builder(videoFile).build()// .Builder(videoFile).build()

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/aCapNYS")
//                  put(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/aCapNYS")
            }
        }
        Log.e("nan***********",outputOptions.toString())
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver,MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        Log.e("nan***********",mediaStoreOutputOptions.toString())
        val videoFile = createVideoFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(videoFile).build()

        videoCapture.output.prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // 録画開始時の処理
                    }
                    is VideoRecordEvent.Finalize -> {
                        // 録画終了時の処理
                    }
                }
            }
    }, ContextCompat.getMainExecutor(context))



    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH,"Android/data/com.kuroda33.acapnys/files/Movies/CapNYS")// "Movies/aCapNYS")
//                  put(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/aCapNYS")
        }
    }
    Log.e("nan***********",contentValues.toString())
    val mediaStoreOutputOptions = MediaStoreOutputOptions
        .Builder(contentResolver,MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
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
                            text = "stop_capture"
                          //  text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            videoURI=recordEvent.outputResults.outputUri.toString()
                            savePara()
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                        //    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                           // viewBinding.playButton.visibility=View.VISIBLE
                            setListView()
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
*/
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
     //   sensorManager.unregisterListener(this)
        //}
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "aCapNYS"
        private const val FILENAME_FORMAT = "yyyy-MMdd-HHmm-ss"
        private const val REQUEST_CODE_PERMISSIONS = 5
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
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
     //   viewBinding.myView.setCamera(cameraNum)
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
  //      sensorReset()
        setPreviewSize(cameraNum)
    }

    private var tempTime:Long = 0
  /*  override fun onSensorChanged(event: SensorEvent) {
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
    }*/
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event != null) {
            when(event.action){
     //           MotionEvent.ACTION_DOWN -> sensorReset()
            }
        }

        //再描画を実行させる呪文
        //   Log.e("kdiidiid","motion touch")
        return super.onTouchEvent(event)
    }
    //センサの精度が変更されたときに呼ばれる
   // override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
   // }

    override fun onResume() {
        super.onResume()

        if (!allPermissionsGranted()) {

            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        /*     sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
             sensorManager.registerListener(
                 this,
                 sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
                 SensorManager.SENSOR_DELAY_FASTEST
             )*/
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
    //    sensorManager.unregisterListener(this)
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

            val intent = Intent(application, PlayActivity::class.java)
            intent.putExtra("videouri",fullPath)
            startActivity(/* intent = */ intent)

        }
        lv.onItemLongClickListener =
            OnItemLongClickListener { parent, view, i, id ->
                var str=videoPathList[i].substring(videoPathList[i].indexOf(")")+1)
                var fullPath=onePath.substring(0,onePath.indexOf("CapNYS")+7) + str + ".mp4"
//                val msg = i.toString() + "番目のアイテムが長押しされました"
  //              Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                showAlertDialog(videoPathList[i],fullPath)
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
    /*
    fun deleteVideoFile_DB(context: Context, filePath: String): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.MediaColumns.DATA + "=?"
        val selectionArgs = arrayOf(filePath)
        return try {
            val rowsDeleted = contentResolver.delete(uri, selection, selectionArgs)
            if (rowsDeleted > 0) {
                // メディアストアを更新
                context.contentResolver.notifyChange(uri, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
*/
    var onePath:String=""//fullPathに戻すために保存
    //@SuppressLint("Range")//"Range"に関連する警告を無視する
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
}



