package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MotivoCambio {
    TALLA,          // el cliente quiere otra talla/color, la prenda está en buen estado
    FALLA_FABRICA   // la prenda tiene un defecto — queda registrado para control de calidad/proveedor
}

/**
 * Registro de un cambio de prenda: el cliente devuelve una variante (ej. Talla 8)
 * de una venta ya registrada, y se lleva otra (ej. Talla 10). Vinculado siempre a
 * la venta original — un cambio nunca es una venta nueva "suelta", es trazable.
 *
 * La diferencia de precio (a favor o en contra del cliente) se resuelve siempre
 * en efectivo en el momento, sin categorías de método de pago: es el flujo más
 * simple y rápido para el mostrador.
 */
@Entity(
    tableName = "cambios",
    foreignKeys = [
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ventaOriginalId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoDevueltoId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoEntregadoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("ventaOriginalId"),
        Index("productoDevueltoId"),
        Index("productoEntregadoId"),
        Index("motivo"),
        Index("fecha")
    ]
)
data class CambioEntity(
    @PrimaryKey
    val id: String,

    val ventaOriginalId: String,
    val folioVentaOriginal: Long?, // copia "congelada" para mostrar en el ticket de cambio sin otro join

    val productoDevueltoId: String,
    val nombreProductoDevuelto: String, // ej. "Buso (Talla 8 / Negro)" — congelado
    val precioUnitarioDevuelto: Double, // precio al que se vendió originalmente, para calcular la diferencia

    val productoEntregadoId: String,
    val nombreProductoEntregado: String, // ej. "Buso (Talla 10 / Negro)" — congelado
    val precioUnitarioEntregado: Double, // precio vigente de la nueva variante

    val cantidad: Int, // normalmente 1; se admite más de una unidad del mismo cambio

    val motivo: MotivoCambio,

    /** Positivo = el cliente pagó esta diferencia. Negativo = se le devolvió al cliente. Cero = sin diferencia. */
    val diferencia: Double,

    /** true si el cambio se autorizó fuera del plazo configurado (excepción manual del vendedor). */
    val fueraDePlazo: Boolean = false,

    val usuarioId: String? = null, // vendedor que atendió el cambio

    val fecha: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)
