package com.tuempresa.possystem.presentation.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.VentaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class EstadoAnulacion {
    object Inactivo : EstadoAnulacion()
    object Procesando : EstadoAnulacion()
    object Exitoso : EstadoAnulacion()
    data class Error(val mensaje: String) : EstadoAnulacion()
}

/**
 * Ventas del día del vendedor logueado (no de todos los vendedores — para eso
 * está Reportes y caja, que es exclusivo de Admin). Permite anular una venta
 * propia del mismo día, lo cual repone el stock automáticamente
 * (ver VentaDao.anularVenta).
 */
class MisVentasViewModel(app: POSApplication) : ViewModel() {

    private val ventaDao = app.database.ventaDao()
    private val sessionManager = app.sessionManager

    private val _detallesPorVenta = MutableStateFlow<Map<String, List<DetalleVentaEntity>>>(emptyMap())
    val detallesPorVenta: StateFlow<Map<String, List<DetalleVentaEntity>>> = _detallesPorVenta.asStateFlow()

    private val _estadoAnulacion = MutableStateFlow<EstadoAnulacion>(EstadoAnulacion.Inactivo)
    val estadoAnulacion: StateFlow<EstadoAnulacion> = _estadoAnulacion.asStateFlow()

    private val inicioDeHoy: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val ventasDeHoy: StateFlow<List<VentaEntity>> = sessionManager.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                ventaDao.observarVentasDeUsuarioEnRango(usuario.id, inicioDeHoy, System.currentTimeMillis())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Carga (una sola vez) el detalle de productos de una venta, cuando el usuario la expande. */
    fun cargarDetalleSiHaceFalta(ventaId: String) {
        if (_detallesPorVenta.value.containsKey(ventaId)) return
        viewModelScope.launch {
            val detalles = ventaDao.obtenerDetallesDeVenta(ventaId)
            _detallesPorVenta.value = _detallesPorVenta.value + (ventaId to detalles)
        }
    }

    fun anularVenta(ventaId: String, motivo: String) {
        viewModelScope.launch {
            _estadoAnulacion.value = EstadoAnulacion.Procesando
            try {
                ventaDao.anularVenta(ventaId, motivo)
                _estadoAnulacion.value = EstadoAnulacion.Exitoso
            } catch (ex: Exception) {
                _estadoAnulacion.value = EstadoAnulacion.Error(ex.message ?: "No se pudo anular la venta.")
            }
        }
    }

    fun limpiarEstadoAnulacion() {
        _estadoAnulacion.value = EstadoAnulacion.Inactivo
    }
}
