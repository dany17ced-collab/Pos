package com.tuempresa.possystem.domain.boleta

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.VentaEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera la boleta/comprobante de una venta ya registrada, como PDF tamaño
 * A4 normal — pensado para compartir por WhatsApp/email o imprimir en una
 * impresora común. Usa los datos de ConfiguracionTiendaEntity (logo, nombre,
 * eslogan, dirección, etc.): es un ajuste único que aplica a todas las
 * boletas, no se pide nada de esto por cada venta.
 *
 * Para la impresora térmica de tienda (58mm) se usa un formato distinto —
 * ver ImpresoraTermicaBluetooth, que no genera PDF sino comandos ESC/POS.
 */
object GeneradorBoletaPdf {

    private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

    private val Terracota = DeviceRgb(0xD9, 0x8E, 0x73)
    private val CarbonTexto = DeviceRgb(0x22, 0x1B, 0x1D)

    private fun moneda(valor: Double): String = "S/ %.2f".format(Locale("es", "PE"), valor)

    private fun etiquetaMetodoPago(metodo: MetodoPago): String = when (metodo) {
        MetodoPago.EFECTIVO -> "Efectivo"
        MetodoPago.TARJETA -> "Tarjeta"
        MetodoPago.TRANSFERENCIA -> "Transferencia"
        MetodoPago.QR -> "QR"
        MetodoPago.MIXTO -> "Mixto"
    }

    fun generar(
        archivoDestino: File,
        configuracion: ConfiguracionTiendaEntity?,
        venta: VentaEntity,
        detalles: List<DetalleVentaEntity>
    ) {
        val writer = PdfWriter(archivoDestino)
        val pdf = PdfDocument(writer)
        val doc = Document(pdf, PageSize.A4)
        doc.setMargins(40f, 40f, 40f, 40f)

        try {
            encabezado(doc, configuracion)
            datosVenta(doc, venta)
            detalleProductos(doc, detalles)
            totales(doc, venta)
            piePagina(doc, configuracion)
        } finally {
            doc.close()
        }
    }

    private fun encabezado(doc: Document, configuracion: ConfiguracionTiendaEntity?) {
        val rutaLogo = configuracion?.rutaLogo
        if (rutaLogo != null && File(rutaLogo).exists()) {
            try {
                val logo = Image(ImageDataFactory.create(rutaLogo))
                val anchoMaximo = 120f
                if (logo.imageWidth > anchoMaximo) {
                    val escala = anchoMaximo / logo.imageWidth
                    logo.scale(escala, escala)
                }
                logo.setHorizontalAlignment(HorizontalAlignment.CENTER)
                doc.add(logo)
            } catch (e: Exception) {
                // Logo corrupto o ilegible: se omite y se sigue con el texto, la boleta no debe fallar por esto.
            }
        }

        val nombreTienda = configuracion?.nombreTienda?.takeIf { it.isNotBlank() } ?: "Mi Tienda"
        doc.add(
            Paragraph(nombreTienda)
                .setFontSize(20f)
                .setBold()
                .setFontColor(CarbonTexto)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6f)
                .setMarginBottom(0f)
        )

        configuracion?.eslogan?.takeIf { it.isNotBlank() }?.let {
            doc.add(
                Paragraph(it)
                    .setFontSize(11f)
                    .setItalic()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(2f)
                    .setMarginBottom(0f)
            )
        }

        val lineaContacto = listOfNotNull(
            configuracion?.direccion?.takeIf { it.isNotBlank() },
            configuracion?.telefono?.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            configuracion?.ruc?.takeIf { it.isNotBlank() }?.let { "RUC: $it" }
        ).joinToString("  •  ")

