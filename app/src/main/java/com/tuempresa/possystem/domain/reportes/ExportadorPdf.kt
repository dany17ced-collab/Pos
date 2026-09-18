package com.tuempresa.possystem.domain.reportes

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera el reporte detallado en formato PDF: mismo contenido que el Excel,
 * pero pensado para leer/imprimir rápido en una sola pasada.
 */
object ExportadorPdf {

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
    private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

    private val Terracota = DeviceRgb(0xD9, 0x8E, 0x73)
    private val CarbonTexto = DeviceRgb(0x22, 0x1B, 0x1D)
    private val GrisClaro = DeviceRgb(0xF0, 0xEB, 0xE7)

    private fun moneda(valor: Double): String = "S/ %,.2f".format(Locale("es", "PE"), valor)

    fun exportar(datos: DatosReporte, archivoDestino: File) {
        val writer = PdfWriter(archivoDestino)
        val pdf = PdfDocument(writer)
        val doc = Document(pdf)
        doc.setMargins(30f, 30f, 30f, 30f)

        try {
            titulo(doc, datos)
            resumenGeneral(doc, datos)
            ventasPorDia(doc, datos)
            productosMasVendidos(doc, datos)
            cortesDeCaja(doc, datos)
            inventario(doc, datos)
        } finally {
            doc.close() // cierra en cascada: Document -> PdfDocument -> PdfWriter
        }
    }

    private fun titulo(doc: Document, datos: DatosReporte) {
        val periodoTexto = if (datos.periodo == PeriodoReporte.SEMANAL) "Semanal" else "Mensual"
        doc.add(
            Paragraph("Reporte $periodoTexto de Ventas e Inventario")
                .setFontSize(18f)
                .setBold()
                .setFontColor(CarbonTexto)
        )
        doc.add(
            Paragraph("Periodo: ${formatoFecha.format(Date(datos.desde))} al ${formatoFecha.format(Date(datos.hasta))}")
                .setFontSize(10f)
        )
        doc.add(
            Paragraph("Generado: ${formatoFechaHora.format(Date(datos.generadoEn))}  —  Por: ${datos.generadoPor}")
                .setFontSize(9f)
                .setFontColor(ColorConstants.GRAY)
        )
        doc.add(Paragraph(" ").setFontSize(4f))
    }

    private fun subtitulo(doc: Document, texto: String) {
        doc.add(
            Paragraph(texto)
                .setFontSize(13f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(Terracota)
                .setPadding(5f)
                .setMarginTop(12f)
        )
    }

    private fun filaEtiquetaValor(tabla: Table, etiqueta: String, valor: String, resaltar: Boolean = false) {
        val celdaEtiqueta = Cell().add(Paragraph(etiqueta).setFontSize(10f))
            .setBorder(null)
        val celdaValor = Cell().add(
            Paragraph(valor).setFontSize(10f).apply { if (resaltar) setBold() }
        ).setBorder(null).setTextAlignment(TextAlignment.RIGHT)

        tabla.addCell(celdaEtiqueta)
        tabla.addCell(celdaValor)
    }

    private fun resumenGeneral(doc: Document, datos: DatosReporte) {
        subtitulo(doc, "Resumen de ventas")

        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        filaEtiquetaValor(tabla, "Total vendido", moneda(datos.totalVentas), resaltar = true)
        filaEtiquetaValor(tabla, "  Efectivo", moneda(datos.totalEfectivo))
        filaEtiquetaValor(tabla, "  Tarjeta", moneda(datos.totalTarjeta))
        filaEtiquetaValor(tabla, "  Transferencia / QR", moneda(datos.totalTransferencia))
        filaEtiquetaValor(tabla, "Descuentos otorgados", moneda(datos.totalDescuentos))
        filaEtiquetaValor(tabla, "Impuestos cobrados", moneda(datos.totalImpuestos))
        filaEtiquetaValor(tabla, "Margen bruto (ganancia)", moneda(datos.margenBrutoTotal), resaltar = true)
        filaEtiquetaValor(tabla, "Número de transacciones", datos.numeroTransacciones.toString())
        filaEtiquetaValor(tabla, "Ventas anuladas", datos.numeroVentasAnuladas.toString())
        filaEtiquetaValor(tabla, "Ticket promedio", moneda(datos.ticketPromedio))
        doc.add(tabla)

        subtitulo(doc, "Resumen de inventario")
        val tablaInv = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        filaEtiquetaValor(tablaInv, "Valor del inventario (costo)", moneda(datos.valorTotalInventarioCosto))
        filaEtiquetaValor(tablaInv, "Valor del inventario (venta)", moneda(datos.valorTotalInventarioVenta))
        filaEtiquetaValor(tablaInv, "Productos con stock bajo el mínimo", datos.productosStockBajo.toString(), resaltar = datos.productosStockBajo > 0)
        doc.add(tablaInv)
    }

    private fun encabezadoCelda(texto: String): Cell {
        return Cell().add(Paragraph(texto).setFontSize(9f).setBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(CarbonTexto)
            .setPadding(4f)
    }

    private fun celda(texto: String, alinearDerecha: Boolean = false, colorTexto: com.itextpdf.kernel.colors.Color? = null): Cell {
        val parrafo = Paragraph(texto).setFontSize(9f)
        if (colorTexto != null) parrafo.setFontColor(colorTexto)
        return Cell().add(parrafo)
            .setPadding(4f)
            .setTextAlignment(if (alinearDerecha) TextAlignment.RIGHT else TextAlignment.LEFT)
    }

    private fun ventasPorDia(doc: Document, datos: DatosReporte) {
        if (datos.ventasPorDia.isEmpty()) return
        subtitulo(doc, "Ventas por día")

        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(1.3f, 0.8f, 1f, 1f, 1f, 1f))).useAllAvailableWidth()
        listOf("Fecha", "N° ventas", "Total", "Efectivo", "Tarjeta", "Transf./QR").forEach {
            tabla.addHeaderCell(encabezadoCelda(it))
        }
        datos.ventasPorDia.forEach { dia ->
            tabla.addCell(celda(formatoFecha.format(Date(dia.fecha))))
            tabla.addCell(celda(dia.numeroVentas.toString(), true))
            tabla.addCell(celda(moneda(dia.totalVentas), true))
            tabla.addCell(celda(moneda(dia.totalEfectivo), true))
            tabla.addCell(celda(moneda(dia.totalTarjeta), true))
            tabla.addCell(celda(moneda(dia.totalTransferencia), true))
        }
        doc.add(tabla)
    }

