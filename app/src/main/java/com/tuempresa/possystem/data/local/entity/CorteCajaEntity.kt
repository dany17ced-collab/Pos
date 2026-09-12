package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoCorte {
    X,  // Corte parcial (no cierra turno)
    Z   // Corte final (cierra turno)
}

/**
 * Registro de un corte de caja (X o Z). Un corte Z marca el fin de un turno;
 * el siguiente turno se considera iniciado justo en su fechaCorte.
 */
@Entity(
    tableName = "cortes_caja",
    indices = [Index("cajaId"), Index("fechaCorte")]
)
data class CorteCajaEntity(
    @PrimaryKey
    val id: String,

    val cajaId: String,
    val tipo: TipoCorte,

    val fechaInicio: Long, // inicio del turno que este corte resume
    val fechaCorte: Long = System.currentTimeMillis(),

    val numeroTransacciones: Int = 0,
    val totalVentas: Double = 0.0,
    val totalEfectivo: Double = 0.0,
    val totalTarjeta: Double = 0.0,
    val totalTransferencia: Double = 0.0,
    val totalDescuentos: Double = 0.0,
    val totalImpuestos: Double = 0.0,

    val fondoInicial: Double = 0.0,       // efectivo con el que se abrió el turno
    val efectivoEsperado: Double = 0.0,   // fondoInicial + totalEfectivo
    val efectivoContado: Double? = null,  // declarado físicamente al cerrar (solo corte Z)
    val diferencia: Double? = null,       // efectivoContado - efectivoEsperado (solo corte Z)

    val usuarioId: String? = null,        // quién hizo el corte

    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)
