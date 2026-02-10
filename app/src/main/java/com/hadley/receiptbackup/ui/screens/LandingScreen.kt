package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.R
import com.hadley.receiptbackup.ui.components.GoogleSignInButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LandingScreen(navController: NavController, viewModel: ReceiptItemViewModel = viewModel()) {
    val context = LocalContext.current as Activity
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Auto redirect if already signed in
    LaunchedEffect(Unit) {
        if (GoogleAuthManager.getCurrentUser() != null) {
            try {
                withContext(Dispatchers.IO) {
                    viewModel.loadCachedReceipts(context)
                }
                navController.navigate("main") {
                    popUpTo("landing") { inclusive = true }
                }
            } catch (e: Exception) {
                Log.e("LandingScreen", "Failed to load cached receipts", e)
            }
        } else {
            isLoading = false
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(bottom = 32.dp)
                )

                Text(text = "Receipt Backup", style = MaterialTheme.typography.headlineLarge)

                Spacer(modifier = Modifier.height(32.dp))

                GoogleSignInButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            val user = GoogleAuthManager.signInWithGoogle(context)
                            if (user != null) {
                                viewModel.clearItems()
                                viewModel.loadReceiptsFromFirestore(context)
                                navController.navigate("main") {
                                    popUpTo("landing") { inclusive = true }
                                }
                            } else {
                                isLoading = false
                                Log.e("LandingScreen", "Firebase sign-in failed")
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
