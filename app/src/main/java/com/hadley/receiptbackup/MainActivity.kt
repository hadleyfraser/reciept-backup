package com.hadley.receiptbackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.hadley.receiptbackup.data.local.SettingsDataStore
import com.hadley.receiptbackup.data.local.ThemeMode
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.navigation.AppNavHost
import com.hadley.receiptbackup.ui.theme.AppTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val receiptItemViewModel: ReceiptItemViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            val context = LocalContext.current
            val themeMode by SettingsDataStore.themeModeFlow(context)
                .collectAsState(initial = ThemeMode.SYSTEM)

            AppTheme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost(receiptItemViewModel = receiptItemViewModel)
                }
            }
        }
    }
}
