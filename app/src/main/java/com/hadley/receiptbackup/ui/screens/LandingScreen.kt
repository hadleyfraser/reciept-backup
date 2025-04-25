package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LandingScreen(navController: NavController, viewModel: ReceiptItemViewModel = viewModel()) {
    val context = LocalContext.current as Activity
    var isLoading by remember { mutableStateOf(true) }

    // Handle sign-in result
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                isLoading = true
                GoogleAuthManager.firebaseAuthWithGoogle(idToken) { user ->
                    if (user != null) {
                        viewModel.clearItems()
                        viewModel.loadReceiptsFromFirestore(context)
                        navController.navigate("list") {
                            popUpTo("landing") { inclusive = true }
                        }
                    } else {
                        isLoading = false
                        Log.e("LandingScreen", "Firebase sign-in failed")
                    }
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            Log.e("LandingScreen", "Google sign-in failed", e)
        }
    }

    // Auto redirect if already signed in
    LaunchedEffect(Unit) {
        if (GoogleAuthManager.getCurrentUser() != null) {
            try {
                withContext(Dispatchers.IO) {
                    viewModel.loadCachedReceipts(context)
                }
                navController.navigate("list") {
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
            Button(
                onClick = {
                    val signInIntent = GoogleAuthManager.getSignInClient(context).signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Sign in with Google")
            }
        }
    }
}
