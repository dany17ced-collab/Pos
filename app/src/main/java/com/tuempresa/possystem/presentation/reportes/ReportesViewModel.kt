package com.tuempresa.possystem.presentation.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.ProductoMasVendido
import com.tuempresa.possystem.data.local.dao.ResumenVentasCaja
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.TipoCorte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Identifica el dispositivo/caja actual — igual que en VentaViewModel, sin soporte multi-caja aún. */
private const val CAJA_ID = "caja-principal"

sealed class EstadoCorte {
    object Inactivo : EstadoCorte()
    object Procesando : EstadoCorte()
    data class Exitoso(val tipo: TipoCorte) : EstadoCorte()
    data class Error(val mensaje: String) : EstadoCorte()
}

/**
 * Junta el resumen de ventas del turno abierto (desde el último corte Z, o desde
 * siempre si nunca se ha hecho uno), el historial de cortes y los productos más
 * vendidos, para la pantalla de Reportes y Caja.
 */
class ReportesViewModel(app: POSApplication) : ViewModel() {

    private val corteCajaDao = app.database.corteCajaDao()
    private val ventaDao = app.database.ventaDao()
    private val detalleVentaDao = app.database.detalleVentaDao()
    private val sessionManager = app.sessionManager

    private val _resumenTurno = MutableStateFlow<ResumenVentasCaja?>(null)
    val resumenTurno: StateFlow<ResumenVentasCaja?> = _resumenTurno.asStateFlow()

    private val _fechaInicioTurno = MutableStateFlow<Long>(0L)
    val fechaInicioTurno: StateFlow<Long> = _fechaInicioTurno.asStateFlow()

    private val _estadoCorte = MutableStateFlow<EstadoCorte>(EstadoCorte.Inactivo)
    val estadoCorte: StateFlow<EstadoCorte> = _estadoCorte.asStateFlow()

    val historialCortes: StateFlow<List<CorteCajaEntity>> = corteCajaDao.observarHistorial()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rango fijo "hoy" para el ranking de más vendidos, independiente del turno de caja.
    private val inicioDeHoy: Long
        get() {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val masVendidosHoy: StateFlow<List<ProductoMasVendido>> =
        detalleVentaDao.observarProductosMasVendidos(inicioDeHoy, Long.MAX_VALUE, limite = 10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        cargarResumenTurno()
    }

    private fun cargarResumenTurno() {
        viewModelScope.launch {
            val ultimoCorte = corteCajaDao.obtenerUltimoCorte(CAJA_ID)
            // El turno abierto empieza justo después del último corte Z. Si el
            // último corte fue X (informativo, no cierra nada), su propio
            // fechaInicio ya apunta al inicio real del turno vigente.
            val desde = when (ultimoCorte?.tipo) {
                TipoCorte.Z -> ultimoCorte.fechaCorte
                TipoCorte.X -> ultimoCorte.fechaInicio
                null -> 0L
            }
            _fechaInicioTurno.value = desde
            _resumenTurno.value = corteCajaDao.calcularResumen(CAJA_ID, desde, System.currentTimeMillis())
        }
    }

    fun refrescar() = cargarResumenTurno()

    /**
     * Corte X: informativo, no cierra caja ni reinicia el turno — se puede hacer
     * las veces que se quiera durante el día para ver cómo va la venta.
     */
    fun realizarCorteX() {
        viewModelScope.launch {
            _estadoCorte.value = EstadoCorte.Procesando
            try {
                val desde = _fechaInicioTurno.value
                val ahora = System.currentTimeMillis()
                val resumen = corteCajaDao.calcularResumen(CAJA_ID, desde, ahora)
                val fondoInicial = historialCortes.value.firstOrNull()?.let {
                    if (it.tipo == TipoCorte.Z) 0.0 else it.fondoInicial
                } ?: 0.0

                corteCajaDao.insertar(
                    CorteCajaEntity(
                        id = UUID.randomUUID().toString(),
                        cajaId = CAJA_ID,
                        tipo = TipoCorte.X,
                        fechaInicio = desde,
                        fechaCorte = ahora,
                        totalVentas = resumen.totalVentas,
                        totalEfectivo = resumen.totalEfectivo,
                        totalTarjeta = resumen.totalTarjeta,
                        totalTransferencia = resumen.totalTransferencia,
                        totalDescuentos = resumen.totalDescuentos,
                        totalImpuestos = resumen.totalImpuestos,
                        numeroTransacciones = resumen.numeroTransacciones,
                        fondoInicial = fondoInicial,
                        efectivoEsperado = fondoInicial + resumen.totalEfectivo,
                        efectivoContado = null,
                        diferencia = null,
                        usuarioId = sessionManager.usuarioActual.value?.id
                    )
                )
                _estadoCorte.value = EstadoCorte.Exitoso(TipoCorte.X)
                cargarResumenTurno()
            } catch (ex: Exception) {
                _estadoCorte.value = EstadoCorte.Error(ex.message ?: "No se pudo generar el corte X.")
            }
        }
    }

    /**
     * Corte Z: cierra el turno actual. Requiere el efectivo contado físicamente
     * por el cajero para calcular la diferencia contra lo esperado por sistema.
     * Después de un corte Z, el próximo turno arranca desde fechaCorte de este.
     */
    fun realizarCorteZ(efectivoContado: Double, fondoInicialSiguienteTurno: Double) {
        viewModelScope.launch {
            _estadoCorte.value = EstadoCorte.Procesando
            try {
                val desde = _fechaInicioTurno.value
                val ahora = System.currentTimeMillis()
                val resumen = corteCajaDao.calcularResumen(CAJA_ID, desde, ahora)
                val fondoInicial = historialCortes.value.firstOrNull()?.let {
                    if (it.tipo == TipoCorte.Z) 0.0 else it.fondoInicial
                } ?: 0.0
                val efectivoEsperado = fondoInicial + resumen.totalEfectivo

                corteCajaDao.insertar(
                    CorteCajaEntity(
                        id = UUID.randomUUID().toString(),
                        cajaId = CAJA_ID,
                        tipo = TipoCorte.Z,
                        fechaInicio = desde,
                        fechaCorte = ahora,
                        totalVentas = resumen.totalVentas,
                        totalEfectivo = resumen.totalEfectivo,
                        totalTarjeta = resumen.totalTarjeta,
                        totalTransferencia = resumen.totalTransferencia,
                        totalDescuentos = resumen.totalDescuentos,
                        totalImpuestos = resumen.totalImpuestos,
                        numeroTransacciones = resumen.numeroTransacciones,
                        fondoInicial = fondoInicial,
                        efectivoEsperado = efectivoEsperado,
                        efectivoContado = efectivoContado,
                        diferencia = efectivoContado - efectivoEsperado,
                        usuarioId = sessionManager.usuarioActual.value?.id
                    )
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
