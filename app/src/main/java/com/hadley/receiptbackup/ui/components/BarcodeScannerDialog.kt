package com.hadley.receiptbackup.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.hadley.receiptbackup.utils.BarcodeScanResult
import com.hadley.receiptbackup.utils.supportedBarcodeOptions
import java.util.concurrent.Executors
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalGetImage
@Composable
fun BarcodeScannerDialog(
    onResult: (BarcodeScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasResult by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Scan Barcode") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )

                if (!hasPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Camera permission is required to scan barcodes.")
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val formats = remember {
                            supportedBarcodeOptions.map { it.mlKitFormat }
                        }
                        val options = remember(formats) {
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray())
                                .build()
                        }
                        val scanner = remember { BarcodeScanning.getClient(options) }

                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { previewView ->
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                val preview = Preview.Builder().build().apply {
                                    setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    if (hasResult) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    val mediaImage = imageProxy.image
                                    if (mediaImage == null) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                            val rawValue = barcode?.rawValue
                                            val formatName = supportedBarcodeOptions
                                                .firstOrNull { it.mlKitFormat == barcode?.format }
                                                ?.format
                                                ?.name
                                            if (!rawValue.isNullOrBlank() && !formatName.isNullOrBlank()) {
                                                hasResult = true
                                                onResult(BarcodeScanResult(formatName, rawValue))
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                }

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            analysis
                                        )
                                    } catch (e: Exception) {
                                        cameraProvider.unbindAll()
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            }
                        )

                        Text(
                            text = "Align the barcode within the frame",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(PaddingValues(bottom = 24.dp))
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
