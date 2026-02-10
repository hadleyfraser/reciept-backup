package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.hadley.receiptbackup.R
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.local.SettingsDataStore
import com.hadley.receiptbackup.data.local.ThemeMode
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.AppDrawerScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: ReceiptItemViewModel) {
    val context = LocalContext.current
    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()
    val themeMode by SettingsDataStore.themeModeFlow(context)
        .collectAsState(initial = ThemeMode.SYSTEM)
    var expanded by remember { mutableStateOf(false) }

    AppDrawerScaffold(
        navController = navController,
        title = "Settings",
        actions = {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
        },
        drawerContent = { closeDrawer ->
            NavigationDrawerItem(
                label = { Text("Logout") },
                selected = false,
                onClick = {
                    closeDrawer()
                    coroutineScope.launch {
                        viewModel.clearItems()
                        GoogleAuthManager.signOut(activity, viewModel)
                        navController.navigate("landing") {
                            popUpTo("list") { inclusive = true }
                        }
                        launch(Dispatchers.IO) {
                            viewModel.clearLocalCache(activity)
                            viewModel.clearCachedImages(activity)

                            val imageLoader = Coil.imageLoader(activity)
                            imageLoader.diskCache?.clear()
                            imageLoader.memoryCache?.clear()
                        }
                    }
                },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                },
                colors = NavigationDrawerItemDefaults.colors()
            )
        }
    ) { paddingValues: PaddingValues ->
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
                        .menuAnchor()
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
        }
    }
}
