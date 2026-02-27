package com.hadley.receiptbackup.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val supportedFormats = listOf(
    BarcodeFormatOption("QR Code", BarcodeFormat.QR_CODE, Barcode.FORMAT_QR_CODE),
    BarcodeFormatOption("EAN-13", BarcodeFormat.EAN_13, Barcode.FORMAT_EAN_13),
    BarcodeFormatOption("EAN-8", BarcodeFormat.EAN_8, Barcode.FORMAT_EAN_8),
    BarcodeFormatOption("UPC-A", BarcodeFormat.UPC_A, Barcode.FORMAT_UPC_A),
    BarcodeFormatOption("UPC-E", BarcodeFormat.UPC_E, Barcode.FORMAT_UPC_E),
    BarcodeFormatOption("Code 39", BarcodeFormat.CODE_39, Barcode.FORMAT_CODE_39),
    BarcodeFormatOption("Code 93", BarcodeFormat.CODE_93, Barcode.FORMAT_CODE_93),
    BarcodeFormatOption("Code 128", BarcodeFormat.CODE_128, Barcode.FORMAT_CODE_128),
    BarcodeFormatOption("PDF417", BarcodeFormat.PDF_417, Barcode.FORMAT_PDF417),
    BarcodeFormatOption("Aztec", BarcodeFormat.AZTEC, Barcode.FORMAT_AZTEC),
    BarcodeFormatOption("Data Matrix", BarcodeFormat.DATA_MATRIX, Barcode.FORMAT_DATA_MATRIX),
    BarcodeFormatOption("ITF-14", BarcodeFormat.ITF, Barcode.FORMAT_ITF)
)

val supportedBarcodeOptions: List<BarcodeFormatOption> = supportedFormats

fun barcodeFormatFromName(name: String): BarcodeFormat? {
    return supportedFormats.firstOrNull { it.format.name == name }?.format
}

fun barcodeOptionFromName(name: String): BarcodeFormatOption? {
    return supportedFormats.firstOrNull { it.format.name == name }
}

fun barcodeFormatNameFromMlKit(format: Int): String? {
    return supportedFormats.firstOrNull { it.mlKitFormat == format }?.format?.name
}

fun createBarcodeBitmap(value: String, formatName: String, width: Int, height: Int): ImageBitmap? {
    val format = barcodeFormatFromName(formatName) ?: return null
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 50)
        val matrix = MultiFormatWriter().encode(value, format, width, height, hints)
        matrix.toBitmap().asImageBitmap()
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun createBarcodeDimensions(is2d: Boolean): Pair<Int, Int> {
    return if (is2d) {
        600 to 600
    } else {
        900 to 300
    }
}

suspend fun scanBarcodeFromImage(context: Context, uri: Uri): BarcodeScanResult? {
    val image = InputImage.fromFilePath(context, uri)
    val formats = supportedFormats.map { it.mlKitFormat }
    if (formats.isEmpty()) return null
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray())
        .build()
    val scanner = BarcodeScanning.getClient(options)
    return suspendCancellableCoroutine { continuation ->
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                val type = barcode?.format?.let { barcodeFormatNameFromMlKit(it) }
                val value = barcode?.rawValue
                continuation.resume(
                    if (!type.isNullOrBlank() && !value.isNullOrBlank()) {
                        BarcodeScanResult(type, value)
                    } else {
                        null
                    }
                )
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}

private fun BitMatrix.toBitmap(): Bitmap {
    val width = width
    val height = height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

data class BarcodeFormatOption(
    val label: String,
    val format: BarcodeFormat,
    val mlKitFormat: Int
) {
    val is2d: Boolean
        get() = format == BarcodeFormat.QR_CODE ||
            format == BarcodeFormat.DATA_MATRIX ||
            format == BarcodeFormat.AZTEC ||
            format == BarcodeFormat.PDF_417
}

data class BarcodeScanResult(
    val typeName: String,
    val value: String
)
