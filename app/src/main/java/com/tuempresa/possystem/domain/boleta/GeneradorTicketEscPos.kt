package com.tuempresa.possystem.domain.boleta

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.VentaEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Arma los bytes ESC/POS de la boleta para una impresora térmica de 58mm
 * (paño útil ≈ 32 columnas en fuente normal). Esto NO es un PDF: son
 * comandos crudos que la propia impresora interpreta para imprimir texto,
 * cortar papel, etc. Se envían tal cual por Bluetooth SPP.
 */
object GeneradorTicketEscPos {

    private const val ANCHO_COLUMNAS = 32

    private val ESC = 0x1B.toByte()
    private val GS = 0x1D.toByte()

    private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

    private fun moneda(valor: Double): String = "S/ %.2f".format(Locale("es", "PE"), valor)

    private fun etiquetaMetodoPago(metodo: MetodoPago): String = when (metodo) {
        MetodoPago.EFECTIVO -> "Efectivo"
        MetodoPago.TARJETA -> "Tarjeta"
        MetodoPago.TRANSFERENCIA -> "Transferencia"
        MetodoPago.QR -> "QR"
        MetodoPago.MIXTO -> "Mixto"
    }

    fun generar(
        configuracion: ConfiguracionTiendaEntity?,
        venta: VentaEntity,
        detalles: List<DetalleVentaEntity>
    ): ByteArray {
        val salida = ByteArrayOutputStream()

        salida.write(byteArrayOf(ESC, 0x40)) // inicializar impresora

        // --- Logo (si existe y es legible como bitmap) ---
        configuracion?.rutaLogo?.let { ruta ->
            val archivo = File(ruta)
            if (archivo.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(ruta)
                    if (bitmap != null) {
                        centrar(salida)
                        salida.write(comandosImagen(bitmap))
                        salida.write(byteArrayOf(0x0A))
                    }
                } catch (e: Exception) {
                    // Si el logo no se puede rasterizar, se omite: el ticket debe imprimirse igual.
                }
            }
        }

        // --- Encabezado ---
        centrar(salida)
        negritaOn(salida)
        textoGrandeOn(salida)
        linea(salida, configuracion?.nombreTienda?.takeIf { it.isNotBlank() } ?: "Mi Tienda")
        textoGrandeOff(salida)
        negritaOff(salida)

        configuracion?.eslogan?.takeIf { it.isNotBlank() }?.let { linea(salida, it) }
        // La descripción puede ser más larga que las 32 columnas del papel térmico,
        // así que se envuelve en varias líneas en vez de dejarla desbordar/cortarse.
        configuracion?.descripcion?.takeIf { it.isNotBlank() }?.let {
            envolverTexto(it, ANCHO_COLUMNAS).forEach { fila -> linea(salida, fila) }
        }
        configuracion?.direccion?.takeIf { it.isNotBlank() }?.let { linea(salida, it) }
        configuracion?.telefono?.takeIf { it.isNotBlank() }?.let { linea(salida, "Tel: $it") }
        configuracion?.ruc?.takeIf { it.isNotBlank() }?.let { linea(salida, "RUC: $it") }

        separador(salida)

        val folioTexto = venta.folio?.toString()?.padStart(6, '0') ?: "000000"
        val tipo = venta.tipoComprobante
        val etiquetaComprobante = when (tipo) {
            com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA -> "Factura"
            com.tuempresa.possystem.data.local.entity.TipoComprobante.BOLETA -> "Boleta"
            // Nota de venta: documento interno sin validez tributaria, nunca debe
            // decir "Boleta" ni "Factura" en ningún lado del ticket.
            com.tuempresa.possystem.data.local.entity.TipoComprobante.NOTA_VENTA -> "Nota de venta"
        }
        val esFactura = tipo == com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA
        negritaOn(salida)
        linea(salida, "$etiquetaComprobante N° $folioTexto")
        negritaOff(salida)
        linea(salida, formatoFechaHora.format(Date(venta.fecha)))

