package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoCorte {
    X,  // Corte parcial (no cierra turno)
    Z   // Corte final (cierra turno)
}

@Entity(tableName = "cortes_caja")
data class CorteCajaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val tipo: TipoCorte,
    val fechaCorte: Long = System.currentTimeMillis(),
    val numeroTransacciones: Int = 0,
    val totalVentas: Double = 0.0,
    val totalEfectivo: Double = 0.0,
    val totalTarjeta: Double = 0.0,
    val totalTransferencia: Double = 0.0,
    val totalDescuentos: Double = 0.0,
    val fondoInicial: Double = 0.0,  // 💰 NUEVO: Efectivo con el que se abrió el turno
    val efectivoDeclarado: Double? = null,  // Efectivo declarado al cerrar
    val diferencia: Double? = null  // Diferencia entre esperado y declarado
)
