package com.hadley.receiptbackup.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun ReceiptImage(navController: NavController, imageUrl: String?, localImageUri: String? = null) {
    val imageData = localImageUri ?: imageUrl
    val isRemote = imageData?.startsWith("http://") == true || imageData?.startsWith("https://") == true
    if (!imageData.isNullOrEmpty()) {
        val requestBuilder = ImageRequest.Builder(LocalContext.current)
            .data(imageData)
            .crossfade(true)

        if (isRemote) {
            requestBuilder
                .diskCacheKey(imageData)
                .diskCachePolicy(CachePolicy.READ_ONLY)
                .networkCachePolicy(CachePolicy.DISABLED)
        } else {
            requestBuilder.diskCachePolicy(CachePolicy.ENABLED)
        }

        val painter = rememberAsyncImagePainter(model = requestBuilder.build())
        val state = painter.state

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable {
                    navController.navigate("image?uri=${Uri.encode(imageData)}")
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = "Receipt Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (state is AsyncImagePainter.State.Loading) {
                CircularProgressIndicator()
            }
            if (state is AsyncImagePainter.State.Error && isRemote) {
                Text("Image not cached")
            }
        }
    }
}