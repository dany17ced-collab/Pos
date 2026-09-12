package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aperturas_caja")
data class AperturaCajaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val fondoInicial: Double,
    val fechaApertura: Long = System.currentTimeMillis(),
    val usuarioId: Long? = null,
    val cierreId: Long? = null  // Referencia al corte Z que cerró este turno
)
