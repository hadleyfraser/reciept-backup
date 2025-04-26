package com.hadley.receiptbackup.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun getDropdownMaxHeight(
    fieldCoordinates: LayoutCoordinates?,
    estimatedKeyboardHeight: Dp = 320.dp
): Dp {
    if (fieldCoordinates == null) return 0.dp

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current

    val fieldTop = with(density) { fieldCoordinates.positionInWindow().y.toDp() }
    val fieldHeight = with(density) { fieldCoordinates.size.height.toDp() }
    val fieldBottom = fieldTop + fieldHeight

    return screenHeight - fieldBottom -  estimatedKeyboardHeight
}
