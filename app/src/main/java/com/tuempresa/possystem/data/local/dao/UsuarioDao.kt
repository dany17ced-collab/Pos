package com.tuempresa.possystem.data.local.dao

import androidx.room.*
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    
    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY nombre")
    fun obtenerTodosUsuarios(): Flow<List<UsuarioEntity>>
    
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun obtenerUsuarioPorId(id: Long): UsuarioEntity?
    
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario AND contrasena = :contrasena AND activo = 1 LIMIT 1")
    suspend fun autenticarUsuario(nombreUsuario: String, contrasena: String): UsuarioEntity?
    
    @Query("SELECT * FROM usuarios WHERE rol = :rol AND activo = 1 ORDER BY nombre")
    fun obtenerUsuariosPorRol(rol: RolUsuario): Flow<List<UsuarioEntity>>
    
    @Query("SELECT COUNT(*) FROM usuarios WHERE activo = 1")
    fun contarUsuariosActivos(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: UsuarioEntity): Long
    
    @Update
    suspend fun actualizarUsuario(usuario: UsuarioEntity)
    
    @Delete
    suspend fun eliminarUsuario(usuario: UsuarioEntity)
    
    @Query("UPDATE usuarios SET activo = 0 WHERE id = :id")
    suspend fun desactivarUsuario(id: Long)
    
    @Query("UPDATE usuarios SET ultimoAcceso = :fecha WHERE id = :id")
    suspend fun actualizarUltimoAcceso(id: Long, fecha: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun existeNombreUsuario(nombreUsuario: String): UsuarioEntity?
}
