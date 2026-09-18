package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RolUsuario {
    ADMIN,
    VENDEDOR
}

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,
    val pinHash: String,
    val pinSal: String,
    val rol: RolUsuario,
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
