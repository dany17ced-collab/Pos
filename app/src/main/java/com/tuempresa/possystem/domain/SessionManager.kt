package com.tuempresa.possystem.domain

import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda quién inició sesión actualmente, en memoria (no persiste si se cierra
 * la app por completo — al reabrir, se pide el PIN de nuevo). Esto es intencional
 * para un POS de mostrador: si alguien deja el teléfono, no debe quedar la sesión
 * de administrador abierta indefinidamente.
 */
class SessionManager {
    private val _usuarioActual = MutableStateFlow<UsuarioEntity?>(null)
    val usuarioActual: StateFlow<UsuarioEntity?> = _usuarioActual.asStateFlow()

    fun iniciarSesion(usuario: UsuarioEntity) {
        _usuarioActual.value = usuario
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
    }

    fun haySesionActiva(): Boolean = _usuarioActual.value != null
}
