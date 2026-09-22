package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuempresa.possystem.data.local.entity.UnidadMedidaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnidadMedidaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(unidad: UnidadMedidaEntity)

    @Query("UPDATE unidades_medida SET eliminado = 1, sincronizado = 0 WHERE id = :id AND esEditable = 1")
    suspend fun marcarEliminada(id: String)

    @Query("SELECT * FROM unidades_medida WHERE eliminado = 0 ORDER BY orden ASC")
    fun observarTodas(): Flow<List<UnidadMedidaEntity>>

    @Query("SELECT COUNT(*) FROM unidades_medida")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(unidades: List<UnidadMedidaEntity>)

    @Query("SELECT * FROM unidades_medida WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<UnidadMedidaEntity>

    @Query("UPDATE unidades_medida SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
