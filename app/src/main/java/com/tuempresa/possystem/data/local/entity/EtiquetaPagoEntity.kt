package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Etiqueta de método de pago configurable por el usuario (ej. "Efectivo",
 * "Yape", "Transferencia"), mostrada al cobrar. Distinta de [MetodoPago]
 * (el enum fijo usado internamente para reportes): esta tabla es la lista
 * editable que ve el cajero, y cada etiqueta puede mapear a un MetodoPago
 * base para que los reportes sigan funcionando igual.
 */
@Entity(tableName = "etiquetas_pago")
data class EtiquetaPagoEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,

    /** Nombre del [MetodoPago] base al que se contabiliza (EFECTIVO, TARJETA, TRANSFERENCIA...). */
    val metodoPagoBase: String = "EFECTIVO",

    /** Etiquetas de fábrica (ej. "Cash") no se pueden borrar, solo ocultar; las que añade el usuario sí. */
    val esEditable: Boolean = true,

    val orden: Int = 0,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
