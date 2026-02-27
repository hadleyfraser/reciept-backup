package com.hadley.receiptbackup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hadley.receiptbackup.data.model.LoyaltyCard
import com.hadley.receiptbackup.utils.LoyaltyCardImageManager
import com.hadley.receiptbackup.utils.readableTextColor

@Composable
fun LoyaltyCardRow(
    card: LoyaltyCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val headerColor = Color(card.coverColor)
    val contentColor = readableTextColor(headerColor)
    val cacheFile = remember(card.id) { LoyaltyCardImageManager.localCacheFile(context, card.id) }
    val hasImage = card.cardImageUrl != null && cacheFile.exists()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 75.dp)
                .background(headerColor)
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
                        color = contentColor
                    )
                }
            }
        }
    }
}
