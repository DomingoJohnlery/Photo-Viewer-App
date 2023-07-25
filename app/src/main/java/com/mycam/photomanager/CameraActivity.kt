package com.mycam.photomanager

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.mycam.photomanager.databinding.ActivityCameraBinding
import kotlinx.coroutines.launch
import java.io.File

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var defaultCamera = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        previewView = binding.previewView
        setContentView(binding.root)

        startCamera()

        binding.btnCapture.setOnClickListener {
            takePicture()
            animateFlash()
        }
        binding.btnSwitch.setOnClickListener {
            switch()
        }
        binding.btnGallery.setOnClickListener {
            finish()
        }
    }
    private fun initPreview() {
        preview = Preview.Builder().build()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    private fun startCamera() {
        initPreview()
        imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(defaultCamera).build()
        lifecycleScope.launch {
            val cameraProvider = ProcessCameraProvider.getInstance(this@CameraActivity).await()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this@CameraActivity,cameraSelector,preview,imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Error: binding usecases $e")
            }
        }
    }

    private fun takePicture() {
        val fileName = "JPEG_${System.currentTimeMillis()}.jpeg"
        val storageDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //For android 10 and above
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        } else {
            // Below android 10
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"PhotoManager")
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val file = File(storageDir, fileName)
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.i(TAG, "The image has been saved in ${file.toUri()}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        saveImageToMediaStore(bitmap, fileName)
                    } else {
                        //Notify the gallery app to add the image to its collection
                        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.i(TAG, "Image capture failed ${exception.message}")
                }
            }
        )
    }

    private fun saveImageToMediaStore(image: Bitmap, title: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, title)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val contentResolver = contentResolver

        //Insert the image to MediaStore
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)

        //Open an OutputStream to write the image data to the MediaStore
        imageUri?.let {
            val outputStream = contentResolver.openOutputStream(it)
            outputStream?.use { stream ->
                //Compress the bitmap to JPEG and write it to the OutputStream
                image.compress(Bitmap.CompressFormat.JPEG,95,stream)
            }
        }
    }

    private fun switch() {
        defaultCamera = if (defaultCamera == CameraSelector.LENS_FACING_BACK){
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }
}