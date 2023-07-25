package com.mycam.photomanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mycam.photomanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var galleryNumber: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var images: List<String>
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private var readImagesPermissionGranted = false
    private var cameraPermissionGranted = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        galleryNumber = binding.galleryNumber
        recyclerView = binding.recyclerView
        checkPermissionRequest()

        binding.btnCamera.setOnClickListener {
            startCamera()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())  { permissions ->
        readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
        writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted
        readImagesPermissionGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: readImagesPermissionGranted
        cameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?: cameraPermissionGranted

        if (readPermissionGranted && writePermissionGranted && cameraPermissionGranted) {
            loadImages()
            Toast.makeText(this,"Read & Write Permission Granted",Toast.LENGTH_SHORT).show()
        } else if (readPermissionGranted && readImagesPermissionGranted && cameraPermissionGranted) {
            loadImages()
            Toast.makeText(this,"Read Images Permission Granted",Toast.LENGTH_SHORT).show()
        } else {
            showPermissionDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkPermissionRequest()
    }

    private fun startCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun loadImages() {
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = GridLayoutManager(this,4)
        images = GalleryImages().listOfImages(this)

        galleryAdapter = GalleryAdapter(this,images)
        recyclerView.adapter = galleryAdapter
        galleryNumber.text = "Photos (${images.size})"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermissionRequest() {
        val hasReadPermission = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasReadImagesPermission = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasCameraPermission = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        writePermissionGranted = hasWritePermission || minSdk29
        readImagesPermissionGranted = hasReadImagesPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        cameraPermissionGranted = hasCameraPermission

        if (readPermissionGranted && writePermissionGranted && cameraPermissionGranted) {
            loadImages()
            Toast.makeText(this,"Read & Write Permission Granted",Toast.LENGTH_SHORT).show()
        } else if (readImagesPermissionGranted && cameraPermissionGranted) {
            loadImages()
            Toast.makeText(this,"Read Images Permission Granted",Toast.LENGTH_SHORT).show()
        } else {
            permissionLauncher.launch(PERMISSIONS)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("Some permissions are needed to be allowed to use this features")
        builder.setPositiveButton("Grant") { d, _ ->
            d.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            activityLauncher.launch(intent)
        }
        builder.setNegativeButton("Cancel") { d, _ ->
            d.dismiss()
        }
        builder.show()
    }
}