package com.kuroda33.acapnys

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentResolver
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.os.ParcelFileDescriptor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import com.kuroda33.acapnys.databinding.ActivityMainBinding
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.math.abs
class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val PREFS_NAME = "aCapNYS2_camera_prefs"
        private const val KEY_FOCUS_PROGRESS = "focus_progress"
        private const val KEY_ZOOM_PROGRESS = "zoom_progress"
        private const val FAR_FOCUS_METERS = 0.3f
        private const val ZOOM_RANGE_FRACTION = 1f / 5f
    }

    private data class CameraChoice(val logicalId: String, val physicalId: String?)

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var sensorManager: SensorManager

    private var gravitySensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null

    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var requestBuilder: CaptureRequest.Builder? = null
    private var recordingUri: android.net.Uri? = null
    private var recordingPfd: ParcelFileDescriptor? = null
    private var backCameraChoices: List<CameraChoice> = emptyList()
    private var backCameraIndex = 0

    private var previewSize: Size = Size(1280, 960)
    private var videoSize: Size = Size(1280, 960)
    private var currentZoomRatio = 1f

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val uiHandler = Handler(android.os.Looper.getMainLooper())

    private var lastVideoUriString: String = ""
    private var currentName: String = ""
    private var startTime: Long = 0L
    private val csvBuffer = StringBuilder()
    private var recordingFlag = false
    private var isRecording = false
    private var cameraToastRunnable: Runnable? = null
    private var requestReset = false

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    private val REQUEST_CODE_PERMISSIONS = 10

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            if (cameraDevice == camera) cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            if (cameraDevice == camera) cameraDevice = null
            runOnUiThread { showControlsAfterStop() }
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread != null) return
        backgroundThread = HandlerThread("Camera2Background").also {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    private fun startCamera() {
        if (!allPermissionsGranted()) return
        if (!this::cameraManager.isInitialized) {
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        }
        if (viewBinding.viewFinder.isAvailable) {
            openCamera()
        } else {
            viewBinding.viewFinder.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun chooseCameraId(): String? {
        return try {
            if (backCameraChoices.isEmpty()) refreshBackCameraChoices()
            backCameraChoices.getOrNull(backCameraIndex)?.logicalId
                ?: backCameraChoices.firstOrNull()?.logicalId
                ?: cameraManager.cameraIdList.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun refreshBackCameraChoices() {
        val choices = mutableListOf<CameraChoice>()

        cameraManager.cameraIdList.forEach { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) return@forEach

            val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                chars.physicalCameraIds.toList()
            } else {
                emptyList()
            }

            if (physicalIds.isNotEmpty()) {
                physicalIds.forEach { physicalId ->
                    choices.add(CameraChoice(id, physicalId))
                }
            } else {
                choices.add(CameraChoice(id, null))
            }
        }

        backCameraChoices = choices
        if (backCameraChoices.isNotEmpty()) {
            backCameraIndex = backCameraIndex.coerceIn(0, backCameraChoices.lastIndex)
        } else {
            backCameraIndex = 0
        }
    }

    private fun showCameraDebugDialog() {
        if (backCameraChoices.isEmpty()) refreshBackCameraChoices()

        val text = buildString {
            appendLine("Back cameras: ${backCameraChoices.size}")
            backCameraChoices.forEachIndexed { index, choice ->
                val chars = cameraManager.getCameraCharacteristics(choice.logicalId)
                val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
                    else -> "unknown"
                }
                val physicalList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    chars.physicalCameraIds.joinToString(", ").ifBlank { "none" }
                } else {
                    "n/a"
                }

                appendLine("${index + 1}. logical=${choice.logicalId}")
                appendLine("   lensFacing=$facing")
                appendLine("   physicalCameraIds=$physicalList")
                appendLine("   physical=${choice.physicalId ?: "none"}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Camera IDs")
            .setMessage(text)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun chooseVideoSize(map: android.hardware.camera2.params.StreamConfigurationMap): Size {
        val choices = map.getOutputSizes(MediaRecorder::class.java)
            ?: map.getOutputSizes(SurfaceTexture::class.java)
            ?: return Size(1280, 960)
        val fourThree = choices.filter { it.width * 3 == it.height * 4 }
        val capped = fourThree.filter { it.width <= 1280 && it.height <= 960 }
        return capped.maxByOrNull { it.width * it.height }
            ?: fourThree.firstOrNull { it.width == 1280 && it.height == 960 }
            ?: fourThree.minByOrNull { it.width * it.height }
            ?: choices.maxByOrNull { it.width * it.height }
            ?: Size(1280, 960)
    }

    private fun choosePreviewSize(map: android.hardware.camera2.params.StreamConfigurationMap, reference: Size): Size {
        val choices = map.getOutputSizes(SurfaceTexture::class.java) ?: return reference
        val targetRatio = reference.width.toFloat() / reference.height.toFloat()
        val matching = choices.filter {
            abs((it.width.toFloat() / it.height.toFloat()) - targetRatio) < 0.01f
        }
        return matching
            .filter { it.width >= 640 && it.height >= 480 }
            .minByOrNull { it.width * it.height }
            ?: matching.minByOrNull { it.width * it.height }
            ?: reference
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (viewWidth == 0 || viewHeight == 0) return

        val matrix = Matrix()
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())

        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)

        val scale = maxOf(
            viewHeight.toFloat() / previewSize.height.toFloat(),
            viewWidth.toFloat() / previewSize.width.toFloat()
        )
        matrix.postScale(scale, scale, centerX, centerY)
        matrix.postRotate(-90f, centerX, centerY)

        viewBinding.viewFinder.setTransform(matrix)
    }

    private fun openCamera() {
        if (cameraDevice != null) return
        val id = chooseCameraId() ?: return
        cameraId = id

        try {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return

            videoSize = chooseVideoSize(map)
            previewSize = choosePreviewSize(map, videoSize)
            configureTransform(viewBinding.viewFinder.width, viewBinding.viewFinder.height)

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            cameraManager.openCamera(id, stateCallback, backgroundHandler)
        } catch (_: Exception) {
        }
    }

    private fun createPreviewSession() {
        val camera = cameraDevice ?: return
        val texture = viewBinding.viewFinder.surfaceTexture ?: return

        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(texture)

        try {
            val choice = backCameraChoices.getOrNull(backCameraIndex)
            val targets = mutableListOf(previewSurface)
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }

            if (isRecording) {
                val recorder = prepareMediaRecorder() ?: run {
                    cleanupRecordingFailure()
                    return
                }
                targets.add(recorder.surface)
                builder.addTarget(recorder.surface)
            }

            cameraSession?.close()
            cameraSession = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && choice?.physicalId != null) {
                val outputs = targets.map { surface ->
                    OutputConfiguration(surface).apply {
                        setPhysicalCameraId(choice.physicalId)
                    }
                }
                val executor = Executor { command ->
                    (backgroundHandler ?: Handler(mainLooper)).post(command)
                }
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    executor,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraSession = session
                            requestBuilder = builder
                            applyZoomProgress(viewBinding.zoomSeekBar.progress)
                            applyFocusProgress(viewBinding.focusSeekBar.progress)

                            try {
                                session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                                if (isRecording) {
                                    mediaRecorder?.start()
                                    recordingFlag = true
                                }
                            } catch (_: Exception) {
                                if (isRecording) cleanupRecordingFailure()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            if (isRecording) cleanupRecordingFailure()
                        }
                    }
                )
                camera.createCaptureSession(sessionConfig)
            } else {
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraSession = session
                        requestBuilder = builder
                        applyZoomProgress(viewBinding.zoomSeekBar.progress)
                        applyFocusProgress(viewBinding.focusSeekBar.progress)

                        try {
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                            if (isRecording) {
                                mediaRecorder?.start()
                                recordingFlag = true
                            }
                        } catch (_: Exception) {
                            if (isRecording) cleanupRecordingFailure()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        if (isRecording) cleanupRecordingFailure()
                    }
                }, backgroundHandler)
            }
        } catch (_: Exception) {
        }
    }

    private fun cleanupRecordingFailure() {
        isRecording = false
        recordingFlag = false
        recordingUri = null
        closeRecordingOutput()
        releaseMediaRecorder()
        showControlsAfterStop()
        createPreviewSession()
    }

    private fun prepareMediaRecorder(): MediaRecorder? {
        val uri = recordingUri ?: return null
        releaseMediaRecorder()
        closeRecordingOutput()
        recordingPfd = contentResolver.openFileDescriptor(uri, "w")
        val pfd = recordingPfd ?: return null
        mediaRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(pfd.fileDescriptor)
            setVideoEncodingBitRate(4_000_000)
            setVideoFrameRate(30)
            setVideoSize(videoSize.width, videoSize.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOrientationHint(0)
            prepare()
        }
        return mediaRecorder
    }

    private fun releaseMediaRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
        }
    }

    private fun applyZoom(ratio: Float) {
        val id = cameraId ?: return
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
        val zoom = ratio.coerceIn(1f, maxZoom)
        currentZoomRatio = zoom

        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
        val cropWidth = (sensorRect.width() / zoom).toInt()
        val cropHeight = (sensorRect.height() / zoom).toInt()
        val left = sensorRect.centerX() - cropWidth / 2
        val top = sensorRect.centerY() - cropHeight / 2
        val cropRegion = Rect(left, top, left + cropWidth, top + cropHeight)

        requestBuilder?.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)

        try {
            cameraSession?.setRepeatingRequest(requestBuilder?.build() ?: return, null, backgroundHandler)
        } catch (_: Exception) {
        }
    }

    private fun applyZoomProgress(progress: Int) {
        val id = cameraId ?: return
        val maxZoom = try {
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
        } catch (_: Exception) {
            1f
        }
        val effectiveMax = 1f + (maxZoom - 1f) * ZOOM_RANGE_FRACTION
        val zoomRatio = 1f + (effectiveMax - 1f) * (progress.coerceIn(0, 100) / 100f)
        applyZoom(zoomRatio)
    }

    private fun applyFocusDistance(distanceDiopters: Float) {
        val id = cameraId ?: return
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val minFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        if (minFocus <= 0f) return

        val focus = distanceDiopters.coerceIn(0f, minFocus)
        requestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        requestBuilder?.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus)

        try {
            cameraSession?.setRepeatingRequest(requestBuilder?.build() ?: return, null, backgroundHandler)
        } catch (_: Exception) {
        }
    }

    private fun applyFocusProgress(progress: Int) {
        val id = cameraId ?: return
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val minFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        if (minFocus <= 0f) return

        val nearestDiopters = minFocus
        val farDiopters = minOf(minFocus, 1f / FAR_FOCUS_METERS)
        val clamped = progress.coerceIn(0, 100)
        val targetDiopters = nearestDiopters - (nearestDiopters - farDiopters) * (clamped / 100f)

        requestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        requestBuilder?.set(CaptureRequest.LENS_FOCUS_DISTANCE, targetDiopters)

        try {
            cameraSession?.setRepeatingRequest(requestBuilder?.build() ?: return, null, backgroundHandler)
        } catch (_: Exception) {
        }
    }

    private fun saveFocusProgress(progress: Int) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_FOCUS_PROGRESS, progress)
            .apply()
    }

    private fun loadFocusProgress(): Int {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_FOCUS_PROGRESS, 0)
    }

    private fun saveZoomProgress(progress: Int) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_ZOOM_PROGRESS, progress)
            .apply()
    }

    private fun loadZoomProgress(): Int {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_ZOOM_PROGRESS, 0)
    }

    private fun hideControlsForRecording() {
        viewBinding.myView.visibility = View.VISIBLE
        viewBinding.lastThumbnail.isEnabled = false
        viewBinding.lastThumbnail.alpha = 0.35f
        viewBinding.listButton.isEnabled = false
        viewBinding.listButton.alpha = 0.35f
        viewBinding.helpButton.isEnabled = false
        viewBinding.helpButton.alpha = 0.35f
        viewBinding.cameraButton.isEnabled = false
        viewBinding.cameraButton.alpha = 0.35f
        viewBinding.videoCaptureButton.isEnabled = true
        viewBinding.videoCaptureButton.visibility = View.VISIBLE
        viewBinding.videoCaptureButton.alpha = 0.08f
        viewBinding.videoCaptureButton.text = "stop record"
        viewBinding.viewFinder.visibility = View.VISIBLE
    }

    private fun showControlsAfterStop() {
        viewBinding.myView.visibility = View.VISIBLE
        viewBinding.lastThumbnail.isEnabled = true
        viewBinding.lastThumbnail.alpha = 1f
        viewBinding.listButton.isEnabled = true
        viewBinding.listButton.alpha = 1f
        viewBinding.helpButton.isEnabled = true
        viewBinding.helpButton.alpha = 1f
        viewBinding.cameraButton.isEnabled = true
        viewBinding.cameraButton.alpha = 1f
        viewBinding.videoCaptureButton.visibility = View.VISIBLE
        viewBinding.videoCaptureButton.alpha = 0.08f
        viewBinding.videoCaptureButton.text = getString(R.string.start_capture)

        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params

        viewBinding.viewFinder.visibility = View.VISIBLE
    }

    private fun captureVideo() {
        if (isRecording || recordingFlag) {
            stopRecordingNow()
            return
        }

        currentName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        recordingUri = createVideoOutputUri("$currentName.mp4")
        if (recordingUri == null) return
        startTime = SystemClock.elapsedRealtimeNanos()
        csvBuffer.clear()
        csvBuffer.append("time,q0,q1,q2,q3\n")
        csvBuffer.append("0,${viewBinding.myView.mnq0},${viewBinding.myView.mnq1},${viewBinding.myView.mnq2},${viewBinding.myView.mnq3}\n")

        hideControlsForRecording()
        resetHead()
        isRecording = true
        createPreviewSession()
    }

    private fun stopRecordingNow() {
        if (!recordingFlag && !isRecording) return

        val savedUri = recordingUri
        recordingFlag = false
        isRecording = false

        try {
            cameraSession?.stopRepeating()
            cameraSession?.abortCaptures()
        } catch (_: Exception) {
        }

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
            savedUri?.let { contentResolver.delete(it, null, null) }
        }

        releaseMediaRecorder()
        closeRecordingOutput()

        try {
            cameraSession?.close()
        } catch (_: Exception) {
        }
        cameraSession = null
        requestBuilder = null
        recordingUri = null

        showControlsAfterStop()
        createPreviewSession()

        if (savedUri != null) {
            handleRecordedMedia(savedUri)
        }
    }

    private fun handleRecordedMedia(uri: android.net.Uri) {
        lastVideoUriString = uri.toString()
        saveCSV(currentName)
        markVideoFinished(uri)
        updateThumbnail(uri, 0)
    }

    private fun updateThumbnail(uri: android.net.Uri, attempt: Int) {
        uiHandler.postDelayed({
            try {
                val retriever = MediaMetadataRetriever()
                val bmp = try {
                    retriever.setDataSource(this, uri)
                    retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                } finally {
                    retriever.release()
                }

                if (bmp != null) {
                    viewBinding.lastThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    viewBinding.lastThumbnail.setPadding(0, 0, 0, 0)
                    viewBinding.lastThumbnail.setImageBitmap(bmp)
                } else if (attempt < 3) {
                    updateThumbnail(uri, attempt + 1)
                }
            } catch (_: Exception) {
                if (attempt < 3) {
                    updateThumbnail(uri, attempt + 1)
                }
            }
        }, if (attempt == 0) 150L else 250L)
    }

    private fun resetHead() {
        viewBinding.myView.resetHead()
    }

    private fun requestHeadReset() {
        requestReset = true
    }

    private fun createVideoOutputUri(displayName: String): android.net.Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/aCapNYS")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun saveCSV(baseName: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$baseName.csv")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/aCapNYS")
            }

            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: return

            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(csvBuffer.toString().toByteArray())
            }
        } catch (_: Exception) {
        }
    }

    private fun markVideoFinished(uri: android.net.Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            } catch (_: Exception) {
            }
        }
    }

    private fun closeRecordingOutput() {
        try {
            recordingPfd?.close()
        } catch (_: Exception) {
        } finally {
            recordingPfd = null
        }
    }

    var resetHeadCount: Int = 0

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                val q = FloatArray(4)
                SensorManager.getQuaternionFromVector(q, event.values)

                if (resetHeadCount > 0) {
                    viewBinding.myView.resetHead()
                    resetHeadCount--
                }

                if (requestReset) {
                    viewBinding.myView.setReferenceQuat(q[0], q[1], q[2], q[3])
                    requestReset = false
                }

                viewBinding.myView.setQuats(q[0], q[1], q[2], q[3])

                if (startTime != 0L && recordingFlag) {
                    val t = (event.timestamp - startTime) / 1_000_000
                    csvBuffer.append("$t,${q[0]},${q[1]},${q[2]},${q[3]}\n")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            requestHeadReset()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "権限が必要です", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.myView.setRpkPpk()
        viewBinding.viewFinder.surfaceTextureListener = surfaceTextureListener

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            return
        }

        startCamera()

        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        viewBinding.lastThumbnail.setOnClickListener {
            val uriString = lastVideoUriString
            if (uriString.isBlank()) return@setOnClickListener
            startActivity(Intent(this, PlayActivity::class.java).apply {
                putExtra("videouri", uriString)
            })
        }
        viewBinding.listButton.setOnClickListener { startActivity(Intent(this, ListActivity::class.java)) }
        viewBinding.helpButton.setOnClickListener { startActivity(Intent(this, How2Activity::class.java)) }
        viewBinding.cameraButton.setOnClickListener {
            if (backCameraChoices.isEmpty()) refreshBackCameraChoices()
            if (backCameraChoices.size > 1) {
                backCameraIndex = (backCameraIndex + 1) % backCameraChoices.size
            }
            cameraSession?.close()
            cameraSession = null
            cameraDevice?.close()
            cameraDevice = null
            openCamera()
            showCameraButtonMessage("Camera ${backCameraIndex + 1}/${maxOf(backCameraChoices.size, 1)}")
        }
        viewBinding.cameraButton.setOnLongClickListener {
            showCameraDebugDialog()
            true
        }
        viewBinding.zoomSeekBar.max = 100
        viewBinding.zoomSeekBar.progress = loadZoomProgress()
        viewBinding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                saveZoomProgress(progress)
                applyZoomProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        viewBinding.focusSeekBar.max = 100
        viewBinding.focusSeekBar.progress = loadFocusProgress()
        viewBinding.focusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                saveFocusProgress(progress)
                applyFocusProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showCameraButtonMessage(message: String) {
        val label = viewBinding.cameraToastText
        label.text = message
        label.visibility = View.VISIBLE
        cameraToastRunnable?.let { uiHandler.removeCallbacks(it) }
        cameraToastRunnable = Runnable { label.visibility = View.GONE }
        uiHandler.postDelayed(cameraToastRunnable!!, 1200)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (this::viewBinding.isInitialized && allPermissionsGranted()) {
            startCamera()
        }
        gravitySensor?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        rotationVectorSensor?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
    }

    override fun onPause() {
        if (recordingFlag || isRecording) {
            stopRecordingNow()
        }
        super.onPause()
        sensorManager.unregisterListener(this)
        try { cameraSession?.close() } catch (_: Exception) {}
        cameraSession = null
        try { cameraDevice?.close() } catch (_: Exception) {}
        cameraDevice = null
        requestBuilder = null
        releaseMediaRecorder()
        stopBackgroundThread()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
