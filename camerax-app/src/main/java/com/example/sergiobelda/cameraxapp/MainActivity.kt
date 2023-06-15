package com.example.sergiobelda.cameraxapp

import android.Manifest
import android.R.attr.start
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.sergiobelda.cameraxapp.databinding.MainActivityBinding
import com.iceteck.silicompressorr.SiliCompressor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread


@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var imagePreview: Preview? = null

    private var videoCapture: VideoCapture? = null

    private lateinit var outputDirectory: File

    private var cameraControl: CameraControl? = null

    private var cameraInfo: CameraInfo? = null

    private var linearZoom = 0f
    private val DOWN_COUNTER_TIMER: Long = 60 * 5 * 1000 //10秒

    private var recording = false
    lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mediaController = MediaController(this);
        binding.videoView.setMediaController(mediaController);
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
        //字体大小
        binding.chronometer.setTextColor(Color.RED);    //字体颜色
        binding.chronometer.setFormat("%s");
        binding.chronometer.isCountDown=true
        binding.chronometer.text = "05:00"
//        binding.chronometer.base = SystemClock.elapsedRealtime()+DOWN_COUNTER_TIMER;
        binding.chronometer.setOnChronometerTickListener { ch -> // 如果从开始计时到现在超过了60s
            if (SystemClock.elapsedRealtime()-ch.getBase()>=0) {
                ch.stop()
                if (recording) {
                    videoCapture?.stopRecording()
                    binding.cameraCaptureButton.isSelected = false
                    binding.cameraCaptureButton.isEnabled = false
                    recording = false
                }
            }

        }

        outputDirectory = getOutputDirectory()

        binding.cameraCaptureButton.setOnClickListener {
            if (recording) {
                binding.chronometer.stop()
                videoCapture?.stopRecording()
                it.isSelected = false
                recording = false
            } else {
                binding.chronometer.base = SystemClock.elapsedRealtime()+DOWN_COUNTER_TIMER;
                binding.chronometer.start()
                recordVideo()
                it.isSelected = true
                recording = true
            }
        }
//        initCameraModeSelector()
        binding.cameraTorchButton.setOnClickListener {
            toggleTorch()
        }
        binding.imageButtonClose.setOnClickListener {
            if (binding.videoView.isPlaying) {
                binding.videoView.stopPlayback()
            }
            binding.chronometer.text= "05:00"
            previewVisible(true)

        }
        binding.imageButtonConfirm.setOnClickListener {
//            Toast.makeText(
//                this@MainActivity,
//                "开始压缩视频",
//                Toast.LENGTH_SHORT
//            ).show()
            previewVisible(true)
            binding.chronometer.text= "05:00"
//            compressVideo()
        }
    }




    private fun compressVideo() {
        val time1 = System.currentTimeMillis()
        thread {
            val filePath: String =
                SiliCompressor.with(this@MainActivity)
                    .compressVideo(
                        file.absolutePath,
                        outputDirectory.path,
                        960,
                        540,
                        1000000
                    )
            val time2 = System.currentTimeMillis()
            //计算两个时间time1 time2差了多少秒
            val time = (time2 - time1) / 1000
            runOnUiThread {
                previewVisible(true)
                binding.chronometer.text= "05:00"
                Toast.makeText(
                    this@MainActivity,
                    "耗时:${time}s,Compressed video path: $filePath",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun previewVisible(isPreViewVisible: Boolean) {
        binding.cameraCaptureButton.isEnabled = true
        binding.videoView.visibility = if (isPreViewVisible) View.GONE else View.VISIBLE
        binding.imageButtonConfirm.visibility = if (isPreViewVisible) View.GONE else View.VISIBLE
        binding.imageButtonClose.visibility = if (isPreViewVisible) View.GONE else View.VISIBLE
        binding.previewView.visibility = if (isPreViewVisible) View.VISIBLE else View.GONE
        binding.cameraCaptureButton.visibility = if (isPreViewVisible) View.VISIBLE else View.GONE
        binding.cameraTorchButton.visibility = if (isPreViewVisible) View.VISIBLE else View.GONE
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
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener({
            imagePreview = Preview.Builder().apply {
//                setTargetAspectRatio(AspectRatio.RATIO_16_9)
                setTargetResolution(Size(540, 960))
                setTargetRotation(windowManager.defaultDisplay.rotation)
            }.build()


            videoCapture = VideoCapture.Builder().apply {
//                setTargetAspectRatio(AspectRatio.RATIO_16_9)
                setTargetResolution(Size(540, 960))
                setVideoFrameRate(20)
                setBitRate(2 * 1024 * 1024)
            }.build()

            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imagePreview,
                // imageAnalysis,
//                imageCapture,
                videoCapture
            )
            binding.previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            imagePreview?.setSurfaceProvider(binding.previewView.surfaceProvider)
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            setTorchStateObserver()
            setZoomStateObserver()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setTorchStateObserver() {
        cameraInfo?.torchState?.observe(this) { state ->
            if (state == TorchState.ON) {
                binding.cameraTorchButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_flashlight_off_24dp
                    )
                )
            } else {
                binding.cameraTorchButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_flashlight_on_24dp
                    )
                )
            }
        }
    }

    private fun setZoomStateObserver() {
        cameraInfo?.zoomState?.observe(this) { state ->
            // state.linearZoom
            // state.zoomRatio
            // state.maxZoomRatio
            // state.minZoomRatio
            Log.d(TAG, "${state.linearZoom}")
        }
    }


    private fun recordVideo() {
        file = createFile(
            outputDirectory,
            FILENAME,
            VIDEO_EXTENSION
        )
        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(file).build()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            videoCapture?.startRecording(
                outputFileOptions,
                cameraExecutor,
                object : VideoCapture.OnVideoSavedCallback {
                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        val msg = "Video capture succeeded: ${file.absolutePath}"
                        runOnUiThread {
                            previewVisible(false)
                            binding.videoView.setVideoPath(file.absolutePath)
                            binding.videoView.start()
                        }

                    }

                    override fun onError(
                        videoCaptureError: Int,
                        message: String,
                        cause: Throwable?
                    ) {
                        val msg = "Video capture failed: $message"
                        binding.previewView.post {
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                })
        }

    }

    private fun toggleTorch() {
        if (cameraInfo?.torchState?.value == TorchState.ON) {
            cameraControl?.enableTorch(false)
        } else {
            cameraControl?.enableTorch(true)
        }
    }

    // Manage camera Zoom
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (linearZoom <= 0.9) {
                    linearZoom += 0.1f
                }
                cameraControl?.setLinearZoom(linearZoom)
                true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (linearZoom >= 0.1) {
                    linearZoom -= 0.1f
                }
                cameraControl?.setLinearZoom(linearZoom)
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun getOutputDirectory(): File {
        // TODO: 29/01/2021 Remove externalMediaDirs (deprecated)
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        private const val PHOTO = 0
        private const val VIDEO = 1

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }
}
