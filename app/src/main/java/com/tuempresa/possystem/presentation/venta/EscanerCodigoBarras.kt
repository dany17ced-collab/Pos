package com.tuempresa.possystem.presentation.venta

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/** Qué tipo de código se espera escanear: cambia los formatos detectados y la guía visual en pantalla. */
enum class ModoEscaneo {
    CODIGO_BARRAS,
    QR
}

/** Forma y tamaño de la ventana nítida de encuadre, según el tipo de código esperado. */
private sealed class FormaRecuadro {
    data class Cuadrado(val ladoDp: Dp) : FormaRecuadro()
    data class Rectangulo(val anchoFraccion: Float, val altoDp: Dp) : FormaRecuadro()
}

/**
 * Calcula, en coordenadas del sensor (sin rotar), el rectángulo que
 * corresponde al recuadro de encuadre mostrado en pantalla — para usarlo
 * como [android.graphics.Rect] en [android.media.Image.setCropRect] y que
 * ML Kit analice solo esa región del frame, sin decodificar el frame
 * completo a Bitmap.
 *
 * Reproduce el mismo mapeo que hace PreviewView con ScaleType.FILL_CENTER
 * (la imagen se agranda manteniendo proporción hasta cubrir la vista,
 * recortando el sobrante) para ubicar el recuadro en la imagen ya orientada
 * como se ve en pantalla, y usa android.graphics.Matrix (rotación estándar
 * de Android, no álgebra manual) para deshacer la rotación de vuelta a las
 * coordenadas nativas del sensor que espera setCropRect.
 */
private fun recuadroEnCoordenadasDeSensor(
    anchoSensor: Int,
    altoSensor: Int,
    rotacionGrados: Int,
    forma: FormaRecuadro,
    anchoVistaPx: Int,
    altoVistaPx: Int,
    densidad: Float
): android.graphics.Rect {
    // Tamaño de la imagen ya orientada como se ve en pantalla (a 90°/270°
    // el sensor entrega ancho y alto intercambiados respecto a la vista).
    val anchoRotado = if (rotacionGrados == 90 || rotacionGrados == 270) altoSensor else anchoSensor
    val altoRotado = if (rotacionGrados == 90 || rotacionGrados == 270) anchoSensor else altoSensor

    if (anchoRotado == 0 || altoRotado == 0 || anchoVistaPx == 0 || altoVistaPx == 0) {
        return android.graphics.Rect(0, 0, anchoSensor, altoSensor)
    }

    val escala = maxOf(anchoVistaPx.toFloat() / anchoRotado, altoVistaPx.toFloat() / altoRotado)
    val recorteXVista = (anchoRotado * escala - anchoVistaPx) / 2f
    val recorteYVista = (altoRotado * escala - altoVistaPx) / 2f

    val cxVista = anchoVistaPx / 2f
    val cyVista = altoVistaPx / 2f
    val (wVista, hVista) = when (forma) {
        is FormaRecuadro.Cuadrado -> {
            val lado = forma.ladoDp.value * densidad
            lado to lado
        }
        is FormaRecuadro.Rectangulo -> {
            (anchoVistaPx * forma.anchoFraccion) to (forma.altoDp.value * densidad)
        }
    }

    fun aRotadoX(xVista: Float) = (xVista + recorteXVista) / escala
    fun aRotadoY(yVista: Float) = (yVista + recorteYVista) / escala

    val leftRotado = aRotadoX(cxVista - wVista / 2f).coerceIn(0f, anchoRotado.toFloat())
    val topRotado = aRotadoY(cyVista - hVista / 2f).coerceIn(0f, altoRotado.toFloat())
    val rightRotado = aRotadoX(cxVista + wVista / 2f).coerceIn(0f, anchoRotado.toFloat())
    val bottomRotado = aRotadoY(cyVista + hVista / 2f).coerceIn(0f, altoRotado.toFloat())

    // Deshace la rotación con android.graphics.Matrix (rotación estándar de
    // Android alrededor del centro de la imagen rotada, luego trasladada al
    // origen de la imagen sin rotar) en vez de fórmulas manuales por caso,
    // para no arriesgar un error de signo en 90/180/270 que sería silencioso.
    val rectRotado = android.graphics.RectF(leftRotado, topRotado, rightRotado, bottomRotado)
    val matriz = android.graphics.Matrix().apply {
        postRotate(-rotacionGrados.toFloat(), anchoRotado / 2f, altoRotado / 2f)
        // Tras rotar alrededor del centro de la imagen rotada, el resultado
        // queda centrado en (anchoRotado/2, altoRotado/2); se traslada para
        // que quede centrado en (anchoSensor/2, altoSensor/2), el sistema de
        // coordenadas real del sensor.
        postTranslate((anchoSensor - anchoRotado) / 2f, (altoSensor - altoRotado) / 2f)
    }
    val rectSensor = android.graphics.RectF()
    matriz.mapRect(rectSensor, rectRotado)

    val left = rectSensor.left.toInt().coerceIn(0, anchoSensor - 1)
    val top = rectSensor.top.toInt().coerceIn(0, altoSensor - 1)
    val right = rectSensor.right.toInt().coerceIn(left + 1, anchoSensor)
    val bottom = rectSensor.bottom.toInt().coerceIn(top + 1, altoSensor)
    return android.graphics.Rect(left, top, right, bottom)
}

