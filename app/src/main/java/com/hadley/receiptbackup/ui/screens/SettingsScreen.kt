package com.hadley.receiptbackup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.local.SettingsDataStore
import com.hadley.receiptbackup.data.local.ThemeMode
import com.hadley.receiptbackup.data.repository.ImageCacheStatus
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun SettingsScreen(
    navController: NavController,
    viewModel: ReceiptItemViewModel,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val themeMode by SettingsDataStore.themeModeFlow(context)
        .collectAsState(initial = ThemeMode.SYSTEM)
    var expanded by remember { mutableStateOf(false) }
    val scaffoldState = LocalAppScaffoldState.current
    val coroutineScope = rememberCoroutineScope()

    val items by viewModel.items.collectAsState()
    val imageCacheStatus by viewModel.imageCacheStatus.collectAsState()
    val missingCount = items.count { item ->
        !item.imageUrl.isNullOrBlank() &&
        imageCacheStatus[item.id] != ImageCacheStatus.CACHED &&
        imageCacheStatus[item.id] != ImageCacheStatus.DOWNLOADING
    }
    val isDownloading = imageCacheStatus.values.any { it == ImageCacheStatus.DOWNLOADING }

    LaunchedEffect(Unit) {
        scaffoldState.title = "Settings"
        scaffoldState.showTopBar = true
        scaffoldState.drawerEnabled = true
        scaffoldState.floatingActionButton = {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = {},
                label = { Text("Theme") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            expanded = false
                            coroutineScope.launch {
                                SettingsDataStore.setThemeMode(context, mode)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Receipts", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        val statusText = when {
            isDownloading -> "Downloading images..."
            missingCount == 0 -> "All receipt images are cached"
            missingCount == 1 -> "1 receipt image not cached"
            else -> "$missingCount receipt images not cached"
        }
        Text(statusText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { viewModel.retryMissingImageDownloads(context) },
                enabled = missingCount > 0 && !isDownloading
            ) {
                Text("Download missing images")
            }
            if (isDownloading) {
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}
