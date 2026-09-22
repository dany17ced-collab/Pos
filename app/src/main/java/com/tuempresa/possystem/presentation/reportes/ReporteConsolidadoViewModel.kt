package com.tuempresa.possystem.presentation.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/** Resumen de una tienda dentro del consolidado: sus ventas de hoy y su stock bajo. */
data class ResumenTienda(
    val tienda: TiendaEntity,
    val totalVentasHoy: Double,
    val numeroVentasHoy: Int,
    val productosStockBajo: Int
)

/**
 * Resumen de TODAS las tiendas activas del negocio, para el administrador
 * que quiere ver de un vistazo cómo va cada sucursal sin entrar una por una.
 * Usa las consultas *Global de VentaDao/ProductoDao, que antes existían en
 * el DAO pero no estaban conectadas a ninguna pantalla.
 */
class ReporteConsolidadoViewModel(app: POSApplication) : ViewModel() {

    private val tiendaDao = app.database.tiendaDao()
    private val ventaDao = app.database.ventaDao()
    private val productoDao = app.database.productoDao()

    private val _resumenes = MutableStateFlow<List<ResumenTienda>>(emptyList())
    val resumenes: StateFlow<List<ResumenTienda>> = _resumenes.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val inicioDeHoy: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true

            val tiendas = tiendaDao.obtenerActivas()
            val ventasHoy = ventaDao.obtenerVentasEnRangoGlobal(inicioDeHoy, System.currentTimeMillis())
                .filter { it.estado == EstadoVenta.COMPLETADA }
                .groupBy { it.tiendaId }
            val stockBajoPorTienda = productoDao.contarStockBajoPorTienda()
                .associate { it.tiendaId to it.cantidad }

            _resumenes.value = tiendas.map { tienda ->
                val ventasDeEstaTienda = ventasHoy[tienda.id].orEmpty()
                ResumenTienda(
                    tienda = tienda,
                    totalVentasHoy = ventasDeEstaTienda.sumOf { it.total },
                    numeroVentasHoy = ventasDeEstaTienda.size,
                    productosStockBajo = stockBajoPorTienda[tienda.id] ?: 0
                )
            }
            _cargando.value = false
        }
    }
}
