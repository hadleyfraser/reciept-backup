package com.hadley.receiptbackup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.repository.ImageCacheStatus

@Composable
fun ReceiptItemRow(
    item: ReceiptItem,
    cacheStatus: ImageCacheStatus?,
    pendingUpload: Boolean,
    uploadProgress: Int?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(item.store, style = MaterialTheme.typography.bodyMedium)
                    Text(item.date.toString(), style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (pendingUpload) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = "Uploading receipt")
                        val percent = uploadProgress ?: 0
                        val percentText = if (percent > 0) "$percent%" else "0%"
                        val percentColor = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (percent > 0) 1f else 0f
                        )
                        Text(
                            text = percentText,
                            style = MaterialTheme.typography.labelSmall,
                            color = percentColor
                        )
                    } else {
                        when (cacheStatus) {
                            ImageCacheStatus.CACHED -> {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Image cached"
                                )
                            }
                            ImageCacheStatus.DOWNLOADING -> {
                                Icon(
                                    Icons.Outlined.CloudDownload,
                                    contentDescription = "Caching image"
                                )
                            }
                            ImageCacheStatus.FAILED -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Image cache failed"
                                )
                            }
                            ImageCacheStatus.NOT_CACHED -> Unit
                            null -> Unit
                        }
                    }
                }
            }
        }
    }
}
