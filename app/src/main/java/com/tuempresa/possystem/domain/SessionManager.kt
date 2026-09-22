package com.tuempresa.possystem.domain

import com.tuempresa.possystem.data.local.entity.TiendaEntity
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deriva el id de caja física a partir del id de tienda. Cada tienda tiene UNA
 * sola caja/mostrador por ahora (suficiente para una tienda de ropa de
 * mostrador único); si en el futuro una sucursal necesita más de una caja
 * física, este es el único punto que habría que cambiar para dar soporte a
 * varias por tienda.
 */
fun cajaIdDeTienda(tiendaId: String): String = "caja-$tiendaId"

/**
 * Guarda quién inició sesión actualmente y en qué tienda está operando, en
 * memoria (no persiste si se cierra la app por completo — al reabrir, se pide
 * el PIN de nuevo). Esto es intencional para un POS de mostrador: si alguien
 * deja el teléfono, no debe quedar la sesión de administrador abierta
 * indefinidamente.
 *
 * La tienda activa determina qué inventario/ventas se muestran en toda la app.
 * Un VENDEDOR normalmente queda fijo en la tienda donde inició sesión; un
 * ADMIN puede cambiarla desde Ajustes para ver/operar otra sucursal.
 */
class SessionManager {
    private val _usuarioActual = MutableStateFlow<UsuarioEntity?>(null)
    val usuarioActual: StateFlow<UsuarioEntity?> = _usuarioActual.asStateFlow()

    private val _tiendaActiva = MutableStateFlow<TiendaEntity?>(null)
    val tiendaActiva: StateFlow<TiendaEntity?> = _tiendaActiva.asStateFlow()

    fun iniciarSesion(usuario: UsuarioEntity) {
        _usuarioActual.value = usuario
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        _tiendaActiva.value = null
    }

    fun haySesionActiva(): Boolean = _usuarioActual.value != null

    fun establecerTiendaActiva(tienda: TiendaEntity) {
        _tiendaActiva.value = tienda
    }

    /** Id de la tienda activa, o lanza si aún no se estableció ninguna. */
    fun tiendaActivaIdRequerida(): String =
        _tiendaActiva.value?.id
            ?: error("No hay tienda activa; llama a establecerTiendaActiva() antes de operar.")
}
