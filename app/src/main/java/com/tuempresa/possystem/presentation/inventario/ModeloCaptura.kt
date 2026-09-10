package com.tuempresa.possystem.presentation.inventario

/** Tallas estándar de ropa manejadas por la tienda: 2 a 16, de dos en dos. */
val TALLAS_DISPONIBLES = listOf("2", "4", "6", "8", "10", "12", "14", "16")

/** Un escalón de precio en captura, antes de guardarse (aplica a todas las variantes del producto). */
data class EscalonEnCaptura(
    val etiqueta: String,
    val cantidadMinima: Int,
    val precioTexto: String = ""
)

fun escalonesPorDefecto(): List<EscalonEnCaptura> = listOf(
    EscalonEnCaptura("Unidad", 1),
    EscalonEnCaptura("1/4", 3),
    EscalonEnCaptura("Media", 6),
    EscalonEnCaptura("Docena", 12)
)

/** Una combinación de color + tallas marcadas con su stock, capturada antes de guardar. */
data class ColorEnCaptura(
    val id: String, // id local temporal, solo para poder editar/quitar en la UI
    val color: String,
    val stockPorTalla: Map<String, Int> // solo contiene las tallas marcadas para este color
)