        if (lineaContacto.isNotBlank()) {
            doc.add(
                Paragraph(lineaContacto)
                    .setFontSize(9f)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6f)
            )
        }

        doc.add(
            Paragraph(" ")
                .setBorderBottom(com.itextpdf.layout.borders.SolidBorder(Terracota, 1.5f))
                .setMarginTop(10f)
                .setMarginBottom(14f)
        )
    }

    private fun datosVenta(doc: Document, venta: VentaEntity) {
        val folioTexto = venta.folio?.toString()?.padStart(6, '0') ?: venta.id.take(8)

        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        tabla.addCell(
            Cell().add(Paragraph("Boleta N° $folioTexto").setBold().setFontSize(13f))
                .setBorder(null)
        )
        tabla.addCell(
            Cell().add(Paragraph(formatoFechaHora.format(Date(venta.fecha))).setFontSize(10f))
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
        )
        doc.add(tabla)
        doc.add(Paragraph(" ").setFontSize(6f))
    }

    private fun detalleProductos(doc: Document, detalles: List<DetalleVentaEntity>) {
        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(3.2f, 0.8f, 1.2f, 1.2f))).useAllAvailableWidth()

        listOf("Producto", "Cant.", "P. unit.", "Subtotal").forEach { titulo ->
            tabla.addHeaderCell(
                Cell().add(Paragraph(titulo).setFontSize(10f).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(CarbonTexto)
                    .setPadding(6f)
                    .setTextAlignment(if (titulo == "Producto") TextAlignment.LEFT else TextAlignment.RIGHT)
            )
        }

        detalles.forEach { linea ->
            tabla.addCell(
                Cell().add(Paragraph(linea.nombreProducto).setFontSize(10f))
                    .setPadding(6f)
                    .setBorder(null)
            )
            tabla.addCell(
                Cell().add(Paragraph(linea.cantidad.toString()).setFontSize(10f))
                    .setPadding(6f)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.RIGHT)
            )
            tabla.addCell(
                Cell().add(Paragraph(moneda(linea.precioUnitario)).setFontSize(10f))
                    .setPadding(6f)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.RIGHT)
            )
            tabla.addCell(
                Cell().add(Paragraph(moneda(linea.subtotal)).setFontSize(10f))
                    .setPadding(6f)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.RIGHT)
            )
        }
        doc.add(tabla)
    }

    private fun filaTotal(tabla: Table, etiqueta: String, valor: String, resaltar: Boolean = false) {
        val tamano = if (resaltar) 13f else 10.5f
        tabla.addCell(
            Cell().add(Paragraph(etiqueta).setFontSize(tamano).apply { if (resaltar) setBold() })
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
        )
        tabla.addCell(
            Cell().add(Paragraph(valor).setFontSize(tamano).apply { if (resaltar) setBold() })
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
        )
    }

    private fun totales(doc: Document, venta: VentaEntity) {
        doc.add(Paragraph(" ").setFontSize(4f))
        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .setWidth(UnitValue.createPercentValue(45f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)

        filaTotal(tabla, "Subtotal", moneda(venta.subtotal))
        if (venta.descuento > 0) filaTotal(tabla, "Descuento", "-${moneda(venta.descuento)}")
        if (venta.impuestos > 0) filaTotal(tabla, "Impuestos", moneda(venta.impuestos))
        filaTotal(tabla, "TOTAL", moneda(venta.total), resaltar = true)
        filaTotal(tabla, "Pago (${etiquetaMetodoPago(venta.metodoPago)})", moneda(venta.montoRecibido ?: venta.total))
        if (venta.metodoPago == MetodoPago.EFECTIVO && venta.cambio != null && venta.cambio > 0) {
            filaTotal(tabla, "Cambio", moneda(venta.cambio))
        }
        doc.add(tabla)
    }

    private fun piePagina(doc: Document, configuracion: ConfiguracionTiendaEntity?) {
        val mensaje = configuracion?.piePagina?.takeIf { it.isNotBlank() } ?: "¡Gracias por su compra!"
        doc.add(
            Paragraph(mensaje)
                .setFontSize(11f)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30f)
        )
    }
}
