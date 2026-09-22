package com.tuempresa.possystem.presentation.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.ResumenVentasCaja
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.TipoCorte
import com.tuempresa.possystem.domain.cajaIdDeTienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class EstadoCorte {
    object Inactivo : EstadoCorte()
    object Procesando : EstadoCorte()
    data class Exitoso(val tipo: TipoCorte) : EstadoCorte()
    data class Error(val mensaje: String) : EstadoCorte()
}

/**
 * Cierre de caja de la TIENDA ACTIVA: resumen del turno abierto, corte X
 * (informativo) y corte Z (cierra el turno). Accesible tanto para VENDEDOR
 * como para ADMIN — cualquiera que esté a cargo del mostrador puede rendir
 * cuentas de su turno, no solo el administrador.
 *
 * A diferencia de ReportesViewModel (exclusivo ADMIN, exporta reportes
 * consolidados en Excel/PDF), esta pantalla es la operación diaria de caja.
 */
class CierreCajaViewModel(private val app: POSApplication) : ViewModel() {

    private val cajaRepository = app.cajaRepository
    private val ventaDao = app.database.ventaDao()
    private val corteCajaDao = app.database.corteCajaDao()
    private val sessionManager = app.sessionManager

    private val tiendaId get() = sessionManager.tiendaActivaIdRequerida()

    private val _resumenTurno = MutableStateFlow<ResumenVentasCaja?>(null)
    val resumenTurno: StateFlow<ResumenVentasCaja?> = _resumenTurno.asStateFlow()

    private val _fechaInicioTurno = MutableStateFlow(0L)
    val fechaInicioTurno: StateFlow<Long> = _fechaInicioTurno.asStateFlow()

    // null = no hay un fondo inicial declarado todavía para el turno vigente
    // (recién se cerró con Z, o es la primera vez que se usa la app en esta
    // tienda) -> hay que pedirlo antes de dejar operar la caja.
    private val _fondoInicialTurno = MutableStateFlow<Double?>(null)
    val fondoInicialTurno: StateFlow<Double?> = _fondoInicialTurno.asStateFlow()

    private val _estadoCorte = MutableStateFlow<EstadoCorte>(EstadoCorte.Inactivo)
    val estadoCorte: StateFlow<EstadoCorte> = _estadoCorte.asStateFlow()

    val historialCortes: StateFlow<List<CorteCajaEntity>> =
        cajaRepository.observarHistorial(tiendaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        cargarResumenTurno()
    }

    private fun cargarResumenTurno() {
        viewModelScope.launch {
            val estado = cajaRepository.obtenerEstadoTurno(tiendaId)
            _fechaInicioTurno.value = estado.fechaInicioTurno
            _fondoInicialTurno.value = estado.fondoInicialVigente
            _resumenTurno.value = corteCajaDao.calcularResumen(
                cajaIdDeTienda(tiendaId),
                estado.fechaInicioTurno,
                System.currentTimeMillis()
            )
        }
    }

    fun refrescar() = cargarResumenTurno()

    fun abrirTurno(fondoInicial: Double) {
        viewModelScope.launch {
            _estadoCorte.value = EstadoCorte.Procesando
            try {
                cajaRepository.abrirTurno(tiendaId, fondoInicial, sessionManager.usuarioActual.value?.id)
                _estadoCorte.value = EstadoCorte.Exitoso(TipoCorte.APERTURA)
                cargarResumenTurno()
            } catch (ex: Exception) {
                _estadoCorte.value = EstadoCorte.Error(ex.message ?: "No se pudo registrar el fondo inicial.")
            }
        }
    }

    fun realizarCorteX() {
        viewModelScope.launch {
            _estadoCorte.value = EstadoCorte.Procesando
            try {
                cajaRepository.realizarCorteX(tiendaId, sessionManager.usuarioActual.value?.id)
                _estadoCorte.value = EstadoCorte.Exitoso(TipoCorte.X)
                cargarResumenTurno()
            } catch (ex: Exception) {
                _estadoCorte.value = EstadoCorte.Error(ex.message ?: "No se pudo generar el corte X.")
            }
        }
    }

    fun realizarCorteZ(efectivoContado: Double, fondoInicialSiguienteTurno: Double) {
        viewModelScope.launch {
            _estadoCorte.value = EstadoCorte.Procesando
            try {
                cajaRepository.realizarCorteZ(
                    tiendaId = tiendaId,
                    efectivoContado = efectivoContado,
                    fondoInicialSiguienteTurno = fondoInicialSiguienteTurno,
                    usuarioId = sessionManager.usuarioActual.value?.id
                )
                _estadoCorte.value = EstadoCorte.Exitoso(TipoCorte.Z)
                cargarResumenTurno()
            } catch (ex: Exception) {
                _estadoCorte.value = EstadoCorte.Error(ex.message ?: "No se pudo cerrar la caja.")
            }
        }
    }

    fun limpiarEstadoCorte() {
        _estadoCorte.value = EstadoCorte.Inactivo
    }
}