        // Datos del cliente, si se capturaron.
        if (esFactura) {
            venta.clienteRazonSocial?.takeIf { it.isNotBlank() }?.let { linea(salida, "Cliente: $it") }
            venta.clienteRuc?.takeIf { it.isNotBlank() }?.let { linea(salida, "RUC: $it") }
        } else {
            venta.clienteNombre?.takeIf { it.isNotBlank() }?.let { linea(salida, "Cliente: $it") }
            venta.clienteDocumento?.takeIf { it.isNotBlank() }?.let { linea(salida, "DNI: $it") }
        }

        separador(salida)

        // --- Detalle de productos ---
        alinearIzquierda(salida)
        detalles.forEach { producto ->
            linea(salida, producto.nombreProducto)
            val cantidadPrecio = "${producto.cantidad} x ${moneda(producto.precioUnitario)}"
            linea(salida, filaDosColumnas(cantidadPrecio, moneda(producto.subtotal)))
        }

        separador(salida)

        // --- Totales ---
        linea(salida, filaDosColumnas("Subtotal", moneda(venta.subtotal)))
        if (venta.descuento > 0) linea(salida, filaDosColumnas("Descuento", "-${moneda(venta.descuento)}"))
        if (venta.impuestos > 0) linea(salida, filaDosColumnas("Impuestos", moneda(venta.impuestos)))

        negritaOn(salida)
        textoGrandeOn(salida)
        linea(salida, filaDosColumnas("TOTAL", moneda(venta.total), anchoMitad = true))
        textoGrandeOff(salida)
        negritaOff(salida)

        linea(salida, filaDosColumnas("Pago (${etiquetaMetodoPago(venta.metodoPago)})", moneda(venta.montoRecibido ?: venta.total)))
        if (venta.metodoPago == MetodoPago.EFECTIVO && venta.cambio != null && venta.cambio > 0) {
            linea(salida, filaDosColumnas("Cambio", moneda(venta.cambio)))
        }

        separador(salida)

        centrar(salida)
        linea(salida, configuracion?.piePagina?.takeIf { it.isNotBlank() } ?: "¡Gracias por su compra!")

        // QR pequeño con el ID de la venta: permite escanear la boleta directo
        // al hacer un cambio/devolución, sin teclear el folio a mano.
        GeneradorQr.generar("$PREFIJO_QR_VENTA${venta.id}", tamanoPx = 180)?.let { bitmapQr ->
            try {
                salida.write(byteArrayOf(0x0A))
                centrar(salida)
                salida.write(comandosImagen(bitmapQr))
                linea(salida, "Escanea para cambios")
            } catch (e: Exception) {
                // Si el QR no se puede imprimir, el ticket sigue sin él.
            }
        }

        // Advertencia legal de plazo/condiciones de cambios y devoluciones,
        // editable en Ajustes; solo se imprime si el dueño la configuró.
        configuracion?.politicaCambios?.takeIf { it.isNotBlank() }?.let {
            envolverTexto("⚠️ $it", ANCHO_COLUMNAS).forEach { fila -> linea(salida, fila) }
        }

        // QR de redes sociales, fijo — configurado una sola vez en Ajustes.
        configuracion?.linkRedesSociales?.takeIf { it.isNotBlank() }?.let { link ->
            GeneradorQr.generar(link, tamanoPx = 180)?.let { bitmapQr ->
                try {
                    salida.write(byteArrayOf(0x0A))
                    centrar(salida)
                    salida.write(comandosImagen(bitmapQr))
                    linea(salida, "Síguenos en nuestras redes")
                } catch (e: Exception) {
                    // Si el QR no se puede imprimir, el ticket sigue sin él.
                }
            }
        }

        // Espacio final + corte de papel (GS V 1 = corte parcial, soportado por la mayoría de 58mm)
        salida.write(byteArrayOf(0x0A, 0x0A, 0x0A))
        salida.write(byteArrayOf(GS, 0x56, 0x01))

