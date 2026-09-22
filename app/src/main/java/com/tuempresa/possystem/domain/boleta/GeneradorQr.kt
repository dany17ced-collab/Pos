package com.tuempresa.possystem.domain.boleta

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Genera bitmaps de código QR a partir de texto plano. Se usa para dos cosas
 * en la boleta: el QR del folio de venta (para escanear al hacer un cambio,
 * sin tener que teclear el folio) y el QR de redes sociales de la tienda
 * (fijo, configurado una sola vez en Ajustes).
 */
object GeneradorQr {

    fun generar(contenido: String, tamanoPx: Int = 300): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val matriz = writer.encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx)
            val bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.RGB_565)
            for (x in 0 until tamanoPx) {
                for (y in 0 until tamanoPx) {
                    bitmap.setPixel(x, y, if (matriz.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            // Contenido no codificable o cualquier otro fallo: la boleta debe
            // seguir generándose sin QR antes que fallar por completo.
            null
        }
    }
}

/** Prefijo usado para que el QR impreso en la boleta sea identificable al escanearlo en Cambios. */
const val PREFIJO_QR_VENTA = "POS-VENTA:"
