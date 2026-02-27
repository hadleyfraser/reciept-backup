package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hadley.receiptbackup.data.repository.LoyaltyCardViewModel
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import com.hadley.receiptbackup.utils.LoyaltyCardImageManager
import com.hadley.receiptbackup.utils.barcodeOptionFromName
import com.hadley.receiptbackup.utils.createBarcodeBitmap
import com.hadley.receiptbackup.utils.createBarcodeDimensions
import com.hadley.receiptbackup.utils.readableTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyCardDetailScreen(
    navController: NavController,
    cardId: String,
    viewModel: LoyaltyCardViewModel,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val card = viewModel.getCardById(cardId)
    val scaffoldState = LocalAppScaffoldState.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    val window = (context as Activity).window
    DisposableEffect(Unit) {
        val originalBrightness = window.attributes.screenBrightness
        window.attributes = window.attributes.also {
            it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }
        onDispose {
            window.attributes = window.attributes.also {
                it.screenBrightness = originalBrightness
            }
        }
    }

    LaunchedEffect(cardId) {
        scaffoldState.title = "Card"
        scaffoldState.showTopBar = true
        scaffoldState.drawerEnabled = true
        scaffoldState.floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FloatingActionButton(onClick = { navController.navigate("cardEdit?cardId=$cardId") }) {
                    androidx.compose.material3.Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                FloatingActionButton(onClick = { showConfirmDialog = true }) {
                    androidx.compose.material3.Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    if (card == null) {
        Text("Card not found", modifier = Modifier.padding(16.dp))
        return
    }

    val headerColor = Color(card.coverColor)
    val headerTextColor = readableTextColor(headerColor)
    val barcodeOption = barcodeOptionFromName(card.barcodeType)
    val is2d = barcodeOption?.is2d ?: false
    val (width, height) = createBarcodeDimensions(is2d)
    val barcodeImage = remember(card.barcodeType, card.barcodeValue) {
        createBarcodeBitmap(card.barcodeValue, card.barcodeType, width, height)
    }
    val cacheFile = remember(card.id) { LoyaltyCardImageManager.localCacheFile(context, card.id) }
    val hasImage = card.cardImageUrl != null && cacheFile.exists()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = headerColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 75.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(cacheFile)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Card image",
                        contentScale = ContentScale.Fit,
                        modifier = if (card.imageOnly) {
                            Modifier
                                .heightIn(max = 32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                                .size(51.dp)
                                .clip(RoundedCornerShape(8.dp))
                        }
                    )
                }
                if (!card.imageOnly) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = headerTextColor
                        )
                        if (card.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = headerTextColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (barcodeImage != null) {
                Image(
                    bitmap = barcodeImage,
                    contentDescription = "Barcode preview",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.barcodeValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Barcode preview unavailable")
                }
            }
        }

        Text(
            text = "Type: ${barcodeOption?.label ?: card.barcodeType}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete card?") },
            text = { Text("Are you sure you want to delete this card?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(context, card.id)
                    showConfirmDialog = false
                    navController.popBackStack()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
