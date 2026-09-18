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

/** Tipo de comprobante emitido: boleta (consumidor final) o factura (con RUC del cliente). */
enum class TipoComprobante {
    BOLETA, FACTURA
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

    /** Tipo de comprobante elegido al cobrar: boleta (por defecto) o factura. */
    val tipoComprobante: TipoComprobante = TipoComprobante.BOLETA,

    /** DNI o nombre del cliente, opcional, capturado antes de emitir el comprobante. */
    val clienteDocumento: String? = null,
    val clienteNombre: String? = null,

    /** RUC y razón social del cliente, obligatorios solo si tipoComprobante es FACTURA. */
    val clienteRuc: String? = null,
    val clienteRazonSocial: String? = null,

    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)
