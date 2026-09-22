package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MetodoPago {
    EFECTIVO, TARJETA, TRANSFERENCIA, QR, MIXTO
}

enum class EstadoVenta {
    COMPLETADA, ANULADA
}

/**
 * Tipo de comprobante emitido. BOLETA y FACTURA son documentos tributarios
 * (numeración correlativa formal, eventualmente reportables a SUNAT vía un
 * PSE/OSE). NOTA_VENTA es un documento interno de la tienda, SIN validez
 * tributaria y SIN reportarse nunca a SUNAT — solo sirve de comprobante de
 * control interno con el cliente (ej. para habilitar un cambio/garantía).
 * Lleva su propio correlativo, separado del de boletas/facturas, para no
 * introducir huecos en la numeración tributaria (ver VentaDao.obtenerUltimoFolio).
 */
enum class TipoComprobante {
    BOLETA, FACTURA, NOTA_VENTA
}

/**
 * Cabecera de una venta. El detalle (productos vendidos) vive en DetalleVentaEntity.
 */
@Entity(
    tableName = "ventas",
    foreignKeys = [
        ForeignKey(
            entity = TiendaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tiendaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fecha"), Index("cajaId"), Index("sincronizado"), Index("tiendaId")]
)
data class VentaEntity(
    @PrimaryKey
    val id: String,

    /** Sucursal donde se realizó la venta. */
    val tiendaId: String,

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
