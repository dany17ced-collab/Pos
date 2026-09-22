package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cliente registrado desde el módulo "Cliente" del menú (estilo Eco POS):
 * nombre obligatorio, resto de campos opcionales. El código de barras permite
 * escanear al cliente durante el cobro (ej. tarjeta de fidelización impresa).
 */
@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,
    val telefono: String? = null,
    val codigoBarras: String? = null,
    val email: String? = null,
    val direccion: String? = null,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
