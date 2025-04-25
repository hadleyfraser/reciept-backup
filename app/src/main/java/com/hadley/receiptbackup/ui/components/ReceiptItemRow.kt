package com.hadley.receiptbackup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hadley.receiptbackup.data.model.ReceiptItem

@Composable
fun ReceiptItemRow(item: ReceiptItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text(item.store, style = MaterialTheme.typography.bodyMedium)
            Text(item.date.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}
