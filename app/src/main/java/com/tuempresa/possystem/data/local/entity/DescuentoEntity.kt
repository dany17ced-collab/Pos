package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoValorDescuento { FIJO, PORCENTAJE }

/**
 * Descuento reutilizable en el carrito (ej. "Liquidación" -S/2.00), aplicable
 * a toda la venta o restringido a un producto puntual (pestaña "Producto" del
 * formulario "Añadir Descuento" de Eco POS).
 */
@Entity(tableName = "descuentos")
data class DescuentoEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,
    val codigoPromocional: String? = null,
    val descripcion: String? = null,

    val tipoValor: TipoValorDescuento = TipoValorDescuento.FIJO,
    val valor: Double = 0.0,

    /** Si es null, el descuento aplica a toda la venta; si no, solo a ese producto. */
    val productoId: String? = null,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
