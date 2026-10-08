package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.PosSlate900
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarcodeScannerModal(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    feedbackMessage: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var manualBarcodeInput by remember { mutableStateOf("") }
    var flashEnabled by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Check camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            cameraError = "Camera permission was denied. You can still type barcode manually below."
        }
    }

    // Automatically request permission upon opening modal if not yet granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Toggle torch/flash when flashEnabled changes
    LaunchedEffect(flashEnabled, activeCamera) {
        try {
            activeCamera?.cameraControl?.enableTorch(flashEnabled)
        } catch (_: Exception) {}
    }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Debounce barcode detections
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("barcode_scanner_modal"),
            color = PosSlate900,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Camera Barcode Scanner",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = if (hasCameraPermission) "Live Camera Active" else "Camera Permission Required",
                                color = if (hasCameraPermission) Color(0xFF00E676) else Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row {
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = { flashEnabled = !flashEnabled },
                                modifier = Modifier.testTag("scanner_flashlight_button")
                            ) {
                                Icon(
                                    imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                                    contentDescription = "Flashlight",
                                    tint = if (flashEnabled) Color(0xFFFFD54F) else Color(0xFF94A3B8)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Viewfinder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && cameraError == null) {
                        // Live CameraX Preview View
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val cameraExecutor = Executors.newSingleThreadExecutor()

                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()

                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        val multiFormatReader = MultiFormatReader()

                                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            try {
                                                val buffer = imageProxy.planes[0].buffer
                                                val data = ByteArray(buffer.remaining())
                                                buffer.get(data)
                                                val width = imageProxy.width
                                                val height = imageProxy.height

                                                val source = PlanarYUVLuminanceSource(
                                                    data, width, height,
                                                    0, 0, width, height, false
                                                )
                                                val bitmap = BinaryBitmap(HybridBinarizer(source))

                                                try {
                                                    val rawResult = multiFormatReader.decodeWithState(bitmap)
                                                    if (rawResult != null && rawResult.text.isNotBlank()) {
                                                        val now = System.currentTimeMillis()
                                                        if (rawResult.text != lastScannedCode || (now - lastScannedTime) > 2000L) {
                                                            lastScannedCode = rawResult.text
                                                            lastScannedTime = now
                                                            ContextCompat.getMainExecutor(ctx).execute {
                                                                onBarcodeScanned(rawResult.text)
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                    // No barcode in this frame
                                                }
                                            } catch (e: Exception) {
                                                // Buffer read exception
                                            } finally {
                                                imageProxy.close()
                                            }
                                        }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                        activeCamera = camera
                                    } catch (e: Exception) {
                                        cameraError = "Camera init notice: ${e.localizedMessage ?: "Preview unavailable"}"
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!hasCameraPermission) {
                        // Permission Request View
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera Permission Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Allow camera access to scan barcodes on products",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("grant_camera_permission_button")
                            ) {
                                Text("Enable Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Fallback message if hardware camera not accessible in environment
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Camera Hardware Notice",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Use manual input or test presets below",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Targeting Reticle Overlay & Scanning Laser
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 150.dp)
                            .border(2.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                    ) {
                        // Animated Scanning Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = (laserOffset % 150f).dp)
                                .background(Color(0xFFFF1744))
                        )
                    }

                    Text(
                        text = "Align barcode within green frame",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }

                // Feedback Message (Scanned Product or Not Found)
                if (!feedbackMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (feedbackMessage.contains("not found", ignoreCase = true))
                                Color(0xFF7F1D1D)
                            else
                                Color(0xFF064E3B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Barcode Input Field
                OutlinedTextField(
                    value = manualBarcodeInput,
                    onValueChange = { manualBarcodeInput = it },
                    label = { Text("Or Type Barcode Number") },
                    placeholder = { Text("e.g. 8901234001") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                    },
                    trailingIcon = {
                        if (manualBarcodeInput.isNotBlank()) {
                            Button(
                                onClick = {
                                    onBarcodeScanned(manualBarcodeInput.trim())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("barcode_submit_button")
                            ) {
                                Text("Scan", fontSize = 12.sp)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (manualBarcodeInput.isNotBlank()) {
                                onBarcodeScanned(manualBarcodeInput.trim())
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("barcode_manual_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick One-Tap Test Presets
                Text(
                    text = "Quick Barcode Simulation Triggers:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "8901234001" to "Wai Wai Noodles",
                        "8901234002" to "DDC Milk",
                        "8901234003" to "Tokla Tea",
                        "8901234004" to "Basmati Rice 5kg",
                        "8901234006" to "Current Noodles"
                    )

                    presets.forEach { (code, name) ->
                        AssistChip(
                            onClick = { onBarcodeScanned(code) },
                            label = {
                                Text(
                                    text = "$name ($code)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            modifier = Modifier.testTag("barcode_chip_$code")
                        )
                    }
                }
            }
        }
    }
}
