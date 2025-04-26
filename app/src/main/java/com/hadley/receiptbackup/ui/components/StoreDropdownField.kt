package com.hadley.receiptbackup.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.hadley.receiptbackup.utils.getDropdownMaxHeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDropdownField(
    store: String,
    onStoreChange: (String) -> Unit,
    allStores: List<String>,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val fieldCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dropdownOffsetY = getDropdownMaxHeight(fieldCoordinates.value)

    val filteredStores = allStores.filter {
        it.contains(store, ignoreCase = true)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
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
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    fieldCoordinates.value = coordinates
                }            ,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = enabled
        )

        if (filteredStores.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = dropdownOffsetY)
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
