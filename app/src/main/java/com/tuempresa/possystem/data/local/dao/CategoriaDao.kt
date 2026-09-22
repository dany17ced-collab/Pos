package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(categoria: CategoriaEntity)

    @Update
    suspend fun actualizar(categoria: CategoriaEntity)

    @Query("UPDATE categorias SET eliminado = 1, sincronizado = 0 WHERE id = :id")
    suspend fun marcarEliminada(id: String)

    @Query("SELECT * FROM categorias WHERE eliminado = 0 ORDER BY nombre ASC")
    fun observarTodas(): Flow<List<CategoriaEntity>>

    /** Igual que observarTodas, pero de un solo disparo — usada para generar reportes. */
    @Query("SELECT * FROM categorias WHERE eliminado = 0 ORDER BY nombre ASC")
    suspend fun obtenerTodas(): List<CategoriaEntity>

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): CategoriaEntity?

    @Query("SELECT * FROM categorias WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<CategoriaEntity>

    @Query("UPDATE categorias SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}

