package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoMovimiento {
    ENTRADA,   // compra a proveedor
    SALIDA,    // merma, pérdida, robo
    AJUSTE,    // corrección manual de inventario (positiva o negativa)
    VENTA,     // descuento automático por venta
    DEVOLUCION // reingreso por devolución de cliente
}

/**
 * Bitácora de todo movimiento de stock. Es la fuente de verdad para auditar
 * por qué el stock de un producto es el que es. Cada venta, compra o ajuste
 * genera una fila aquí — nunca se modifica el stock sin dejar rastro.
 */
@Entity(
    tableName = "movimientos_inventario",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productoId"), Index("fecha"), Index("tipo")]
)
data class MovimientoInventarioEntity(
    @PrimaryKey
    val id: String,

    val productoId: String,
    val tipo: TipoMovimiento,

    val cantidad: Int, // siempre positivo; el signo lo determina `tipo`
    val stockAnterior: Int,
    val stockNuevo: Int,

    val costoUnitario: Double? = null, // relevante en ENTRADA (compra a proveedor)
    val referenciaId: String? = null, // ID de venta, compra o ajuste relacionado
    val motivo: String? = null, // texto libre: "merma por caducidad", "conteo físico", etc.
    val usuarioId: String? = null,

    val fecha: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)
