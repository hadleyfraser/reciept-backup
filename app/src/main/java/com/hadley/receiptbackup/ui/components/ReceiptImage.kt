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
fun ReceiptImage(navController: NavController, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        )
        val state = painter.state

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable {
                    navController.navigate("image?uri=${Uri.encode(imageUrl)}")
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
        }
    }
}