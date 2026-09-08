package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MetodoPago {
    EFECTIVO, TARJETA, TRANSFERENCIA, QR, MIXTO
}

enum class EstadoVenta {
    COMPLETADA, ANULADA
}

/**
 * Cabecera de una venta. El detalle (productos vendidos) vive en DetalleVentaEntity.
 */
@Entity(
    tableName = "ventas",
    indices = [Index("fecha"), Index("cajaId"), Index("sincronizado")]
)
data class VentaEntity(
    @PrimaryKey
    val id: String,

    val folio: Long? = null, // consecutivo legible para el ticket, asignado al sincronizar o localmente por caja
    val fecha: Long, // epoch millis

    val subtotal: Double,
    val impuestos: Double,
    val descuento: Double = 0.0,
    val total: Double,

    val metodoPago: MetodoPago,
    val montoRecibido: Double? = null, // relevante si es efectivo
    val cambio: Double? = null,

    val cajaId: String, // identifica el dispositivo/caja que realizó la venta
    val usuarioId: String? = null, // cajero, si hay multi-usuario

    val estado: EstadoVenta = EstadoVenta.COMPLETADA,
    val notaAnulacion: String? = null,

    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)
