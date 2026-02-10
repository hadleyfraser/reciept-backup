package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.hadley.receiptbackup.R
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.AppDrawerScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: ReceiptItemViewModel) {
    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()

    AppDrawerScaffold(
        navController = navController,
        title = "Reports",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Reports coming soon")
        }
    }
}
