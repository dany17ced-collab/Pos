package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RolUsuario {
    ADMIN, VENDEDOR
}

/**
 * Usuario del sistema (admin o vendedor). El acceso es por PIN numérico corto,
 * pensado para uso rápido en mostrador desde una tablet/teléfono compartido.
 *
 * El PIN NUNCA se guarda en texto plano: `pinHash` almacena el resultado de
 * hashear el PIN + una sal aleatoria (ver PinHasher). Esto evita que alguien
 * con acceso directo a la base de datos local pueda leer los PINs de los
 * empleados.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,
    val pinHash: String,   // hash del PIN, nunca el PIN en claro
    val pinSal: String,    // sal aleatoria única por usuario, usada al hashear
    val rol: RolUsuario,
    val activo: Boolean = true,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
