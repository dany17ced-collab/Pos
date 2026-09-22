package com.tuempresa.possystem.presentation.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EstadoAperturaCaja {
    /** Aún no se sabe si hay fondo declarado — evita parpadear el diálogo mientras carga. */
    object Verificando : EstadoAperturaCaja()
    /** Hay un fondo inicial vigente para el turno actual: se puede operar con normalidad. */
    object Lista : EstadoAperturaCaja()
    /** No hay fondo declarado para el turno vigente: hay que pedirlo antes de vender. */
    object RequiereApertura : EstadoAperturaCaja()
}

sealed class EstadoGuardadoApertura {
    object Inactivo : EstadoGuardadoApertura()
    object Guardando : EstadoGuardadoApertura()
    data class Error(val mensaje: String) : EstadoGuardadoApertura()
}

/**
 * Verifica si el turno vigente de la TIENDA ACTIVA ya tiene un fondo inicial
 * declarado y, si no, permite declararlo. Usa CajaRepository, el mismo que
 * PantallaCierreCaja, así que un fondo declarado desde Vender (vendedor) o
 * desde Cierre de Caja (vendedor o admin) es el mismo turno para esa tienda.
 *
 * Esto habilita que CUALQUIER vendedor, al entrar a Vender por primera vez en
 * el turno (es decir, mientras no haya un fondo inicial vigente desde el
 * último Corte Z), deba contar y declarar el dinero con el que arranca la
 * caja antes de poder cobrar.
 */
class AperturaCajaViewModel(private val app: POSApplication) : ViewModel() {

    private val cajaRepository = app.cajaRepository
    private val sessionManager = app.sessionManager

    private val _estado = MutableStateFlow<EstadoAperturaCaja>(EstadoAperturaCaja.Verificando)
    val estado: StateFlow<EstadoAperturaCaja> = _estado.asStateFlow()

    private val _estadoGuardado = MutableStateFlow<EstadoGuardadoApertura>(EstadoGuardadoApertura.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardadoApertura> = _estadoGuardado.asStateFlow()

    init {
        verificar()
    }

    fun verificar() {
        viewModelScope.launch {
            _estado.value = EstadoAperturaCaja.Verificando
            val tiendaId = sessionManager.tiendaActivaIdRequerida()
            val turno = cajaRepository.obtenerEstadoTurno(tiendaId)
            _estado.value = if (turno.fondoInicialVigente != null) {
                EstadoAperturaCaja.Lista
            } else {
                EstadoAperturaCaja.RequiereApertura
            }
        }
    }

    /**
     * Declara el fondo inicial contado por quien esté abriendo el turno (el
     * primer vendedor o admin que entra a vender después de un Corte Z, o la
     * primera vez que se usa la app en esta tienda). Queda registrado con el
     * usuario logueado en este momento, sea vendedor o admin.
     */
    fun declararFondoInicial(fondoInicial: Double) {
        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardadoApertura.Guardando
            try {
                cajaRepository.abrirTurno(
                    tiendaId = sessionManager.tiendaActivaIdRequerida(),
                    fondoInicial = fondoInicial,
                    usuarioId = sessionManager.usuarioActual.value?.id
                )
                _estadoGuardado.value = EstadoGuardadoApertura.Inactivo
                _estado.value = EstadoAperturaCaja.Lista
            } catch (ex: Exception) {
                _estadoGuardado.value = EstadoGuardadoApertura.Error(
                    ex.message ?: "No se pudo registrar el fondo inicial."
                )
            }
        }
    }
}
