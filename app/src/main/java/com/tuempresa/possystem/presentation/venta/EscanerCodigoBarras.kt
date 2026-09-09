package com.tuempresa.possystem.presentation.venta

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Vista de cámara en vivo que detecta códigos de barras y llama a [onCodigoDetectado]
 * con el primer código válido que encuentre. El llamador es responsable de dejar de
 * mostrar este composable tras recibir un resultado (para no seguir escaneando).
 *
 * Requiere permiso de cámara ya concedido; usar junto con SolicitarPermisoCamara.
 */
@Composable
fun EscanerCodigoBarras(
    modifier: Modifier = Modifier,
    onCodigoDetectado: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var yaDetectado by remember { mutableStateOf(false) }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val opciones = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_QR_CODE
                        )
                        .build()
                    val scanner = BarcodeScanning.getClient(opciones)
                    val executor = Executors.newSingleThreadExecutor()

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !yaDetectado) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { codigos ->
                                    val valor = codigos.firstOrNull()?.rawValue
                                    if (valor != null && !yaDetectado) {
                                        yaDetectado = true
                                        onCodigoDetectado(valor)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        // Cámara no disponible o ya vinculada; se ignora, la UI
                        // seguirá mostrando el buscador manual como alternativa.
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Marco visual simple para guiar dónde apuntar el código de barras
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        ) {
            Text(
                text = "Apunta al código de barras",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** true si el permiso de cámara ya está concedido. */
fun tienePermisoCamara(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
