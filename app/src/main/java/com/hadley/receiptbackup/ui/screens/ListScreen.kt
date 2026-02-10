package com.hadley.receiptbackup.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import com.hadley.receiptbackup.ui.components.ReceiptItemRow
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun ListScreen(
    navController: NavController,
    viewModel: ReceiptItemViewModel,
    paddingValues: PaddingValues
) {
    val items by viewModel.items.collectAsState()
    val isLoadingReceipts by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()
    val imageCacheStatus by viewModel.imageCacheStatus.collectAsState()
    val imageDownloadProgress by viewModel.imageDownloadProgress.collectAsState()

    val listState = rememberLazyListState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    val stores = listOf("All") + items.map { it.store }.distinct().sorted()

    val filteredItems = items
        .filter { it.name.contains(searchQuery, ignoreCase = true) }
        .filter { selectedStore == "All" || it.store == selectedStore }

    val grouped = filteredItems.sortedByDescending { it.date }.groupBy {
        val month = it.date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val year = it.date.year
        "$month $year"
    }

    val focusManager = LocalFocusManager.current
    val scaffoldState = LocalAppScaffoldState.current

    LaunchedEffect(Unit) {
        scaffoldState.title = "Receipts"
        scaffoldState.showTopBar = true
        scaffoldState.drawerEnabled = true
        scaffoldState.floatingActionButton = {
            FloatingActionButton(onClick = { isSheetOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddEditItemScreen(
                navController = navController,
                viewModel = viewModel,
                onFinish = { isSheetOpen = false }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search by name") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedStore,
                    onValueChange = {},
                    label = { Text("Filter by store") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store) },
                            onClick = {
                                viewModel.updateselectedStore(store)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoadingReceipts -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text("No receipts saved")
                    }
                }
                else -> {
                    LazyColumn(state = listState) {
                        grouped.forEach { (monthYear, receipts) ->
                            stickyHeader {
                                Text(
                                    text = monthYear,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(vertical = 8.dp)
                                )
                            }

                            items(receipts) { item ->
                                ReceiptItemRow(
                                    item = item,
                                    cacheStatus = imageCacheStatus[item.id],
                                    pendingUpload = item.pendingUpload,
                                    uploadProgress = item.uploadProgress,
                                    downloadProgress = imageDownloadProgress[item.id]
                                ) {
                                    navController.navigate("detail/${item.id}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
