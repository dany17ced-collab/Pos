package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.EtiquetaPagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EtiquetaPagoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(etiqueta: EtiquetaPagoEntity)

    @Update
    suspend fun actualizar(etiqueta: EtiquetaPagoEntity)

    @Query("UPDATE etiquetas_pago SET eliminado = 1, sincronizado = 0 WHERE id = :id AND esEditable = 1")
    suspend fun marcarEliminada(id: String)

    @Query("SELECT * FROM etiquetas_pago WHERE eliminado = 0 ORDER BY orden ASC")
    fun observarTodas(): Flow<List<EtiquetaPagoEntity>>

    @Query("SELECT COUNT(*) FROM etiquetas_pago")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(etiquetas: List<EtiquetaPagoEntity>)

    @Query("SELECT * FROM etiquetas_pago WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<EtiquetaPagoEntity>

    @Query("UPDATE etiquetas_pago SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
