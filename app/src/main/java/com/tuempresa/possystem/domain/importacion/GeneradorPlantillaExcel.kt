package com.tuempresa.possystem.domain.importacion

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera la plantilla .xlsx de ejemplo que el usuario descarga desde
 * "Importar Productos > Descargar Ejemplo", con los encabezados exactos que
 * espera [com.tuempresa.possystem.presentation.importar.ImportarProductosViewModel]
 * y una fila de muestra.
 *
 * Mismo enfoque que ExportadorExcel.kt (zip + XML a mano, sin Apache POI por
 * su incompatibilidad conocida con Android); se mantiene como generador
 * independiente y minimalista en vez de reutilizar el privado de
 * ExportadorExcel para no acoplar ni arriesgar ese exportador ya en producción.
 */
object GeneradorPlantillaExcel {

    private val encabezados = listOf(
        "Nombre", "SKU", "Codigo de barras", "Precio de venta", "Precio de compra", "Stock inicial"
    )
    private val filaEjemplo = listOf("Polo básico", "POLO-001", "7751234567890", "25.00", "12.00", "10")

    fun generar(archivoDestino: File) {
        ZipOutputStream(archivoDestino.outputStream()).use { zip ->
            escribir(zip, "[Content_Types].xml", contentTypesXml())
            escribir(zip, "_rels/.rels", relsRaizXml())
            escribir(zip, "xl/workbook.xml", workbookXml())
            escribir(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            escribir(zip, "xl/worksheets/sheet1.xml", sheetXml())
        }
    }

    private fun escribir(zip: ZipOutputStream, nombre: String, contenido: String) {
        zip.putNextEntry(ZipEntry(nombre))
        zip.write(contenido.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun relsRaizXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Productos" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

    private fun columnaExcel(indice: Int): String {
        var i = indice
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun filaXml(numeroFila: Int, valores: List<String>): String {
        val celdas = valores.mapIndexed { i, valor ->
            val ref = "${columnaExcel(i)}$numeroFila"
            "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escaparXml(valor)}</t></is></c>"
        }.joinToString("")
        return "<row r=\"$numeroFila\">$celdas</row>"
    }

    private fun sheetXml(): String {
        val filas = filaXml(1, encabezados) + filaXml(2, filaEjemplo)
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>$filas</sheetData>
</worksheet>"""
    }

    private fun escaparXml(texto: String): String = texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