/**
 * Vista nativa superpuesta al preview de la cámara: redibuja el mismo feed
 * desenfocado (blur real vía RenderEffect, disponible desde Android 12/API 31)
 * y le recorta un agujero nítido en la zona del recuadro de encuadre — el
 * resto de la pantalla queda borroso, igual que en los escáneres de apps como
 * WhatsApp o Google Lens.
 *
 * En Android < 12, donde RenderEffect no existe, se usa como respaldo un
 * oscurecido semitransparente del fondo (mismo recorte, sin blur real).
 */
private class VistaDesenfoqueConVentana(
    context: android.content.Context,
    private val previewView: PreviewView
) : View(context) {

    var forma: FormaRecuadro = FormaRecuadro.Rectangulo(0.85f, 120.dp)
        set(value) {
            field = value
            invalidate()
        }

    private val densidad: Float = context.resources.displayMetrics.density
    private val radioBlurPx = 30f

    private val renderNodeBorroso: RenderNode? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderNode("blurFondoEscaner") else null

    private val pinturaAgujero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val pinturaOscurecido = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(140, 0, 0, 0)
    }

    init {
        setWillNotDraw(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            renderNodeBorroso?.setRenderEffect(
                RenderEffect.createBlurEffect(radioBlurPx, radioBlurPx, Shader.TileMode.CLAMP)
            )
        }
    }

    private fun rutaAgujero(ancho: Int, alto: Int): Path {
        val path = Path()
        val cx = ancho / 2f
        val cy = alto / 2f
        when (val f = forma) {
            is FormaRecuadro.Cuadrado -> {
                val lado = f.ladoDp.value * densidad
                path.addRect(cx - lado / 2f, cy - lado / 2f, cx + lado / 2f, cy + lado / 2f, Path.Direction.CW)
            }
            is FormaRecuadro.Rectangulo -> {
                val w = ancho * f.anchoFraccion
                val h = f.altoDp.value * densidad
                path.addRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, Path.Direction.CW)
            }
        }
        return path
    }

    override fun onDraw(canvas: Canvas) {
        val ancho = width
        val alto = height
        if (ancho == 0 || alto == 0) return

        val agujero = rutaAgujero(ancho, alto)
        val nodo = renderNodeBorroso

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && nodo != null) {
            // Se vuelve a dibujar el mismo feed de cámara dentro de un
            // RenderNode con blur aplicado, y se recorta la ventana nítida.
            nodo.setPosition(0, 0, ancho, alto)
            val canvasNodo = nodo.beginRecording()
            previewView.draw(canvasNodo)
            nodo.endRecording()

            val capa = canvas.saveLayer(0f, 0f, ancho.toFloat(), alto.toFloat(), null)
            canvas.drawRenderNode(nodo)
            canvas.drawPath(agujero, pinturaAgujero)
            canvas.restoreToCount(capa)
        } else {
            // Respaldo sin blur real (Android < 12): oscurece el fondo y deja
            // transparente la ventana del recuadro.
            val capa = canvas.saveLayer(0f, 0f, ancho.toFloat(), alto.toFloat(), null)
            canvas.drawRect(0f, 0f, ancho.toFloat(), alto.toFloat(), pinturaOscurecido)
            canvas.drawPath(agujero, pinturaAgujero)
            canvas.restoreToCount(capa)
        }

        // Se redibuja en cada frame para que el blur quede sincronizado con la imagen en vivo.
        postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Al salir de la pantalla se detiene el redibujo continuo del blur;
        // postInvalidateOnAnimation() ya no tiene efecto sin ventana adjunta,
        // pero se libera explícitamente el RenderNode para no retenerlo.
        renderNodeBorroso?.discardDisplayList()
    }
}

