package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: UsuarioEntity)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("UPDATE usuarios SET activo = 0, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun desactivar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("UPDATE usuarios SET activo = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun reactivar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("SELECT * FROM usuarios WHERE eliminado = 0 ORDER BY rol ASC, nombre ASC")
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE eliminado = 0 AND activo = 1 AND rol = :rol ORDER BY nombre ASC")
    fun observarPorRol(rol: RolUsuario): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios WHERE eliminado = 0 AND rol = 'ADMIN' AND activo = 1")
    suspend fun contarAdminsActivos(): Int

    /**
     * Trae todos los usuarios activos para intentar el login por PIN.
     * La verificación del hash se hace en la capa de dominio (PinHasher),
     * ya que SQLite no puede comparar hashes con sal por usuario en una sola query.
     */
    @Query("SELECT * FROM usuarios WHERE eliminado = 0 AND activo = 1")
    suspend fun obtenerTodosActivos(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<UsuarioEntity>

    @Query("UPDATE usuarios SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
