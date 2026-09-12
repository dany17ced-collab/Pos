package com.tuempresa.possystem.data.repository

import com.tuempresa.possystem.data.local.dao.UsuarioDao
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {
    val todosUsuarios: Flow<List<UsuarioEntity>> = usuarioDao.obtenerTodosUsuarios()
    
    fun obtenerUsuariosPorRol(rol: RolUsuario): Flow<List<UsuarioEntity>> {
        return usuarioDao.obtenerUsuariosPorRol(rol)
    }
    
    fun contarUsuariosActivos(): Flow<Int> {
        return usuarioDao.contarUsuariosActivos()
    }
    
    suspend fun obtenerUsuarioPorId(id: Long): UsuarioEntity? {
        return usuarioDao.obtenerUsuarioPorId(id)
    }
    
    suspend fun autenticarUsuario(nombreUsuario: String, contrasena: String): UsuarioEntity? {
        return usuarioDao.autenticarUsuario(nombreUsuario, contrasena)
    }
    
    suspend fun insertarUsuario(usuario: UsuarioEntity): Long {
        return usuarioDao.insertarUsuario(usuario)
    }
    
    suspend fun actualizarUsuario(usuario: UsuarioEntity) {
        usuarioDao.actualizarUsuario(usuario)
    }
    
    suspend fun eliminarUsuario(usuario: UsuarioEntity) {
        usuarioDao.eliminarUsuario(usuario)
    }
    
    suspend fun desactivarUsuario(id: Long) {
        usuarioDao.desactivarUsuario(id)
    }
    
    suspend fun actualizarUltimoAcceso(id: Long) {
        usuarioDao.actualizarUltimoAcceso(id)
    }
    
    suspend fun existeNombreUsuario(nombreUsuario: String): Boolean {
        return usuarioDao.existeNombreUsuario(nombreUsuario) != null
    }
}
