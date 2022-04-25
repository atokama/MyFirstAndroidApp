package com.example.myfirstandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*

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

class MainActivity : AppCompatActivity(),
    CameraBridgeViewBase.CvCameraViewListener2 {

    private val viewFinder by lazy { findViewById<JavaCameraView>(R.id.cameraPreview) }
    private val tvFrameSize by lazy { findViewById<TextView>(R.id.tvFrameSize) }
    private val tvFps by lazy { findViewById<TextView>(R.id.tvFps) }

    private var height: Int = 0
    private var width: Int = 0

    companion object {
        init {
            System.loadLibrary("opencv_java4")
        }
    }

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


    private var mRgba = Mat(0, 0, CvType.CV_8UC4)

    override fun onCameraViewStarted(w: Int, h: Int) {
        width = w
        height = h
        tvFrameSize.text = "${w}x${h}"
        Log.d(TAG, "onCameraViewStarted() ${tvFrameSize.text}")

        mRgba = Mat(width, height, CvType.CV_8UC4)
        mGray = Mat(width, height, CvType.CV_8UC1)

    }

    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped()")
        mRgba.release()
    }

    private var mGray = Mat()
    private var mContours: MutableList<MatOfPoint> = mutableListOf()
    private var mHierarchy = Mat()

    private var framesCount: Int = 0
    private var timer = kotlin.concurrent.fixedRateTimer("timer", true, Date(), 500L) {
        runOnUiThread {
            Log.d(TAG, "timer")
            val fps = 2.0 * framesCount.toDouble()
            framesCount = 0
            tvFps.text = "${fps.toInt()} ${(1000.0 / fps).toInt()}.0"
        }
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        Log.v(TAG, "onCameraFrame()")
        framesCount += 1

        mRgba = inputFrame!!.rgba()
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY, 1)

        val threshold = Scalar(125.0)
        val maxPossible = Scalar(255.0)
        Core.inRange(mGray, threshold, maxPossible, mGray)
        mContours.clear()
        Imgproc.findContours(
            mGray,
            mContours,
            mHierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        Log.v(TAG, "contours: " + mContours.size.toString())

        val color = Scalar(255.0, 0.0, 0.0, 0.0) // red color
        val thickness = 3
        val contourIdx = -1 // -1 to draw all contours
        Imgproc.drawContours(mRgba, mContours, contourIdx, color, thickness)

        Log.v(TAG, "mRgba.size: " + mRgba.size().toString())
        return mRgba
    }

}