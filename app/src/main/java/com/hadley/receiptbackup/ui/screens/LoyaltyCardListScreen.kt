package com.hadley.receiptbackup.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.repository.LoyaltyCardViewModel
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import com.hadley.receiptbackup.ui.components.LoyaltyCardRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var draggingCardId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    val canReorder = searchQuery.isBlank()
    val displayedCards = if (canReorder) cards else cards.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

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
                val dragModifier = if (canReorder) {
                    Modifier.pointerInput(card.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingCardId = card.id
                                dragOffset = 0f
                            },
                            onDragEnd = {
                                draggingCardId = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingCardId = null
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y
                                val layoutInfo = listState.layoutInfo
                                var currentIndex = displayedCards.indexOfFirst { it.id == card.id }
                                if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                var currentItem = layoutInfo.visibleItemsInfo.firstOrNull {
                                    it.index == currentIndex
                                } ?: return@detectDragGesturesAfterLongPress

                                var swapped = true
                                var safety = 0
                                while (swapped && safety < 12) {
                                    swapped = false
                                    val draggedCenter = currentItem.offset + dragOffset + currentItem.size / 2
                                    val prevItem = layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.index == currentIndex - 1
                                    }
                                    val nextItem = layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.index == currentIndex + 1
                                    }

                                    if (prevItem != null && draggedCenter < prevItem.offset + prevItem.size / 2) {
                                        viewModel.reorderCards(context, currentIndex, currentIndex - 1)
                                        dragOffset -= (prevItem.offset - currentItem.offset)
                                        currentIndex -= 1
                                        currentItem = prevItem
                                        swapped = true
                                    } else if (nextItem != null && draggedCenter > nextItem.offset + nextItem.size / 2) {
                                        viewModel.reorderCards(context, currentIndex, currentIndex + 1)
                                        dragOffset -= (nextItem.offset - currentItem.offset)
                                        currentIndex += 1
                                        currentItem = nextItem
                                        swapped = true
                                    }
                                    safety += 1
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }

                LoyaltyCardRow(
                    card = card,
                    onClick = { navController.navigate("cardDetail/${card.id}") },
                    showDragHandle = canReorder,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer(
                            translationY = if (isDragging) dragOffset else 0f,
                            scaleX = if (isDragging) 1.06f else 1f,
                            scaleY = if (isDragging) 1.06f else 1f
                        )
                        .then(dragModifier)
                )
            }
        }
    }
}
