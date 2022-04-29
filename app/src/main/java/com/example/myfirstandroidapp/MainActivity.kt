package com.example.myfirstandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
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
import java.lang.Math.abs
import java.util.*

private const val REQUEST_CODE_PERMISSIONS = 111
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
)

class MainActivity : AppCompatActivity(),
    CameraBridgeViewBase.CvCameraViewListener2 {

    private val onSwipeListener = object {
        fun onSwipeTop() {
            Log.v(TAG, "gesture:onSwipeTop()")
        }

        fun onSwipeBottom() {
            Log.v(TAG, "gesture:onSwipeBottom()")
        }

        fun onSwipeLeft() {
            Log.v(TAG, "gesture:onSwipeLeft()")
        }

        fun onSwipeRight() {
            Log.v(TAG, "gesture:onSwipeRight()")
        }
    }

    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float
        ): Boolean {
            with(onSwipeListener) {
                if (abs(velocityX) > abs(velocityY))
                    if (velocityX > 0) onSwipeRight() else onSwipeLeft()
                else
                    if (velocityY > 0) onSwipeBottom() else onSwipeTop()
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private val gestureDetector = GestureDetector(baseContext, onGestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private val cameraPreview by lazy { findViewById<JavaCameraView>(R.id.cameraPreview) }
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

        cameraPreview.visibility = SurfaceView.VISIBLE
        cameraPreview.setCameraIndex(CameraCharacteristics.LENS_FACING_FRONT)
        cameraPreview.setCvCameraViewListener(this)

        // Request camera permissions
        if (permissionsGranted()) {
            enableCameraView()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun enableCameraView() {
        Log.d(TAG, "enableCameraView()")
        cameraPreview.setCameraPermissionGranted()
        initOpenCv()
        cameraPreview.enableView()
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
        cameraPreview?.let { cameraPreview.disableView() }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraPreview?.let { cameraPreview.disableView() }
    }

    private fun initOpenCv() {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "initOpenCv() successful")
        } else {
            Log.e(TAG, "finish - initOpenCv() failed")
            finish()
        }
    }


    private var frame = Mat()
    private val chain = Filter.Chain<Mat>(Mat())
    private val builder = Filter.Chain.Builder<Mat>(chain)

    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped()")
        frame.release()
    }

    private var framesCount: Int = 0
    private val timer =
        kotlin.concurrent.fixedRateTimer("timer", true, Date(), 500L) {
            runOnUiThread {
                Log.d(TAG, "timer")
                val fps = 2.0 * framesCount.toDouble()
                framesCount = 0
                tvFps.text = "${fps.toInt()} ${(1000.0 / fps).toInt()}.0"
            }
        }

    override fun onCameraViewStarted(w: Int, h: Int) {
        tvFrameSize.text = "${w}x${h}"
        Log.d(TAG, "onCameraViewStarted() ${tvFrameSize.text}")

        width = w
        height = h
        frame = Mat(width, height, CvType.CV_8UC4)

        builder.add(Mat()) { data, result ->
            Log.v(TAG, "Imgproc.cvtColor() data:${data.print()} result:${result.print()}")
            Imgproc.cvtColor(data, result, Imgproc.COLOR_RGBA2GRAY)
        }

        builder.add(Mat()) { data, result ->
            Log.v(TAG, "Core.inRange() data:${data.print()} result:${result.print()}")
            val threshold = Scalar(125.0)
            val maxPossible = Scalar(255.0)
            Core.inRange(data, threshold, maxPossible, result)
        }

        builder.add(frame) { data, result ->
            Log.v(TAG, "Imgproc.findContours() data:${data.print()}")
            val contours: MutableList<MatOfPoint> = mutableListOf()
            val hierarchy = Mat()
            Imgproc.findContours(
                data,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            Log.v(TAG, "contours: ${contours.size}")

            val conversion = Imgproc.COLOR_GRAY2RGBA
            Log.v(TAG, "Imgproc.cvtColor() result:${result.print()}")
            Imgproc.cvtColor(result, result, conversion)

            Log.v(TAG, "Imgproc.drawContours() result:${result.print()}")
            val color = Scalar(255.0, 0.0, 0.0, 0.0) // red color
            val thickness = 3
            val contourIdx = -1 // -1 to draw all contours
            Imgproc.drawContours(result, contours, contourIdx, color, thickness)
            Log.v(TAG, "contours drawn")
        }
    }

    override fun onCameraFrame(input: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        framesCount += 1
        frame = input!!.rgba()
        Log.v(TAG, "onCameraFrame() before: ${frame.print()}")

        with(chain.filters.last()) {
            output = frame
            prev?.output = frame
        }

        with(chain.filters.first()) {
            process(frame)
        }

        Log.v(TAG, "after: ${frame.print()}")
        return frame
    }


}