    private fun productosMasVendidos(doc: Document, datos: DatosReporte) {
        if (datos.productosMasVendidos.isEmpty()) return
        subtitulo(doc, "Productos más vendidos")

        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1f, 1f))).useAllAvailableWidth()
        listOf("Producto", "Unidades", "Total vendido", "Margen").forEach {
            tabla.addHeaderCell(encabezadoCelda(it))
        }
        datos.productosMasVendidos.forEach { p ->
            tabla.addCell(celda(p.nombreProducto))
            tabla.addCell(celda(p.unidadesVendidas.toString(), true))
            tabla.addCell(celda(moneda(p.totalVendido), true))
            tabla.addCell(celda(moneda(p.margenTotal), true))
        }
        doc.add(tabla)
    }

    private fun cortesDeCaja(doc: Document, datos: DatosReporte) {
        if (datos.cortesDeCaja.isEmpty()) return
        subtitulo(doc, "Cortes de caja")

        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(1.4f, 0.8f, 1f, 1f, 1f))).useAllAvailableWidth()
        listOf("Fecha", "Tipo", "Total ventas", "Efect. esperado", "Diferencia").forEach {
            tabla.addHeaderCell(encabezadoCelda(it))
        }
        datos.cortesDeCaja.forEach { corte ->
            tabla.addCell(celda(formatoFechaHora.format(Date(corte.fechaCorte))))
            tabla.addCell(celda(if (corte.tipo.name == "Z") "Z" else "X"))
            tabla.addCell(celda(moneda(corte.totalVentas), true))
            tabla.addCell(celda(moneda(corte.efectivoEsperado), true))
            tabla.addCell(celda(corte.diferencia?.let { moneda(it) } ?: "—", true))
        }
        doc.add(tabla)
    }

    private fun inventario(doc: Document, datos: DatosReporte) {
        if (datos.inventarioActual.isEmpty()) return
        subtitulo(doc, "Inventario actual")

        val tabla = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1.3f, 0.8f, 1f, 1f, 0.9f))).useAllAvailableWidth()
        listOf("Producto", "Categoría", "Stock", "P. venta", "Valor (venta)", "Alerta").forEach {
            tabla.addHeaderCell(encabezadoCelda(it))
        }
        datos.inventarioActual.forEach { p ->
            tabla.addCell(celda("${p.nombre}\n${p.sku}"))
            tabla.addCell(celda(p.categoria))
            tabla.addCell(celda(p.stockActual.toString(), true))
            tabla.addCell(celda(moneda(p.precioVenta), true))
            tabla.addCell(celda(moneda(p.valorVenta), true))
            tabla.addCell(
                celda(if (p.bajoMinimo) "BAJO" else "", colorTexto = if (p.bajoMinimo) ColorConstants.RED else null)
            )
        }
        doc.add(tabla)
    }
}
