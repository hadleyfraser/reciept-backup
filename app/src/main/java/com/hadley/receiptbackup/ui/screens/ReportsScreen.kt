package com.hadley.receiptbackup.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState

@Composable
@Suppress("UNUSED_PARAMETER")
fun ReportsScreen(
    navController: NavController,
    viewModel: ReceiptItemViewModel,
    paddingValues: PaddingValues
) {
    val scaffoldState = LocalAppScaffoldState.current

    LaunchedEffect(Unit) {
        scaffoldState.title = "Reports"
        scaffoldState.showTopBar = true
        scaffoldState.drawerEnabled = true
        scaffoldState.floatingActionButton = {}
    }

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
