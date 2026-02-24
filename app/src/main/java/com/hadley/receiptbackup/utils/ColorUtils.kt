package com.hadley.receiptbackup.utils

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun readableTextColor(background: Color): Color {
    return if (background.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

// Samples the four corners of a bitmap and returns the averaged color as an ARGB int.
// Works well for logo images with solid background colors.
fun extractBackgroundColor(bitmap: Bitmap): Int {
    val w = bitmap.width - 1
    val h = bitmap.height - 1
    val corners = listOf(
        bitmap.getPixel(0, 0),
        bitmap.getPixel(w, 0),
        bitmap.getPixel(0, h),
        bitmap.getPixel(w, h)
    )
    val r = corners.map { AndroidColor.red(it) }.average().toInt()
    val g = corners.map { AndroidColor.green(it) }.average().toInt()
    val b = corners.map { AndroidColor.blue(it) }.average().toInt()
    return AndroidColor.rgb(r, g, b)
}

