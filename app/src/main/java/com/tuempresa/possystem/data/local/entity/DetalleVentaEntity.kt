package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Línea individual de una venta (un producto + cantidad).
 * Guarda precioUnitario "congelado" al momento de la venta:
 * si el precio del producto cambia después, el histórico de ventas no se altera.
 */
@Entity(
    tableName = "detalle_venta",
    foreignKeys = [
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ventaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("ventaId"), Index("productoId")]
)
data class DetalleVentaEntity(
    @PrimaryKey
    val id: String,

    val ventaId: String,
    val productoId: String,
    val nombreProducto: String, // copia "congelada" por si el producto se renombra/borra luego
    val cantidad: Int,
    val precioUnitario: Double, // precio de venta congelado
    val precioCompraUnitario: Double, // costo congelado, para calcular margen histórico real
    val impuestoPorcentaje: Double,
    val descuentoLinea: Double = 0.0,
    val subtotal: Double, // cantidad * precioUnitario - descuentoLinea

    val sincronizado: Boolean = false
)
