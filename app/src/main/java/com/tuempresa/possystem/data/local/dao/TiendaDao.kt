package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TiendaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(tienda: TiendaEntity)

    @Update
    suspend fun actualizar(tienda: TiendaEntity)

    @Query("SELECT * FROM tiendas WHERE eliminado = 0 ORDER BY creadoEn ASC")
    fun observarTodas(): Flow<List<TiendaEntity>>

    @Query("SELECT * FROM tiendas WHERE eliminado = 0 AND activa = 1 ORDER BY creadoEn ASC")
    suspend fun obtenerActivas(): List<TiendaEntity>

    @Query("SELECT * FROM tiendas WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): TiendaEntity?

    @Query("SELECT COUNT(*) FROM tiendas WHERE eliminado = 0")
    suspend fun contarTiendas(): Int
}
