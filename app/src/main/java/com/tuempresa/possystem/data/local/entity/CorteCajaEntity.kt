package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoCorte {
    APERTURA, // Registra el fondo inicial contado al empezar un turno (no tiene ventas propias)
    X,  // Corte parcial (no cierra turno)
    Z   // Corte final (cierra turno)
}

/**
 * Registro de un corte de caja (X o Z). Un corte Z marca el fin de un turno;
 * el siguiente turno se considera iniciado justo en su fechaCorte.
 */
@Entity(
    tableName = "cortes_caja",
    foreignKeys = [
        ForeignKey(
            entity = TiendaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tiendaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cajaId"), Index("fechaCorte"), Index("tiendaId")]
)
data class CorteCajaEntity(
    @PrimaryKey
    val id: String,

    /** Sucursal a la que pertenece esta caja. */
    val tiendaId: String,

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
