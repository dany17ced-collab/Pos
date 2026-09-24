package com.tuempresa.possystem.presentation.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.TipoMovimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RangoMovimientos(val etiqueta: String, val dias: Int) {
    HOY("Hoy", 1),
    SEMANA("7 días", 7),
    MES("30 días", 30)
}

/** Un movimiento ya resuelto con el nombre de su producto, listo para mostrar. */
data class MovimientoConProducto(
    val movimiento: MovimientoInventarioEntity,
    val nombreProducto: String
)

/** Totales agregados del rango, por tipo de movimiento. */
data class TotalesMovimientos(
    val ventas: Int = 0,
    val entradas: Int = 0,
    val salidas: Int = 0,
    val ajustes: Int = 0,
    val devoluciones: Int = 0,
    val cambios: Int = 0
)

/** Un punto del gráfico: un día con su desglose por tipo. */
data class PuntoGrafico(
    val etiquetaDia: String,
    val porTipo: Map<TipoMovimiento, Int>
) {
    val total: Int get() = porTipo.values.sum()
}

class MovimientosViewModel(private val app: POSApplication) : ViewModel() {

    private val movimientoDao = app.database.movimientoInventarioDao()
    private val productoDao = app.database.productoDao()

    private val _rango = MutableStateFlow(RangoMovimientos.SEMANA)
    val rango: StateFlow<RangoMovimientos> = _rango.asStateFlow()

    private val _filtroTipo = MutableStateFlow<TipoMovimiento?>(null)
    val filtroTipo: StateFlow<TipoMovimiento?> = _filtroTipo.asStateFlow()

    private val _movimientos = MutableStateFlow<List<MovimientoConProducto>>(emptyList())

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    val movimientosFiltrados: StateFlow<List<MovimientoConProducto>> =
        combine(_movimientos, _filtroTipo) { movimientos, tipo ->
            if (tipo == null) movimientos else movimientos.filter { it.movimiento.tipo == tipo }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totales: StateFlow<TotalesMovimientos> = _movimientos.map { lista ->
        TotalesMovimientos(
            ventas = lista.filter { it.movimiento.tipo == TipoMovimiento.VENTA }.sumOf { it.movimiento.cantidad },
            entradas = lista.filter { it.movimiento.tipo == TipoMovimiento.ENTRADA }.sumOf { it.movimiento.cantidad },
            salidas = lista.filter { it.movimiento.tipo == TipoMovimiento.SALIDA }.sumOf { it.movimiento.cantidad },
            ajustes = lista.filter { it.movimiento.tipo == TipoMovimiento.AJUSTE }.sumOf { it.movimiento.cantidad },
            devoluciones = lista.filter { it.movimiento.tipo == TipoMovimiento.DEVOLUCION }.sumOf { it.movimiento.cantidad },
            cambios = lista.filter { it.movimiento.tipo == TipoMovimiento.CAMBIO_SALIDA }.sumOf { it.movimiento.cantidad }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TotalesMovimientos())

    private val _puntosGrafico = MutableStateFlow<List<PuntoGrafico>>(emptyList())
    val puntosGrafico: StateFlow<List<PuntoGrafico>> = _puntosGrafico.asStateFlow()

    private val formatoDia = SimpleDateFormat("dd/MM", Locale("es", "PE"))

    private var trabajoCarga: kotlinx.coroutines.Job? = null

    init {
        cargar()
    }

    fun seleccionarRango(nuevoRango: RangoMovimientos) {
        _rango.value = nuevoRango
        cargar()
    }

    fun seleccionarFiltro(tipo: TipoMovimiento?) {
        _filtroTipo.value = tipo
    }

    fun cargar() {
        // Cancela la colección anterior antes de lanzar una nueva: si no, cada
        // cambio de rango deja un observador viejo corriendo en paralelo.
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _cargando.value = true
            try {
                val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
                val dias = _rango.value.dias

                val calendarHasta = Calendar.getInstance()
                calendarHasta.set(Calendar.HOUR_OF_DAY, 23)
                calendarHasta.set(Calendar.MINUTE, 59)
                calendarHasta.set(Calendar.SECOND, 59)
                val hasta = calendarHasta.timeInMillis

                val calendarDesde = Calendar.getInstance()
                calendarDesde.add(Calendar.DAY_OF_YEAR, -(dias - 1))
                calendarDesde.set(Calendar.HOUR_OF_DAY, 0)
                calendarDesde.set(Calendar.MINUTE, 0)
                calendarDesde.set(Calendar.SECOND, 0)
                val desde = calendarDesde.timeInMillis

                // observarEnRango no filtra por tienda, así que cruzamos con el
                // producto para quedarnos solo con los movimientos de esta tienda.
                val movimientosEnRango = movimientoDao.observarEnRango(desde, hasta)
                movimientosEnRango.collect { lista ->
                    val resueltos = lista.mapNotNull { movimiento ->
                        val producto = productoDao.obtenerPorId(movimiento.productoId)
                        if (producto != null && producto.tiendaId == tiendaId) {
                            MovimientoConProducto(
                                movimiento = movimiento,
                                nombreProducto = producto.nombreVariante ?: producto.nombre
                            )
                        } else null
                    }
                    _movimientos.value = resueltos
                    _puntosGrafico.value = construirPuntosGrafico(resueltos, dias)
                    _cargando.value = false
                }
            } catch (e: Exception) {
                _cargando.value = false
            }
        }
    }

    private fun construirPuntosGrafico(lista: List<MovimientoConProducto>, dias: Int): List<PuntoGrafico> {
        val formatoClave = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val porDia = lista.groupBy { formatoClave.format(Date(it.movimiento.fecha)) }

        val puntos = mutableListOf<PuntoGrafico>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(dias - 1))

        repeat(dias) {
            val clave = formatoClave.format(calendar.time)
            val etiqueta = formatoDia.format(calendar.time)
            val movimientosDia = porDia[clave].orEmpty()
            val porTipo = TipoMovimiento.values().associateWith { tipo ->
                movimientosDia.filter { it.movimiento.tipo == tipo }.sumOf { it.movimiento.cantidad }
            }.filterValues { it > 0 }
            puntos.add(PuntoGrafico(etiquetaDia = etiqueta, porTipo = porTipo))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return puntos
    }
}
