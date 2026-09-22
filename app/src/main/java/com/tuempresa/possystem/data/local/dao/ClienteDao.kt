package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cliente: ClienteEntity)

    @Update
    suspend fun actualizar(cliente: ClienteEntity)

    @Query("UPDATE clientes SET eliminado = 1, sincronizado = 0 WHERE id = :id")
    suspend fun marcarEliminado(id: String)

    @Query("SELECT * FROM clientes WHERE eliminado = 0 ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ClienteEntity?

    @Query("SELECT * FROM clientes WHERE codigoBarras = :codigo AND eliminado = 0 LIMIT 1")
    suspend fun buscarPorCodigoBarras(codigo: String): ClienteEntity?

    @Query("SELECT * FROM clientes WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<ClienteEntity>

    @Query("UPDATE clientes SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
