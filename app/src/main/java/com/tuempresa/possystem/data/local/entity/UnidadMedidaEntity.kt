package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Unidad de medida seleccionable en "Precio > Unidad" del formulario de
 * producto (ej. "pcs" entero, "kg" fraccional). Las de fábrica vienen
 * precargadas (ver DatosIniciales) y no son editables; el usuario puede
 * añadir las suyas desde el menú "Unidad".
 */
@Entity(tableName = "unidades_medida")
data class UnidadMedidaEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,

    /** true = admite decimales (kg, L, m); false = solo enteros (pcs). */
    val esFraccional: Boolean = false,

    val esEditable: Boolean = true,
    val orden: Int = 0,

    val creadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
