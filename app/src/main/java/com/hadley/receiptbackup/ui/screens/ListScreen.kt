package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.Coil
import com.hadley.receiptbackup.R
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.ReceiptItemRow
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(navController: NavController, viewModel: ReceiptItemViewModel) {
    val items by viewModel.items.collectAsState()
    val isLoadingReceipts by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val listState = rememberLazyListState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    var selectedStore by remember { mutableStateOf("All") }
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
    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Receipts")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            viewModel.clearItems()
                            viewModel.clearLocalCache(activity)

                            Coil.imageLoader(activity).diskCache?.clear()

                            GoogleAuthManager.signOut(activity, viewModel) {
                                navController.navigate("landing") {
                                    popUpTo("list") { inclusive = true }
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isSheetOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
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
                                    selectedStore = store
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
                                            .background(Color.White)
                                            .padding(vertical = 8.dp)
                                    )
                                }

                                items(receipts) { item ->
                                    ReceiptItemRow(item = item) {
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
}
