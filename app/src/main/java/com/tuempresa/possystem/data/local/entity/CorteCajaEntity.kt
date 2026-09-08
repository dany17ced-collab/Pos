package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoCorte {
    X, // corte parcial informativo, NO cierra el turno/caja
    Z  // corte final del día, cierra la caja y reinicia contadores
}

/**
 * Registro de un corte de caja (X o Z). Guarda el snapshot de totales
 * al momento del corte para que el reporte histórico no cambie aunque
 * haya ventas nuevas después.
 */
@Entity(tableName = "cortes_caja")
data class CorteCajaEntity(
    @PrimaryKey
    val id: String,

    val cajaId: String,
    val tipo: TipoCorte,

    val fechaInicio: Long, // desde la última apertura o corte Z anterior
    val fechaCorte: Long,

    val totalVentas: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val totalTransferencia: Double,
    val totalDescuentos: Double,
    val totalImpuestos: Double,
    val numeroTransacciones: Int,

    val fondoInicial: Double = 0.0, // efectivo con el que se abrió la caja
    val efectivoEsperado: Double, // fondoInicial + totalEfectivo
    val efectivoContado: Double? = null, // lo que el cajero cuenta físicamente
    val diferencia: Double? = null, // efectivoContado - efectivoEsperado

    val usuarioId: String? = null,
    val sincronizado: Boolean = false
)
