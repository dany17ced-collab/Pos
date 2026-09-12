package com.tuempresa.possystem.domain.reportes

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera el reporte detallado en formato Excel (.xlsx) sin depender de
 * ninguna librería externa: un .xlsx es simplemente un .zip con archivos XML
 * adentro (formato OOXML), así que se arma directamente con java.util.zip.
 *
 * Se evita Apache POI a propósito: su soporte en Android es problemático
 * (depende internamente de java.awt, que no existe en el runtime de Android,
 * y sus artefactos oficiales no están pensados para correr fuera de un JVM
 * de escritorio). Generar el XML a mano es más código, pero funciona siempre
 * igual, sin sorpresas de compatibilidad.
 *
 * El archivo resultante tiene una hoja por sección: Resumen, Ventas por día,
 * Más vendidos, Cortes de caja e Inventario. Se abre normal en Excel, Google
 * Sheets, Sheets de Android, WPS, LibreOffice, etc.
 */
object ExportadorExcel {

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
    private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

    fun exportar(datos: DatosReporte, archivoDestino: File) {
        val hojas = listOf(
            "Resumen" to filasResumen(datos),
            "Ventas por dia" to filasVentasPorDia(datos),
            "Mas vendidos" to filasMasVendidos(datos),
            "Cortes de caja" to filasCortesDeCaja(datos),
            "Inventario" to filasInventario(datos)
        )

        ZipOutputStream(archivoDestino.outputStream()).use { zip ->
            escribirEntrada(zip, "[Content_Types].xml", contentTypesXml(hojas.size))
            escribirEntrada(zip, "_rels/.rels", relsRaizXml())
            escribirEntrada(zip, "xl/workbook.xml", workbookXml(hojas.map { it.first }))
            escribirEntrada(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml(hojas.size))
            escribirEntrada(zip, "xl/styles.xml", stylesXml())
            hojas.forEachIndexed { i, (_, filas) ->
                escribirEntrada(zip, "xl/worksheets/sheet${i + 1}.xml", sheetXml(filas))
            }
        }
    }

