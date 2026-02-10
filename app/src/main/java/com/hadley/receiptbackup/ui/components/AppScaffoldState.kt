package com.hadley.receiptbackup.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class AppScaffoldState {
    var title by mutableStateOf("")
    var showTopBar by mutableStateOf(true)
    var drawerEnabled by mutableStateOf(true)
    var floatingActionButton by mutableStateOf<@Composable () -> Unit>({})
}

val LocalAppScaffoldState = compositionLocalOf<AppScaffoldState> {
    error("LocalAppScaffoldState not provided")
}

