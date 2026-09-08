package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Categoría de productos (ej: "Bebidas", "Ropa", "Electrónica").
 */
@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey
    val id: String, // UUID generado en el cliente (compatible con Postgres uuid)

    val nombre: String,
    val descripcion: String? = null,
    val colorHex: String? = null, // para distinguir visualmente en la UI

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    // Flags de sincronización offline-first
    val sincronizado: Boolean = false,
    val eliminado: Boolean = false // soft-delete: nunca borramos físicamente hasta sincronizar
)