    private fun escribirEntrada(zip: ZipOutputStream, nombre: String, contenido: String) {
        zip.putNextEntry(ZipEntry(nombre))
        zip.write(contenido.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    // ---------- Modelo simple de celdas/filas ----------

    private sealed class Celda {
        data class Texto(val valor: String, val negrita: Boolean = false) : Celda()
        data class Numero(val valor: Double) : Celda()
        data class Moneda(val valor: Double) : Celda()
    }

    private fun texto(v: String, negrita: Boolean = false) = Celda.Texto(v, negrita)
    private fun numero(v: Int) = Celda.Numero(v.toDouble())
    private fun moneda(v: Double) = Celda.Moneda(v)

    // ---------- Contenido de cada hoja ----------

    private fun filasResumen(datos: DatosReporte): List<List<Celda>> {
        val periodoTexto = if (datos.periodo == PeriodoReporte.SEMANAL) "Semanal" else "Mensual"
        val filas = mutableListOf<List<Celda>>()

        filas += listOf(texto("Reporte $periodoTexto de Ventas e Inventario", negrita = true))
        filas += listOf(texto(""))
        filas += listOf(texto("Periodo"), texto("${formatoFecha.format(Date(datos.desde))} al ${formatoFecha.format(Date(datos.hasta))}"))
        filas += listOf(texto("Generado"), texto(formatoFechaHora.format(Date(datos.generadoEn))))
        filas += listOf(texto("Generado por"), texto(datos.generadoPor))
        filas += listOf(texto(""))

        filas += listOf(texto("Resumen de ventas", negrita = true))
        filas += listOf(texto("Total vendido"), moneda(datos.totalVentas))
        filas += listOf(texto("  Efectivo"), moneda(datos.totalEfectivo))
        filas += listOf(texto("  Tarjeta"), moneda(datos.totalTarjeta))
        filas += listOf(texto("  Transferencia / QR"), moneda(datos.totalTransferencia))
        filas += listOf(texto("Descuentos otorgados"), moneda(datos.totalDescuentos))
        filas += listOf(texto("Impuestos cobrados"), moneda(datos.totalImpuestos))
        filas += listOf(texto("Margen bruto (ganancia)"), moneda(datos.margenBrutoTotal))
        filas += listOf(texto("Número de transacciones"), numero(datos.numeroTransacciones))
        filas += listOf(texto("Ventas anuladas"), numero(datos.numeroVentasAnuladas))
        filas += listOf(texto("Ticket promedio"), moneda(datos.ticketPromedio))
        filas += listOf(texto(""))

        filas += listOf(texto("Resumen de inventario", negrita = true))
        filas += listOf(texto("Valor del inventario (costo)"), moneda(datos.valorTotalInventarioCosto))
        filas += listOf(texto("Valor del inventario (venta)"), moneda(datos.valorTotalInventarioVenta))
        filas += listOf(texto("Productos con stock bajo el mínimo"), numero(datos.productosStockBajo))

        return filas
    }

    private fun filasVentasPorDia(datos: DatosReporte): List<List<Celda>> {
        val filas = mutableListOf<List<Celda>>()
        filas += listOf(
            texto("Fecha", true), texto("N° ventas", true), texto("Total", true),
            texto("Efectivo", true), texto("Tarjeta", true), texto("Transferencia/QR", true)
        )
        datos.ventasPorDia.forEach { dia ->
            filas += listOf(
                texto(formatoFecha.format(Date(dia.fecha))),
                numero(dia.numeroVentas),
                moneda(dia.totalVentas),
                moneda(dia.totalEfectivo),
                moneda(dia.totalTarjeta),
                moneda(dia.totalTransferencia)
            )
        }
        return filas
    }

    private fun filasMasVendidos(datos: DatosReporte): List<List<Celda>> {
        val filas = mutableListOf<List<Celda>>()
        filas += listOf(
            texto("Producto", true), texto("Unidades vendidas", true),
            texto("Total vendido", true), texto("Margen", true)
        )
        datos.productosMasVendidos.forEach { p ->
            filas += listOf(
                texto(p.nombreProducto),
                numero(p.unidadesVendidas),
                moneda(p.totalVendido),
                moneda(p.margenTotal)
            )
        }
        return filas
    }

    private fun filasCortesDeCaja(datos: DatosReporte): List<List<Celda>> {
        val filas = mutableListOf<List<Celda>>()
        filas += listOf(
            texto("Fecha", true), texto("Tipo", true), texto("N° transacciones", true),
            texto("Total ventas", true), texto("Efectivo esperado", true),
            texto("Efectivo contado", true), texto("Diferencia", true)
        )
        datos.cortesDeCaja.forEach { corte ->
            filas += listOf(
                texto(formatoFechaHora.format(Date(corte.fechaCorte))),
                texto(if (corte.tipo.name == "Z") "Z (cierre)" else "X (parcial)"),
                numero(corte.numeroTransacciones),
                moneda(corte.totalVentas),
                moneda(corte.efectivoEsperado),
                moneda(corte.efectivoContado ?: 0.0),
                moneda(corte.diferencia ?: 0.0)
            )
        }
        return filas
    }

    private fun filasInventario(datos: DatosReporte): List<List<Celda>> {
        val filas = mutableListOf<List<Celda>>()
        filas += listOf(
            texto("SKU", true), texto("Producto", true), texto("Categoría", true),
            texto("Stock actual", true), texto("Stock mínimo", true),
            texto("Precio compra", true), texto("Precio venta", true),
            texto("Valor (costo)", true), texto("Valor (venta)", true), texto("Alerta", true)
        )
        datos.inventarioActual.forEach { p ->
            filas += listOf(
                texto(p.sku),
                texto(p.nombre),
                texto(p.categoria),
                numero(p.stockActual),
                numero(p.stockMinimo),
                moneda(p.precioCompra),
                moneda(p.precioVenta),
                moneda(p.valorCosto),
                moneda(p.valorVenta),
                texto(if (p.bajoMinimo) "STOCK BAJO" else "")
            )
        }
        return filas
    }

    // ---------- Generación de XML (formato OOXML mínimo) ----------

    private fun contentTypesXml(numHojas: Int): String {
        val overrides = (1..numHojas).joinToString("") {
            "<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$overrides
</Types>"""
    }

    private fun relsRaizXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(nombresHojas: List<String>): String {
        val sheets = nombresHojas.mapIndexed { i, nombre ->
            "<sheet name=\"${escaparXml(nombre)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$sheets</sheets>
</workbook>"""
    }

    private fun workbookRelsXml(numHojas: Int): String {
        val rels = (1..numHojas).joinToString("") {
            "<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
</Relationships>"""
    }

    /** Dos estilos: 0 = normal, 1 = negrita. Los números y montos se escriben como texto plano (sin formato numérico especial) para máxima compatibilidad. */
    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
</fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" xfId="0" applyFont="1"/>
</cellXfs>
</styleSheet>"""

    private fun columnaExcel(indice: Int): String {
        var i = indice
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun sheetXml(filas: List<List<Celda>>): String {
        val filasXml = StringBuilder()
        filas.forEachIndexed { fIdx, fila ->
            val numFila = fIdx + 1
            filasXml.append("<row r=\"$numFila\">")
            fila.forEachIndexed { cIdx, celda ->
                val ref = "${columnaExcel(cIdx)}$numFila"
                when (celda) {
                    is Celda.Texto -> {
                        val estilo = if (celda.negrita) " s=\"1\"" else ""
                        filasXml.append("<c r=\"$ref\"$estilo t=\"inlineStr\"><is><t xml:space=\"preserve\">${escaparXml(celda.valor)}</t></is></c>")
                    }
                    is Celda.Numero -> {
                        filasXml.append("<c r=\"$ref\"><v>${formatearNumero(celda.valor)}</v></c>")
                    }
                    is Celda.Moneda -> {
                        // Se antepone "S/ " como texto para que se vea el símbolo de moneda
                        // sin depender de un numFmt personalizado (más compatible entre apps).
                        val texto = "S/ " + "%,.2f".format(Locale("es", "PE"), celda.valor)
                        filasXml.append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escaparXml(texto)}</t></is></c>")
                    }
                }
            }
            filasXml.append("</row>")
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>$filasXml</sheetData>
</worksheet>"""
    }

    private fun formatearNumero(valor: Double): String {
        return if (valor == valor.toLong().toDouble()) valor.toLong().toString() else valor.toString()
    }

    private fun escaparXml(texto: String): String {
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
