package com.hadley.receiptbackup.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.model.LoyaltyCard
import com.hadley.receiptbackup.data.repository.LoyaltyCardViewModel
import com.hadley.receiptbackup.ui.components.BarcodeScannerDialog
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import com.hadley.receiptbackup.utils.BarcodeScanResult
import com.hadley.receiptbackup.utils.barcodeOptionFromName
import com.hadley.receiptbackup.utils.createBarcodeBitmap
import com.hadley.receiptbackup.utils.createBarcodeDimensions
import com.hadley.receiptbackup.utils.scanBarcodeFromImage
import com.hadley.receiptbackup.utils.supportedBarcodeOptions
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun AddEditLoyaltyCardScreen(
    navController: NavController,
    viewModel: LoyaltyCardViewModel,
    cardId: String?,
    paddingValues: PaddingValues
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scaffoldState = LocalAppScaffoldState.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val existingCard = cardId?.let { viewModel.getCardById(it) }
    var name by remember { mutableStateOf(existingCard?.name ?: "") }
    var notes by remember { mutableStateOf(existingCard?.notes ?: "") }
    var barcodeType by remember {
        mutableStateOf(existingCard?.barcodeType ?: supportedBarcodeOptions.first().format.name)
    }
    var barcodeValue by remember { mutableStateOf(existingCard?.barcodeValue ?: "") }
    var coverColor by remember { mutableStateOf(existingCard?.coverColor ?: defaultCoverColor) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    val barcodeOption = barcodeOptionFromName(barcodeType)
    val is2d = barcodeOption?.is2d ?: false
    val (barcodeWidth, barcodeHeight) = createBarcodeDimensions(is2d)
    val barcodeImage = remember(barcodeType, barcodeValue) {
        if (barcodeValue.isBlank()) {
            null
        } else {
            createBarcodeBitmap(barcodeValue, barcodeType, barcodeWidth, barcodeHeight)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isScanning = true
                val result = scanBarcodeFromImage(context, uri)
                handleScanResult(result, onResult = {
                    barcodeType = it.typeName
                    barcodeValue = it.value
                })
                isScanning = false
            }
        }
    }

    LaunchedEffect(cardId) {
        scaffoldState.title = if (existingCard == null) "Add Card" else "Edit Card"
        scaffoldState.showTopBar = true
        scaffoldState.drawerEnabled = true
        scaffoldState.floatingActionButton = {}
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onResult = { result ->
                barcodeType = result.typeName
                barcodeValue = result.value
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = coverColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { selectedColor ->
                coverColor = selectedColor
                showColorPicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Card name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            val selectedOption = barcodeOptionFromName(barcodeType)
            OutlinedTextField(
                readOnly = true,
                value = selectedOption?.label ?: barcodeType,
                onValueChange = {},
                label = { Text("Barcode type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                supportedBarcodeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            barcodeType = option.format.name
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = barcodeValue,
            onValueChange = { barcodeValue = it },
            label = { Text("Barcode value") },
            modifier = Modifier.fillMaxWidth()
        )

        if (barcodeValue.isNotBlank()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (barcodeImage != null) {
                    Image(
                        bitmap = barcodeImage,
                        contentDescription = "Barcode preview",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = barcodeValue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Barcode preview unavailable")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showScanner = true },
                enabled = !isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan")
            }
            Button(
                onClick = { imagePicker.launch("image/*") },
                enabled = !isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("From photo")
            }
        }

        Text(text = "Cover color", style = MaterialTheme.typography.titleSmall)
        Button(
            onClick = { showColorPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(coverColor), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick color")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    if (name.isBlank() || barcodeValue.isBlank()) {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Name and barcode value are required",
                            duration = SnackbarDuration.Short
                        )
                        return@launch
                    }
                    val card = if (existingCard == null) {
                        LoyaltyCard(
                            name = name.trim(),
                            notes = notes.trim(),
                            barcodeType = barcodeType,
                            barcodeValue = barcodeValue.trim(),
                            coverColor = coverColor
                        )
                    } else {
                        existingCard.copy(
                            name = name.trim(),
                            notes = notes.trim(),
                            barcodeType = barcodeType,
                            barcodeValue = barcodeValue.trim(),
                            coverColor = coverColor
                        )
                    }
                    if (existingCard == null) {
                        viewModel.addCard(context, card)
                    } else {
                        viewModel.updateCard(context, card)
                    }
                    navController.popBackStack()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (existingCard == null) "Save" else "Update")
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val hsv = remember { mutableStateOf(colorIntToHsv(initialColor)) }
    var alpha by remember { mutableFloatStateOf(colorIntToAlpha(initialColor)) }
    var hexText by remember { mutableStateOf(colorIntToHex(initialColor)) }
    var hexError by remember { mutableStateOf(false) }
    var isEditingHex by remember { mutableStateOf(false) }

    val currentColorInt = remember(hsv.value[0], hsv.value[1], hsv.value[2], alpha) {
        android.graphics.Color.HSVToColor((alpha * 255f).toInt(), hsv.value)
    }
    val currentColor = remember(currentColorInt) { Color(currentColorInt) }

    LaunchedEffect(currentColorInt, isEditingHex) {
        if (!isEditingHex) {
            hexText = colorIntToHex(currentColorInt)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select color", style = MaterialTheme.typography.titleMedium)

                ColorWheel(
                    hue = hsv.value[0],
                    saturation = hsv.value[1],
                    onChange = { hue, saturation ->
                        hsv.value = floatArrayOf(hue, saturation, hsv.value[2])
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                ColorSlider(
                    label = "Value",
                    value = hsv.value[2],
                    onValueChange = { value ->
                        hsv.value = floatArrayOf(hsv.value[0], hsv.value[1], value)
                    },
                    colors = listOf(Color.Black, hsvToColor(hsv.value, 1f))
                )

                ColorSlider(
                    label = "Alpha",
                    value = alpha,
                    onValueChange = { value ->
                        alpha = value
                    },
                    colors = listOf(Color.Transparent, currentColor),
                    showChecker = true
                )

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { value ->
                        val normalized = value.trim().uppercase()
                        hexText = normalized
                        val parsed = parseHexColor(normalized)
                        if (parsed != null) {
                            val parsedHsv = colorIntToHsv(parsed)
                            hsv.value = parsedHsv
                            alpha = colorIntToAlpha(parsed)
                            hexError = false
                        } else {
                            hexError = normalized.isNotBlank()
                        }
                    },
                    label = { Text("Hex") },
                    supportingText = {
                        if (hexError) {
                            Text("Use #RRGGBB or #AARRGGBB")
                        }
                    },
                    isError = hexError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isEditingHex = it.isFocused }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(currentColorInt) }) { Text("Use color") }
                }
            }
        }
    }
}

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bitmap = remember(size) {
        if (size.width == 0 || size.height == 0) {
            null
        } else {
            createColorWheelBitmap(size)
        }
    }
    val currentOnChange = rememberUpdatedState(onChange)

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val radius = min(size.width, size.height) / 2f
                    val distance = hypot(dx, dy).coerceAtMost(radius)
                    val sat = (distance / radius).coerceIn(0f, 1f)
                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0
                    currentOnChange.value(angle.toFloat(), sat)
                }
            }
    ) {
        if (bitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    canvas.drawImage(bitmap, Offset.Zero, androidx.compose.ui.graphics.Paint())
                }
            }
        }

        val radius = min(size.width, size.height) / 2f
        val selectorRadius = radius * saturation
        val angleRad = Math.toRadians(hue.toDouble())
        val selectorX = size.width / 2f + (cos(angleRad) * selectorRadius).toFloat()
        val selectorY = size.height / 2f + (sin(angleRad) * selectorRadius).toFloat()
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = Offset(selectorX, selectorY)
            )
            drawCircle(
                color = Color.Black,
                radius = 12f,
                center = Offset(selectorX, selectorY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    colors: List<Color>,
    showChecker: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(
                    if (showChecker) {
                        Modifier.drawBehind { drawCheckerboard() }
                    } else {
                        Modifier
                    }
                )
                .background(Brush.horizontalGradient(colors), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(fraction)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val thumbX = value.coerceIn(0f, 1f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = Offset(thumbX, size.height / 2f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 12f,
                    center = Offset(thumbX, size.height / 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }
    }
}

private fun createColorWheelBitmap(size: IntSize): ImageBitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size.width, size.height, android.graphics.Bitmap.Config.ARGB_8888)
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = min(size.width, size.height) / 2f
    val hsv = FloatArray(3)
    for (y in 0 until size.height) {
        val dy = y - centerY
        for (x in 0 until size.width) {
            val dx = x - centerX
            val distance = hypot(dx, dy)
            if (distance <= radius) {
                val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0
                hsv[0] = angle.toFloat()
                hsv[1] = (distance / radius).coerceIn(0f, 1f)
                hsv[2] = 1f
                bitmap.setPixel(x, y, android.graphics.Color.HSVToColor(hsv))
            } else {
                bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            }
        }
    }
    return bitmap.asImageBitmap()
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheckerboard() {
    val tileSize = 12f
    val rows = (size.height / tileSize).toInt()
    val cols = (size.width / tileSize).toInt()
    for (row in 0..rows) {
        for (col in 0..cols) {
            val isLight = (row + col) % 2 == 0
            val color = if (isLight) Color(0xFFCCCCCC) else Color(0xFF888888)
            val left = col * tileSize
            val top = row * tileSize
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(tileSize, tileSize)
            )
        }
    }
}

private fun colorIntToHsv(color: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color, hsv)
    return hsv
}

private fun colorIntToAlpha(color: Int): Float {
    return android.graphics.Color.alpha(color) / 255f
}

private fun hsvToColor(hsv: FloatArray, alpha: Float): Color {
    val colorInt = android.graphics.Color.HSVToColor((alpha * 255f).toInt(), hsv)
    return Color(colorInt)
}

private fun handleScanResult(
    result: BarcodeScanResult?,
    onResult: (BarcodeScanResult) -> Unit
) {
    if (result != null) {
        onResult(result)
    }
}

private fun parseHexColor(value: String): Int? {
    if (value.isBlank()) return null
    val cleaned = value.removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return try {
        val parsed = cleaned.toLong(16).toInt()
        if (cleaned.length == 6) {
            0xFF000000.toInt() or parsed
        } else {
            parsed
        }
    } catch (e: NumberFormatException) {
        null
    }
}

private fun colorIntToHex(color: Int): String {
    return "#" + color.toUInt().toString(16).padStart(8, '0').uppercase()
}

private const val defaultCoverColor: Int = 0xFF1565C0.toInt()
