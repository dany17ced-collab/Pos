package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tuempresa.possystem.data.local.entity.AperturaCajaEntity

@Dao
interface AperturaCajaDao {

    @Insert
    suspend fun insertar(apertura: AperturaCajaEntity)

    /**
     * Última apertura de caja registrada para esta caja, la haya hecho el
     * usuario que sea. Se usa para saber si YA se declaró un fondo inicial
     * para el turno vigente (es decir, después del último corte Z).
     */
    @Query("SELECT * FROM aperturas_caja WHERE cajaId = :cajaId ORDER BY fechaApertura DESC LIMIT 1")
    suspend fun obtenerUltimaApertura(cajaId: String): AperturaCajaEntity?

    /**
     * Última apertura hecha por este usuario específico en esta caja — se usa
     * para decidir si a ESTE vendedor le corresponde declarar el fondo inicial
     * (por ejemplo, si es la primera vez que entra a vender hoy).
     */
    @Query(
        "SELECT * FROM aperturas_caja WHERE cajaId = :cajaId AND usuarioId = :usuarioId " +
            "ORDER BY fechaApertura DESC LIMIT 1"
    )
    suspend fun obtenerUltimaAperturaDeUsuario(cajaId: String, usuarioId: String): AperturaCajaEntity?
}
