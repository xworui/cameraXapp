package com.example.cameraxapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Point
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxapp.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.SimpleFormatter

class MainActivity : AppCompatActivity() {

    lateinit var bind: ActivityMainBinding

    private lateinit var outputDir: File
    lateinit var cameraExecutor: ExecutorService
    private var cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null

    private lateinit var videoCapture: VideoCapture



    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        getPermission()

        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDir = getOutputDir()

        startCamera()

        bind.flipCamera.setOnClickListener {
            if (cameraFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraFacing = CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        bind.startVideo.setOnClickListener {
            bind.startVideo.setImageResource(R.drawable.ic_baseline_play_circle_24_red)
            val videoFile = File(outputDir,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis())+".mp4")

            val outputFileOption = VideoCapture.OutputFileOptions.Builder(videoFile).build()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            videoCapture.startRecording(outputFileOption, ContextCompat.getMainExecutor(this),
            object: VideoCapture.OnVideoSavedCallback{
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    Log.d(Constants.TAG, "Видео записано")
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.d(Constants.TAG, "Ошибка записи видео")
                }

            })

        }
        bind.stopVideo.setOnClickListener {
            bind.startVideo.setImageResource(R.drawable.ic_baseline_play_circle_24)
            videoCapture.stopRecording()

        }



    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun getOutputDir(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir!=null && mediaDir.exists()) mediaDir else filesDir

    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFeature = ProcessCameraProvider.getInstance(this)
        cameraProviderFeature.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFeature.get()
            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.setSurfaceProvider(bind.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val point = Point()

            videoCapture = VideoCapture.Builder()
                .setAudioBitRate(320000)
                .setAudioSampleRate(44100)
                .setAudioChannelCount(2)
                .build()

            val cameraSelector = cameraFacing

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider
                    .bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture)

                bind.capture.setOnClickListener {
                    val photoFile = File(
                        outputDir, SimpleDateFormat(
                            Constants.FILE_NAME_FORMAT, Locale.getDefault())
                            .format(System.currentTimeMillis())+".png"
                        )
                    val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture!!.takePicture(outputOption, ContextCompat.getMainExecutor(this),
                    object: ImageCapture.OnImageSavedCallback{
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val msg = "Фото сохранено в: ${(Uri.fromFile(photoFile))}"
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(Constants.TAG,
                            "Ошибка записи в файл: ${exception.message}", exception)
                        }

                    })


                }

                bind.toggleFlash.setOnClickListener {
                    if (camera.cameraInfo.hasFlashUnit()) {
                        if (camera.cameraInfo.torchState.value == 0) {
                            camera.cameraControl.enableTorch(true)
                            bind.toggleFlash.setImageResource(R.drawable.ic_baseline_flash_off_24)
                        } else {
                            camera.cameraControl.enableTorch(false)
                            bind.toggleFlash.setImageResource(R.drawable.ic_baseline_flash_on_24)
                        }

                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity,
                                "Подсветка не доступна", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }catch (e: java.lang.Exception){
                Log.d(Constants.TAG, "Ошибка запуска камеры: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun getPermission() {
        val permissionList = mutableListOf<String>()
        Constants.REQUIRED_PERMISSION.forEach { perm->
            if(ContextCompat.checkSelfPermission(this, perm)
            != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(perm)
            }
        }
        if (permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), Constants.REQUEST_CODE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach { perm->
            if (perm != PackageManager.PERMISSION_GRANTED){
                getPermission()
            }
        }
    }
}

