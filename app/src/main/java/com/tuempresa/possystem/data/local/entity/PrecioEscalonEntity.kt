package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Escalón de precio por cantidad, específico de un ProductoEntity (normalmente
 * una variante hija con talla/color, aunque también aplica a productos simples
 * sin variantes).
 *
 * Ejemplo para una variante "Blusa floral, talla M, color Vino":
 *   cantidadMinima=1  -> etiqueta="Unidad" -> precioUnitario=150.0
 *   cantidadMinima=3  -> etiqueta="1/4"    -> precioUnitario=140.0
 *   cantidadMinima=6  -> etiqueta="Media"  -> precioUnitario=130.0
 *   cantidadMinima=12 -> etiqueta="Docena" -> precioUnitario=120.0
 *   cantidadMinima=24 -> etiqueta="Mayoreo"-> precioUnitario=110.0
 *
 * Al vender, se busca el escalón con la cantidadMinima más alta que sea
 * <= a la cantidad que se está vendiendo de ESA variante específica
 * (ver PrecioEscalonDao.obtenerPrecioParaCantidad).
 */
@Entity(
    tableName = "precios_escalon",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productoId"),
        Index(value = ["productoId", "cantidadMinima"], unique = true)
    ]
)
data class PrecioEscalonEntity(
    @PrimaryKey
    val id: String,

    val productoId: String,

    val cantidadMinima: Int,     // 1, 3, 6, 12, 24...
    val etiqueta: String,        // "Unidad", "1/4", "Media", "Docena", "Mayoreo"
    val precioUnitario: Double,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
