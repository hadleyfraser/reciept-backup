package com.hadley.receiptbackup.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavController
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.compose.rememberAsyncImagePainter
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState

@Composable
fun FullScreenImageScreen(
    navController: NavController,
    imageUri: String,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val scaffoldState = LocalAppScaffoldState.current

    LaunchedEffect(imageUri) {
        scaffoldState.title = ""
        scaffoldState.showTopBar = false
        scaffoldState.drawerEnabled = false
        scaffoldState.floatingActionButton = {}
    }

    val isRemote = imageUri.startsWith("http://") || imageUri.startsWith("https://")

    val requestBuilder = ImageRequest.Builder(context)
        .data(imageUri)

    if (isRemote) {
        requestBuilder
            .diskCacheKey(imageUri)
            .diskCachePolicy(CachePolicy.READ_ONLY)
            .networkCachePolicy(CachePolicy.DISABLED)
    } else {
        requestBuilder.diskCachePolicy(CachePolicy.ENABLED)
    }

    val painter = rememberAsyncImagePainter(requestBuilder.build())
    val state = painter.state
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
            .padding(paddingValues)
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
            painter = painter,
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
        if (state is coil.compose.AsyncImagePainter.State.Error && isRemote) {
            Text("Image not cached", color = Color.White)
        }
    }
}
