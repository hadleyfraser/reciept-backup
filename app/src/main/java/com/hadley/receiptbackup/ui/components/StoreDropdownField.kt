package com.hadley.receiptbackup.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDropdownField(
    store: String,
    onStoreChange: (String) -> Unit,
    allStores: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredStores = allStores.filter {
        it.contains(store, ignoreCase = true)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = true }
    ) {
        OutlinedTextField(
            value = store,
            onValueChange = { newText ->
                onStoreChange(newText)
                expanded = true
            },
            label = { Text("Store") },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        if (filteredStores.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredStores.forEach { matchedStore ->
                    DropdownMenuItem(
                        text = { Text(matchedStore) },
                        onClick = {
                            onStoreChange(matchedStore)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
