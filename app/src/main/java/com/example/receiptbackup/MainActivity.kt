package com.example.receiptbackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.receiptbackup.data.repository.ReceiptItemViewModel
import com.example.receiptbackup.navigation.AppNavHost
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val receiptItemViewModel: ReceiptItemViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                AppNavHost(receiptItemViewModel = receiptItemViewModel)
            }
        }
    }
}
