package com.tuempresa.possystem.data.local.dao

import androidx.room.*
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE activo = 1 AND eliminado = 0 ORDER BY nombre")
    fun obtenerTodosUsuariosFlow(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE activo = 1 AND eliminado = 0")
    suspend fun obtenerTodosActivos(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun obtenerUsuarioPorId(id: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE rol = :rol AND activo = 1 AND eliminado = 0 ORDER BY nombre")
    fun obtenerUsuariosPorRol(rol: RolUsuario): Flow<List<UsuarioEntity>>

    @Query("SELECT COUNT(*) FROM usuarios WHERE activo = 1 AND eliminado = 0")
    fun contarUsuariosActivos(): Flow<Int>

    @Query("SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = 1 AND eliminado = 0")
    suspend fun contarAdminsActivos(): Int

    @Query("SELECT * FROM usuarios WHERE nombre = :nombre LIMIT 1")
    suspend fun existeNombre(nombre: String): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("UPDATE usuarios SET activo = 0, actualizadoEn = :fecha, sincronizado = 0 WHERE id = :id")
    suspend fun desactivarUsuario(id: String, fecha: Long = System.currentTimeMillis())
}
