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
 * Captura de una talla específica dentro de un color: su stock y su propia
 * tabla de precios por cantidad (cada talla puede costar distinto y tener
 * distintos precios de mayoreo, ej. talla 2 y talla 16 no cuestan lo mismo).
 */
data class TallaEnCaptura(
    val talla: String,
    val stockTexto: String = "",
    val escalones: List<EscalonEnCaptura> = escalonesPorDefecto()
)

/** Una combinación de color + tallas marcadas, cada una con su stock y precios propios. */
data class ColorEnCaptura(
    val id: String, // id local temporal, solo para poder editar/quitar en la UI
    val color: String,
    val tallas: List<TallaEnCaptura> // solo las tallas marcadas para este color
)