/**
 * Vista de cámara en vivo que detecta códigos de barras o QR y llama a [onCodigoDetectado]
 * con el primer código válido que encuentre. El llamador es responsable de dejar de
 * mostrar este composable tras recibir un resultado (para no seguir escaneando).
 *
 * [modo] ajusta el escaneo al tipo de código esperado: en modo QR solo se detecta
 * QR y se muestra una ventana pequeña y cuadrada para encuadrarlo; en modo código
 * de barras se detectan los formatos de barra y se muestra una ventana rectangular
 * y ancha, apropiada para un código lineal. Fuera de esa ventana, la imagen de la
 * cámara se ve desenfocada de verdad (blur real en Android 12+, oscurecido como
 * respaldo en versiones anteriores), para que quede claro dónde enfocar.
 *
 * Requiere permiso de cámara ya concedido; usar junto con SolicitarPermisoCamara.
 */
@Composable
@OptIn(ExperimentalGetImage::class)
fun EscanerCodigoBarras(
    modifier: Modifier = Modifier,
    modo: ModoEscaneo = ModoEscaneo.CODIGO_BARRAS,
    onCodigoDetectado: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var yaDetectado by remember { mutableStateOf(false) }

    val forma = when (modo) {
        ModoEscaneo.QR -> FormaRecuadro.Cuadrado(220.dp)
        ModoEscaneo.CODIGO_BARRAS -> FormaRecuadro.Rectangulo(0.85f, 120.dp)
    }

    // El scanner de ML Kit y el executor del análisis se crean una sola vez
    // por composición de la pantalla (no en cada recomposición de `update`,
    // como ocurría antes) y se liberan al salir, para no acumular hilos ni
    // instancias del scanner cada vez que Compose recompone.
    val opciones = remember(modo) {
        when (modo) {
            ModoEscaneo.QR -> BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            ModoEscaneo.CODIGO_BARRAS -> BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39
                )
                .build()
        }
    }
    val scanner = remember(opciones) { BarcodeScanning.getClient(opciones) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val densidad = context.resources.displayMetrics.density

    androidx.compose.runtime.DisposableEffect(scanner, executor) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    // COMPATIBLE fuerza el uso de TextureView en vez de
                    // SurfaceView: es imprescindible para poder volver a
                    // dibujar este preview (previewView.draw(...)) dentro del
                    // RenderNode del overlay borroso — un SurfaceView renderiza
                    // en una superficie aparte y jamás aparecería ahí.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val overlayDesenfoque = VistaDesenfoqueConVentana(ctx, previewView).apply {
                    this.forma = forma
                }
                FrameLayout(ctx).apply {
                    addView(
                        previewView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    addView(
                        overlayDesenfoque,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    tag = Pair(previewView, overlayDesenfoque)
                }
            },
            update = { contenedor ->
                val parImplementaciones = contenedor.tag as Pair<*, *>
                val previewView = parImplementaciones.first as PreviewView
                val overlayDesenfoque = parImplementaciones.second as VistaDesenfoqueConVentana
                overlayDesenfoque.forma = forma

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (yaDetectado || mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        try {
                            val anchoVista = previewView.width
                            val altoVista = previewView.height
                            val rotacion = imageProxy.imageInfo.rotationDegrees
                            if (anchoVista > 0 && altoVista > 0) {
                                // Limita la región que ML Kit analiza al recuadro
                                // de encuadre en pantalla, mapeado a coordenadas
                                // del sensor. setCropRect es la vía oficial de
                                // CameraX para esto: acota el análisis sin pasar
                                // por Bitmap, que era el costo alto por frame.
                                imageProxy.setCropRect(
                                    recuadroEnCoordenadasDeSensor(
                                        anchoSensor = imageProxy.width,
                                        altoSensor = imageProxy.height,
                                        rotacionGrados = rotacion,
                                        forma = forma,
                                        anchoVistaPx = anchoVista,
                                        altoVistaPx = altoVista,
                                        densidad = densidad
                                    )
                                )
                            }
                            // ML Kit lee el plano YUV directo del sensor, ya
                            // acotado al cropRect de arriba: no se convierte a
                            // Bitmap en ningún momento, que era el costo más
                            // alto por frame (asignaba y procesaba una imagen
                            // completa solo para leer una fracción de ella).
                            val image = InputImage.fromMediaImage(mediaImage, rotacion)
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
                        } catch (e: Exception) {
                            // Frame descartable: si falla el análisis de este
                            // frame puntual, simplemente se ignora y se sigue
                            // con el siguiente.
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

        // Borde del recuadro y texto de ayuda, encima del desenfoque nativo.
        val colorMarco = com.tuempresa.possystem.presentation.theme.EcoPosColors.AcentoAmbar
        when (modo) {
            ModoEscaneo.QR -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(220.dp)
                        .border(2.dp, colorMarco)
                )
                Text(
                    text = "Encuadra el código QR dentro del recuadro",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                )
            }
            ModoEscaneo.CODIGO_BARRAS -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.85f)
                        .height(120.dp)
                        .border(2.dp, colorMarco)
                )
                Text(
                    text = "Apunta al código de barras dentro del recuadro",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                )
            }
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
