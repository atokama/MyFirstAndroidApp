package com.example.myfirstandroidapp

import org.junit.Assert.*
import org.junit.Test
import org.opencv.core.CvType
import org.opencv.core.Mat

class UtilTest {
    @Test
    fun printMat() {
        val mat = Mat(50, 50, CvType.CV_8UC4)
        assertEquals("", mat.print())
    }
}