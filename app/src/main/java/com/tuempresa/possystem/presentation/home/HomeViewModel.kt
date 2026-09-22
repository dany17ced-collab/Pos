package com.tuempresa.possystem.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/** Resumen de una tienda para la tarjeta deslizable del Home de administrador. */
data class ResumenTiendaHome(
    val tienda: TiendaEntity,
    val vendedoresActivos: Int,
    val ventasHoy: Double
)

/**
 * Stats del Home, ya sea de la tienda activa (vendedor) o consolidadas de
 * todas las tiendas (administrador).
 */
data class StatsHome(
    val ventasHoy: Double = 0.0,
    val numeroVentasHoy: Int = 0,
    val transaccionesHoy: Int = 0,
    val productosStockBajo: Int = 0,
    val vendedoresActivosAhora: Int = 0
)

/**
 * Alimenta las pantallas de Inicio de Vendedor y Administrador con datos
 * reales: ventas del día (propias o de todas las tiendas), stock bajo, y
 * tarjetas de tienda para el selector del administrador. Un solo ViewModel
 * porque ambas pantallas comparten el cálculo de "inicio de hoy" y consultan
 * los mismos DAOs, solo que filtrados distinto según el rol.
 */
class HomeViewModel(private val app: POSApplication) : ViewModel() {

    private val ventaDao = app.database.ventaDao()
    private val productoDao = app.database.productoDao()
    private val tiendaDao = app.database.tiendaDao()
    private val categoriaDao = app.database.categoriaDao()
    private val sessionManager = app.sessionManager

    private val _statsVendedor = MutableStateFlow(StatsHome())
    val statsVendedor: StateFlow<StatsHome> = _statsVendedor.asStateFlow()

    private val _statsAdmin = MutableStateFlow(StatsHome())
    val statsAdmin: StateFlow<StatsHome> = _statsAdmin.asStateFlow()

    private val _tiendasResumen = MutableStateFlow<List<ResumenTiendaHome>>(emptyList())
    val tiendasResumen: StateFlow<List<ResumenTiendaHome>> = _tiendasResumen.asStateFlow()

    private val _categorias = MutableStateFlow<List<CategoriaEntity>>(emptyList())
    val categorias: StateFlow<List<CategoriaEntity>> = _categorias.asStateFlow()

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
        viewModelScope.launch {
            categoriaDao.observarTodas().collect { _categorias.value = it }
        }
    }

    /** Carga las stats de SOLO la tienda activa — para el Home y el Inventario del vendedor. */
    fun cargarStatsVendedor() {
        viewModelScope.launch {
            val tiendaId = sessionManager.tiendaActivaIdRequerida()
            val ahora = System.currentTimeMillis()
            val ventas = ventaDao.obtenerVentasEnRango(tiendaId, inicioDeHoy, ahora)
                .filter { it.estado == EstadoVenta.COMPLETADA }
            val stockBajo = productoDao.obtenerTodosActivos(tiendaId)
                .count { it.stockActual <= it.stockMinimo }

            _statsVendedor.value = StatsHome(
                ventasHoy = ventas.sumOf { it.total },
                numeroVentasHoy = ventas.size,
                productosStockBajo = stockBajo
            )
        }
    }

    /** Carga las stats consolidadas de TODAS las tiendas — para el Home del administrador. */
    fun cargarStatsAdmin() {
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val tiendas = tiendaDao.obtenerActivas()

            val ventasHoy = ventaDao.obtenerVentasEnRangoGlobal(inicioDeHoy, ahora)
                .filter { it.estado == EstadoVenta.COMPLETADA }
                .groupBy { it.tiendaId }

            val stockBajoPorTienda = productoDao.contarStockBajoPorTienda()
                .associate { it.tiendaId to it.cantidad }

            _tiendasResumen.value = tiendas.map { tienda ->
                val ventasDeEstaTienda = ventasHoy[tienda.id].orEmpty()
                ResumenTiendaHome(
                    tienda = tienda,
                    // TODO: no hay todavía un registro de "vendedores asignados por
                    // tienda" ni de sesiones activas en tiempo real; se deja en 0
                    // hasta que exista esa función, en vez de inventar un número.
                    vendedoresActivos = 0,
                    ventasHoy = ventasDeEstaTienda.sumOf { it.total }
                )
            }

            _statsAdmin.value = StatsHome(
                ventasHoy = ventasHoy.values.flatten().sumOf { it.total },
                transaccionesHoy = ventasHoy.values.flatten().size,
                productosStockBajo = stockBajoPorTienda.values.sum(),
                // TODO: igual que arriba, requiere una noción real de sesión activa
                // por dispositivo para no ser un número inventado.
                vendedoresActivosAhora = 0
            )
        }
    }
}
