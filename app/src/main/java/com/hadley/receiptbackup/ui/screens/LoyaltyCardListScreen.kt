package com.hadley.receiptbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.repository.LoyaltyCardViewModel
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import com.hadley.receiptbackup.ui.components.LoyaltyCardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyCardListScreen(
    navController: NavController,
    viewModel: LoyaltyCardViewModel,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val scaffoldState = LocalAppScaffoldState.current
    val listState = rememberLazyListState()

    val displayedCards = if (searchQuery.isBlank()) {
        cards
    } else {
        cards.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val reorderEnabled = searchQuery.isBlank()

    var draggingCardId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val currentDisplayedCards = rememberUpdatedState(displayedCards)

    LaunchedEffect(Unit) {
        viewModel.loadCards(context)
        scaffoldState.title = "Cards"
        scaffoldState.showTopBar = true
        scaffoldState.drawerEnabled = true
        scaffoldState.floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("cardEdit") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    }

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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (displayedCards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Text("No cards saved")
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                displayedCards,
                key = { _, card -> card.id }
            ) { _, card ->
                val isDragging = draggingCardId == card.id
                val dragModifier = if (reorderEnabled) {
                    Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                        .pointerInput(card.id, reorderEnabled) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingCardId = card.id
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingCardId = null
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggingCardId = null
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consumeAllChanges()
                                    if (draggingCardId != card.id) return@detectDragGesturesAfterLongPress
                                    dragOffsetY += dragAmount.y
                                    val currentItem = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == card.id } ?: return@detectDragGesturesAfterLongPress
                                    val currentCenter = currentItem.offset + dragOffsetY + currentItem.size / 2f
                                    val targetItem = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { currentCenter.toInt() in it.offset..(it.offset + it.size) }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val targetId = targetItem.key as? String ?: return@detectDragGesturesAfterLongPress
                                    if (targetId == card.id) return@detectDragGesturesAfterLongPress
                                    val currentList = currentDisplayedCards.value
                                    val fromCard = currentList.firstOrNull { it.id == card.id } ?: return@detectDragGesturesAfterLongPress
                                    val toCard = currentList.firstOrNull { it.id == targetId } ?: return@detectDragGesturesAfterLongPress
                                    viewModel.moveCard(context, fromCard.id, toCard.id)
                                    dragOffsetY += currentItem.offset - targetItem.offset
                                }
                            )
                        }
                } else {
                    Modifier
                }

                LoyaltyCardRow(
                    card = card,
                    onClick = { navController.navigate("cardDetail/${card.id}") },
                    modifier = dragModifier
                )
            }
        }
    }
}
