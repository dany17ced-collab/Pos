package com.tuempresa.possystem.domain

import com.tuempresa.possystem.data.local.dao.UsuarioDao
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import java.util.UUID

sealed class ResultadoLogin {
    data class Exitoso(val usuario: UsuarioEntity) : ResultadoLogin()
    object PinIncorrecto : ResultadoLogin()
    object SinUsuariosActivos : ResultadoLogin()
}

class AuthRepository(
    private val usuarioDao: UsuarioDao
) {
    /**
     * Intenta iniciar sesión probando el PIN contra todos los usuarios activos.
     * Como cada usuario tiene su propia sal, no se puede buscar directamente
     * por PIN en SQL: se compara uno por uno con PinHasher.verificar.
     */
    suspend fun iniciarSesion(pin: String): ResultadoLogin {
        val activos = usuarioDao.obtenerTodosActivos()
        if (activos.isEmpty()) return ResultadoLogin.SinUsuariosActivos

        val encontrado = activos.firstOrNull { usuario ->
            PinHasher.verificar(pin, usuario.pinSal, usuario.pinHash)
        }

        return if (encontrado != null) {
            ResultadoLogin.Exitoso(encontrado)
        } else {
            ResultadoLogin.PinIncorrecto
        }
    }

    /** Crea un usuario nuevo (admin o vendedor) con PIN hasheado. */
    suspend fun crearUsuario(nombre: String, pin: String, rol: RolUsuario): UsuarioEntity {
        val sal = PinHasher.generarSal()
        val hash = PinHasher.hashear(pin, sal)
        val usuario = UsuarioEntity(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            pinHash = hash,
            pinSal = sal,
            rol = rol
        )
        usuarioDao.insertar(usuario)
        return usuario
    }

    /** Cambia el PIN de un usuario existente. */
    suspend fun cambiarPin(usuario: UsuarioEntity, nuevoPin: String) {
        val nuevaSal = PinHasher.generarSal()
        val nuevoHash = PinHasher.hashear(nuevoPin, nuevaSal)
        usuarioDao.actualizar(
            usuario.copy(
                pinHash = nuevoHash,
                pinSal = nuevaSal,
                actualizadoEn = System.currentTimeMillis(),
                sincronizado = false
            )
        )
    }

    /** Evita que se desactive al último admin activo (bloquea a todos del sistema). */
    suspend fun puedeDesactivarse(usuario: UsuarioEntity): Boolean {
        if (usuario.rol != RolUsuario.ADMIN) return true
        val totalAdmins = usuarioDao.contarAdminsActivos()
        return totalAdmins > 1
    }
}
