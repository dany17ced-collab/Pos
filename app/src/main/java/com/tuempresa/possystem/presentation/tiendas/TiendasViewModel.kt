package com.tuempresa.possystem.presentation.tiendas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Gestión de sucursales: crear, renombrar, desactivar, y elegir cuál está
 * activa para el dispositivo actual. Solo debe ser accesible para ADMIN — la
 * pantalla que la usa es responsable de esa verificación, este ViewModel no
 * la repite.
 */
class TiendasViewModel(private val app: POSApplication) : ViewModel() {

    private val tiendaRepository = app.tiendaRepository

    val tiendas: StateFlow<List<TiendaEntity>> = tiendaRepository.observarTiendas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tiendaActiva: StateFlow<TiendaEntity?> = app.sessionManager.tiendaActiva

    fun crearTienda(nombre: String, onCreada: () -> Unit) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        viewModelScope.launch {
            tiendaRepository.crearTienda(nombreLimpio)
            onCreada()
        }
    }

    fun renombrar(tienda: TiendaEntity, nuevoNombre: String) {
        val nombreLimpio = nuevoNombre.trim()
        if (nombreLimpio.isBlank()) return
        viewModelScope.launch {
            tiendaRepository.renombrar(tienda, nombreLimpio)
            // Si se renombró la tienda activa, refresca la sesión para que el
            // nombre nuevo se vea de inmediato en el resto de la app (Home, etc.)
            if (app.sessionManager.tiendaActiva.value?.id == tienda.id) {
                app.sessionManager.establecerTiendaActiva(tienda.copy(nombre = nombreLimpio))
            }
        }
    }

    /**
     * No se permite desactivar la única tienda activa que queda: la app
     * siempre necesita al menos una tienda operativa para poder vender.
     */
    fun desactivar(tienda: TiendaEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            val activas = tiendaRepository.obtenerTiendasActivas()
            if (activas.size <= 1 && activas.any { it.id == tienda.id }) {
                onError("No puedes desactivar la única tienda activa.")
                return@launch
            }
            tiendaRepository.desactivar(tienda)
            // Si era la tienda activa de la sesión, cambia a otra automáticamente.
            if (app.sessionManager.tiendaActiva.value?.id == tienda.id) {
                activas.firstOrNull { it.id != tienda.id }?.let {
                    app.sessionManager.establecerTiendaActiva(it)
                }
            }
        }
    }

    fun seleccionarComoActiva(tienda: TiendaEntity) {
        app.sessionManager.establecerTiendaActiva(tienda)
    }
}