        return salida.toByteArray()
    }

    // ---- Comandos ESC/POS básicos ----

    private fun centrar(salida: ByteArrayOutputStream) {
        salida.write(byteArrayOf(ESC, 0x61, 0x01))
    }

    private fun alinearIzquierda(salida: ByteArrayOutputStream) {
        salida.write(byteArrayOf(ESC, 0x61, 0x00))
    }

    private fun negritaOn(salida: ByteArrayOutputStream) {
        salida.write(byteArrayOf(ESC, 0x45, 0x01))
    }

    private fun negritaOff(salida: ByteArrayOutputStream) {
        salida.write(byteArrayOf(ESC, 0x45, 0x00))
    }

    private fun textoGrandeOn(salida: ByteArrayOutputStream) {
        salida.write(byteArrayOf(GS, 0x21, 0x11)) // doble ancho + doble alto
    }

    private fun textoGrandeOff(salida: ByteArrayOutputStream) {
        salida.write(byteArrayOf(GS, 0x21, 0x00))
    }

    /** Envuelve texto por palabras para que no exceda [ancho] columnas por línea. */
    private fun envolverTexto(texto: String, ancho: Int): List<String> {
        val palabras = texto.trim().split(Regex("\\s+"))
        val filas = mutableListOf<String>()
        var actual = StringBuilder()
        for (palabra in palabras) {
            val candidata = if (actual.isEmpty()) palabra else "${actual} $palabra"
            if (candidata.length > ancho) {
                if (actual.isNotEmpty()) filas.add(actual.toString())
                actual = StringBuilder(palabra)
            } else {
                actual = StringBuilder(candidata)
            }
        }
        if (actual.isNotEmpty()) filas.add(actual.toString())
        return filas
    }

    private fun linea(salida: ByteArrayOutputStream, texto: String) {
        salida.write(texto.toByteArray(charset("ISO-8859-1")))
        salida.write(byteArrayOf(0x0A))
    }

    private fun separador(salida: ByteArrayOutputStream) {
        linea(salida, "-".repeat(ANCHO_COLUMNAS))
    }

    /** Arma una fila "etiqueta ......... valor" ajustada al ancho de la impresora (32 columnas). */
    private fun filaDosColumnas(izquierda: String, derecha: String, anchoMitad: Boolean = false): String {
        val ancho = if (anchoMitad) ANCHO_COLUMNAS / 2 else ANCHO_COLUMNAS
        val espacioDisponible = ancho - izquierda.length - derecha.length
        return if (espacioDisponible > 0) {
            izquierda + " ".repeat(espacioDisponible) + derecha
        } else {
            // Si no cabe en una sola línea, se corta el texto izquierdo con "…" para no romper el ticket.
            val maxIzquierda = (ancho - derecha.length - 1).coerceAtLeast(1)
            izquierda.take(maxIzquierda) + " " + derecha
        }
    }

    /**
     * Convierte un bitmap a comandos ESC/POS de imagen rasterizada (modo GS v 0),
     * escalando al ancho de una impresora de 58mm (384 puntos a 203dpi).
     */
    private fun comandosImagen(original: Bitmap): ByteArray {
        val anchoMaximoPuntos = 384
        val bitmap = if (original.width > anchoMaximoPuntos) {
            val alturaEscalada = (original.height * anchoMaximoPuntos / original.width)
            Bitmap.createScaledBitmap(original, anchoMaximoPuntos, alturaEscalada, true)
        } else {
            original
        }

        val ancho = bitmap.width
        val alto = bitmap.height
        val bytesPorFila = (ancho + 7) / 8

        val salida = ByteArrayOutputStream()
        salida.write(byteArrayOf(GS, 0x76, 0x30, 0x00))
        salida.write(byteArrayOf((bytesPorFila and 0xFF).toByte(), ((bytesPorFila shr 8) and 0xFF).toByte()))
        salida.write(byteArrayOf((alto and 0xFF).toByte(), ((alto shr 8) and 0xFF).toByte()))

        val umbralGris = 160
        for (y in 0 until alto) {
            var bufferByte = 0
            var bitsEnBuffer = 0
            for (x in 0 until ancho) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val gris = (r + g + b) / 3
                val bitNegro = if (gris < umbralGris) 1 else 0

                bufferByte = (bufferByte shl 1) or bitNegro
                bitsEnBuffer++
                if (bitsEnBuffer == 8) {
                    salida.write(bufferByte)
                    bufferByte = 0
                    bitsEnBuffer = 0
                }
            }
            if (bitsEnBuffer > 0) {
                bufferByte = bufferByte shl (8 - bitsEnBuffer)
                salida.write(bufferByte)
            }
        }
        return salida.toByteArray()
    }
}
