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

    private var viewMode = 0
    private val onSwipeListener = object {
        fun onSwipeTop() {
            viewMode += 1
            Log.v(TAG, "gesture:onSwipeTop() viewMode:$viewMode")
        }

        fun onSwipeBottom() {
            viewMode -= 1
            Log.v(TAG, "gesture:onSwipeBottom() viewMode:$viewMode")
        }

        fun onSwipeLeft() {
            threshold.`val`[0] -= 10.0
            Log.v(TAG, "gesture:onSwipeLeft() ${threshold.`val`[0]}")
        }

        fun onSwipeRight() {
            threshold.`val`[0] += 10.0
            Log.v(TAG, "gesture:onSwipeRight() ${threshold.`val`[0]}")
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

    class Image(
        var pixels: Mat = Mat(),
        val contours: MutableList<MatOfPoint> = mutableListOf()
    ) {
        fun clone(): Image = Image(pixels = pixels.clone(), contours = contours.toMutableList())
        fun release() = pixels.release()
        fun print() = "contours: $contours, pixels: ${pixels.print()}"
    }

    private var frame = Image()
    private val chain = Filter.Chain<Image>(frame.clone())
    private val builder = Filter.Chain.Builder<Image>(chain)

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
    var threshold = Scalar(125.0)
    val maxPossible = Scalar(255.0)

    override fun onCameraViewStarted(w: Int, h: Int) {
        tvFrameSize.text = "${w}x${h}"
        Log.d(TAG, "onCameraViewStarted() ${tvFrameSize.text}")

        width = w
        height = h
        frame = Image(pixels = Mat(width, height, CvType.CV_8UC4))

        builder.add(frame.clone()) { data, result ->
            convertColor(data, Imgproc.COLOR_RGBA2GRAY, result)
        }

        builder.add(frame.clone()) { data, result ->
            Log.v(TAG, "Core.inRange() data:${data.print()} result:${result.print()}")
            Core.inRange(data.pixels, threshold, maxPossible, result.pixels)
        }

        builder.add(frame.clone()) { data, result ->
            Log.v(TAG, "Imgproc.findContours() data:${data.print()}")
            result.contours.clear()
            val hierarchy = Mat()
            Imgproc.findContours(
                data.pixels,
                result.contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
        }

        builder.add(frame.clone()) { data, result ->
            convertColor(data, Imgproc.COLOR_GRAY2RGBA, result)
        }

        builder.add(frame.clone()) { data, result ->
            Log.v(TAG, "Imgproc.drawContours() ${data.contours} result:${result.print()}")
            val color = Scalar(255.0, 0.0, 0.0, 0.0) // red color (RGBA)
            val thickness = 3
            val contourIdx = -1 // draw all contours
            Imgproc.drawContours(result.pixels, data.contours, contourIdx, color, thickness)
        }

    }

    override fun onCameraFrame(input: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        framesCount += 1
        frame.pixels = input!!.rgba()
        Log.v(TAG, "onCameraFrame() before: ${frame.print()}")

        when (viewMode) {
            1 -> {
                Log.v(TAG, "viewMode: contours over binary (gray) image")
                chain.filters.forEach {
                    it.setOutput(frame)
                }
                with(chain.filters.last()) {
                    prev!!.enabled = true
                }
            }
            else -> {
                Log.v(TAG, "viewMode: contours over original image")
                with(chain.filters.last()) {
                    setOutput(frame)
                    prev!!.enabled = false
                }
            }
        }
        with(chain.filters.first()) {
            process(frame)
        }

        Log.v(TAG, "after: ${frame.print()}")
        return frame.pixels
    }


    private fun convertColor(
        data: Image,
        conversionType: Int,
        result: Image
    ) {
        Log.v(TAG, "Imgproc.cvtColor() conversion:$conversionType result:${result.print()}")
        Imgproc.cvtColor(data.pixels, result.pixels, conversionType)
    }


}