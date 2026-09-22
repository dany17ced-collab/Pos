package com.tuempresa.possystem.domain.importacion

import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/**
 * Lee la primera hoja de un archivo .xlsx sin ninguna librería externa (Apache
 * POI no funciona de forma confiable en Android — ver ExportadorExcel.kt).
 * Un .xlsx es un .zip con XML adentro: se extraen `xl/sharedStrings.xml` (las
 * cadenas de texto que Excel/Sheets deduplican) y `xl/worksheets/sheet1.xml`
 * (las celdas en sí, que referencian esas cadenas por índice o traen su
 * propio texto inline), y se combinan en una matriz de filas de texto plano.
 *
 * Soporta los dos formatos de celda de texto que emiten distintos programas:
 * - `t="s"`: la celda guarda un índice hacia sharedStrings.xml (Excel/Sheets estándar).
 * - `t="inlineStr"`: la celda trae el texto directamente (formato usado por
 *   nuestro propio ExportadorExcel.kt).
 * Las celdas numéricas (sin atributo `t`, o `t="n"`) se devuelven como el
 * texto crudo del `<v>`.
 */
object LectorExcel {

    /** Devuelve la primera hoja como filas de celdas de texto (vacío = celda vacía). */
    fun leerPrimeraHoja(entrada: InputStream): List<List<String>> {
        var sharedStrings: List<String> = emptyList()
        var filasHoja: List<List<CeldaCruda>>? = null

        ZipInputStream(entrada).use { zip ->
            var entry = zip.nextEntry
            var bytesHoja: ByteArray? = null
            var bytesSharedStrings: ByteArray? = null

            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> bytesSharedStrings = zip.readBytes()
                    entry.name.matches(Regex("xl/worksheets/sheet1\\.xml")) -> bytesHoja = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            bytesSharedStrings?.let { sharedStrings = parsearSharedStrings(it.inputStream()) }
            bytesHoja?.let { filasHoja = parsearHoja(it.inputStream()) }
        }

        val filas = filasHoja ?: return emptyList()
        return filas.map { fila ->
            fila.map { celda ->
                when (celda.tipo) {
                    TipoCelda.COMPARTIDA -> celda.valor.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                    TipoCelda.DIRECTA -> celda.valor
                }
            }
        }
    }

    private enum class TipoCelda { COMPARTIDA, DIRECTA }
    private data class CeldaCruda(val columna: Int, val tipo: TipoCelda, val valor: String)

    /** xl/sharedStrings.xml: lista de <si><t>texto</t></si> en orden, indexada por posición. */
    private fun parsearSharedStrings(input: InputStream): List<String> {
        val resultado = mutableListOf<String>()
        val textoActual = StringBuilder()
        var dentroDeSi = false
        var dentroDeT = false

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                when (qName) {
                    "si" -> { dentroDeSi = true; textoActual.clear() }
                    "t" -> dentroDeT = true
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                if (dentroDeSi && dentroDeT && ch != null) textoActual.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (qName) {
                    "t" -> dentroDeT = false
                    "si" -> { resultado.add(textoActual.toString()); dentroDeSi = false }
                }
            }
        }

        crearParserSax(handler).parse(InputSource(input))
        return resultado
    }

    /** xl/worksheets/sheet1.xml: filas <row><c r="A1" t="s"><v>0</v></c>...</row>. */
    private fun parsearHoja(input: InputStream): List<List<CeldaCruda>> {
        val filas = mutableListOf<MutableList<CeldaCruda>>()
        var filaActual: MutableList<CeldaCruda>? = null
        var columnaActual = -1
        var tipoActual = TipoCelda.DIRECTA
        var dentroDeValor = false
        var dentroDeTextoInline = false
        val valorActual = StringBuilder()

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                when (qName) {
                    "row" -> filaActual = mutableListOf<CeldaCruda>().also { filas.add(it) }
                    "c" -> {
                        val ref = attributes?.getValue("r") ?: ""
                        columnaActual = referenciaAColumna(ref)
                        tipoActual = if (attributes?.getValue("t") == "s") TipoCelda.COMPARTIDA else TipoCelda.DIRECTA
                        valorActual.clear()
                    }
                    "v" -> dentroDeValor = true
                    "t" -> dentroDeTextoInline = true
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                if ((dentroDeValor || dentroDeTextoInline) && ch != null) valorActual.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (qName) {
                    "v" -> dentroDeValor = false
                    "t" -> dentroDeTextoInline = false
                    "c" -> {
                        if (columnaActual >= 0) {
                            filaActual?.add(CeldaCruda(columnaActual, tipoActual, valorActual.toString()))
                        }
                    }
                }
            }
        }

        crearParserSax(handler).parse(InputSource(input))

        // Rellena huecos de columnas vacías para que cada fila tenga celdas contiguas.
        return filas.map { fila ->
            val maxColumna = fila.maxOfOrNull { it.columna } ?: -1
            (0..maxColumna).map { col ->
                fila.find { it.columna == col } ?: CeldaCruda(col, TipoCelda.DIRECTA, "")
            }
        }
    }

    private fun crearParserSax(handler: DefaultHandler): org.xml.sax.XMLReader =
        SAXParserFactory.newInstance().newSAXParser().xmlReader.apply {
            contentHandler = handler
        }

    /** "C7" -> 2 (columna índice 0-based); ignora el número de fila. */
    private fun referenciaAColumna(ref: String): Int {
        var columna = 0
        for (ch in ref) {
            if (ch.isLetter()) {
                columna = columna * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else break
        }
        return columna - 1
    }
}
