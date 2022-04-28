package com.example.myfirstandroidapp

import org.opencv.core.Mat

val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        val length = 23
        return if (tag.length <= length) tag else tag.substring(0, length)
    }

fun Mat.print(): String {
    return "$this ${System.identityHashCode(this)} ${this.nativeObjAddr} " +
            "${rows()}x${cols()} " +
            "type:${type()} depth:${depth()} elemSize:${elemSize()},${elemSize1()} " +
            "dims:${dims()}"
}