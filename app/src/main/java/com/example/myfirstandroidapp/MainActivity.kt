package com.example.myfirstandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.CvType
import org.opencv.core.Mat

val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        val length = 23
        return if (tag.length <= length) tag else tag.substring(0, length)
    }

private const val REQUEST_CODE_PERMISSIONS = 111
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
)

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val viewFinder by lazy { findViewById<JavaCameraView>(R.id.camera_view) }

    lateinit var imageMat: Mat

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun permissionsGranted(): Boolean {
        val granted = allPermissionsGranted()
        Log.d(TAG, "permissionsGranted() $granted")
        return granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        viewFinder.visibility = SurfaceView.VISIBLE
        viewFinder.setCameraIndex(CameraCharacteristics.LENS_FACING_FRONT)
        viewFinder.setCvCameraViewListener(this)

        // Request camera permissions
        if (permissionsGranted()) {
            enableCameraView()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun enableCameraView() {
        Log.d(TAG, "enableCameraView()")
        viewFinder.setCameraPermissionGranted()
        initOpenCv()
        viewFinder.enableView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult()")

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (permissionsGranted()) {
                enableCameraView()
            } else {
                Log.e(TAG, "finish - permissions not granted")
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewFinder?.let { viewFinder.disableView() }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewFinder?.let { viewFinder.disableView() }
    }

    private fun initOpenCv() {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "initOpenCv() successful")
        } else {
            Log.e(TAG, "finish - initOpenCv() failed")
            finish()
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "width: $width, height: $height")
        imageMat = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped()")
        imageMat.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        Log.v(TAG, "onCameraFrame()")
        imageMat = inputFrame!!.rgba()
        return imageMat
    }
}