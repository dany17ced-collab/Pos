package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.DescuentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DescuentoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(descuento: DescuentoEntity)

    @Update
    suspend fun actualizar(descuento: DescuentoEntity)

    @Query("UPDATE descuentos SET eliminado = 1, sincronizado = 0 WHERE id = :id")
    suspend fun marcarEliminado(id: String)

    @Query("SELECT * FROM descuentos WHERE eliminado = 0 ORDER BY creadoEn DESC")
    fun observarTodos(): Flow<List<DescuentoEntity>>

    @Query("SELECT * FROM descuentos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): DescuentoEntity?

    @Query("SELECT * FROM descuentos WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<DescuentoEntity>

    @Query("UPDATE descuentos SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
