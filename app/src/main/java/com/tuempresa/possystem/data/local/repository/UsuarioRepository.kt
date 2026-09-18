package com.tuempresa.possystem.data.local.repository

import com.tuempresa.possystem.data.local.dao.UsuarioDao
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de solo-lectura/listado para la pantalla de gestión de usuarios
 * (listar, buscar, filtrar, desactivar). La creación de usuarios y el cambio
 * de PIN pasan siempre por AuthRepository (domain/AuthRepository.kt), que es
 * quien conoce el hasheo con PinHasher.
 */
class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {
    val todosUsuarios: Flow<List<UsuarioEntity>> = usuarioDao.obtenerTodosUsuariosFlow()

    fun obtenerUsuariosPorRol(rol: RolUsuario): Flow<List<UsuarioEntity>> {
        return usuarioDao.obtenerUsuariosPorRol(rol)
    }

    fun contarUsuariosActivos(): Flow<Int> {
        return usuarioDao.contarUsuariosActivos()
    }

    suspend fun obtenerUsuarioPorId(id: String): UsuarioEntity? {
        return usuarioDao.obtenerUsuarioPorId(id)
    }

    suspend fun actualizarUsuario(usuario: UsuarioEntity) {
        usuarioDao.actualizar(
            usuario.copy(
                actualizadoEn = System.currentTimeMillis(),
                sincronizado = false
            )
        )
    }

    /** Desactiva (soft-delete) un usuario; no se permite si es el último admin activo. */
    suspend fun desactivarUsuario(id: String) {
        usuarioDao.desactivarUsuario(id)
    }

    suspend fun existeNombreUsuario(nombre: String): Boolean {
        return usuarioDao.existeNombre(nombre) != null
    }
}
