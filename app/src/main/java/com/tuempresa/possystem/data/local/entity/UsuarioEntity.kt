package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RolUsuario {
    ADMINISTRADOR,
    VENDEDOR,
    SUPERVISOR
}

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val nombre: String,
    val nombreUsuario: String,
    val contrasena: String,  // En producción, debería estar encriptada
    val rol: RolUsuario,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val ultimoAcceso: Long? = null
)
