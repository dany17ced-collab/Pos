package com.tuempresa.possystem.presentation.inventario

/** Tallas estándar de ropa manejadas por la tienda: 2 a 16, de dos en dos. */
val TALLAS_DISPONIBLES = listOf("2", "4", "6", "8", "10", "12", "14", "16")

/** Un escalón de precio en captura, antes de guardarse. */
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

/**
 * Plantilla de precios de una talla: se define UNA SOLA VEZ por producto y
 * se aplica igual a todos los colores (mismo precio por talla sin importar
 * el color, ej. talla 2 y talla 16 cuestan distinto entre sí, pero el mismo
 * precio en cualquier color). El stock NO va aquí — el stock sí es distinto
 * por color y se captura aparte en StockPorColor.
 */
data class TallaEnCaptura(
    val talla: String,
    val escalones: List<EscalonEnCaptura> = escalonesPorDefecto()
)

/**
 * Stock de una talla específica dentro de un color concreto. Cada color
 * tiene su propio inventario por talla, aunque comparta los precios de la
 * plantilla de tallas.
 */
data class StockTallaEnCaptura(
    val talla: String,
    val stockTexto: String = ""
)

/** Un color agregado, con el stock de cada talla marcada (precios vienen de la plantilla). */
data class ColorEnCaptura(
    val id: String, // id local temporal, solo para poder editar/quitar en la UI
    val color: String,
    val stockPorTalla: Map<String, StockTallaEnCaptura> // clave = talla
)
