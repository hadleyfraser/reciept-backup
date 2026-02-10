package com.hadley.receiptbackup.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun readableTextColor(background: Color): Color {
    return if (background.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

