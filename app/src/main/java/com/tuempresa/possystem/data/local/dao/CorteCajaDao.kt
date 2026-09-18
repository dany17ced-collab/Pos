package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import kotlinx.coroutines.flow.Flow

/** Fila agregada usada para calcular los totales de un corte X o Z. */
data class ResumenVentasCaja(
    val totalVentas: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val totalTransferencia: Double,
    val totalDescuentos: Double,
    val totalImpuestos: Double,
    val numeroTransacciones: Int
)

@Dao
interface CorteCajaDao {

    @Insert
    suspend fun insertar(corte: CorteCajaEntity)

    @Query(
        """
        SELECT 
            COALESCE(SUM(total), 0.0) as totalVentas,
            COALESCE(SUM(CASE WHEN metodoPago = 'EFECTIVO' THEN total ELSE 0.0 END), 0.0) as totalEfectivo,
            COALESCE(SUM(CASE WHEN metodoPago = 'TARJETA' THEN total ELSE 0.0 END), 0.0) as totalTarjeta,
            COALESCE(SUM(CASE WHEN metodoPago IN ('TRANSFERENCIA', 'QR') THEN total ELSE 0.0 END), 0.0) as totalTransferencia,
            COALESCE(SUM(descuento), 0.0) as totalDescuentos,
            COALESCE(SUM(impuestos), 0.0) as totalImpuestos,
            COUNT(*) as numeroTransacciones
        FROM ventas 
        WHERE cajaId = :cajaId AND estado = 'COMPLETADA' AND fecha BETWEEN :desde AND :hasta
        """
    )
    suspend fun calcularResumen(cajaId: String, desde: Long, hasta: Long): ResumenVentasCaja

    @Query("SELECT * FROM cortes_caja WHERE cajaId = :cajaId ORDER BY fechaCorte DESC LIMIT 1")
    suspend fun obtenerUltimoCorte(cajaId: String): CorteCajaEntity?

    @Query("SELECT * FROM cortes_caja ORDER BY fechaCorte DESC")
    fun observarHistorial(): Flow<List<CorteCajaEntity>>

    /** Igual que observarHistorial, pero de un solo disparo — usada para generar reportes. */
    @Query("SELECT * FROM cortes_caja WHERE fechaCorte BETWEEN :desde AND :hasta ORDER BY fechaCorte DESC")
    suspend fun obtenerHistorialEnRango(desde: Long, hasta: Long): List<CorteCajaEntity>

    @Query("SELECT * FROM cortes_caja WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<CorteCajaEntity>

    @Query("UPDATE cortes_caja SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
