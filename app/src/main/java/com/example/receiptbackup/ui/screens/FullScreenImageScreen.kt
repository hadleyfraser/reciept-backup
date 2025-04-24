package com.example.receiptbackup.ui.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun FullScreenImageScreen(navController: NavController, imageUri: String) {
    var rawScale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isZoomed by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = tween(durationMillis = 200),
        label = "Zoom Animation"
    )

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { containerSize = it.size }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (rawScale * zoom).coerceIn(1f, 5f)
                    rawScale = newScale

                    val maxOffsetX = ((containerSize.width * (newScale - 1)) / 2)
                    val maxOffsetY = ((containerSize.height * (newScale - 1)) / 2)

                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        navController.popBackStack()
                    },
                    onDoubleTap = {
                        if (!isZoomed) {
                            rawScale = 2f
                            isZoomed = true
                        } else {
                            rawScale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            isZoomed = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(Uri.parse(imageUri)),
            contentDescription = "Full screen image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}
