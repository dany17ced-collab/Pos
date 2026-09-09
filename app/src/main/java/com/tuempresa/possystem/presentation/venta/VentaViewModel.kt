package com.tuempresa.possystem.presentation.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.StockInsuficienteException
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.data.local.entity.VentaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/** Resultado de buscar un producto por código de barras o texto. */
sealed class ResultadoBusqueda {
    object SinBuscar : ResultadoBusqueda()
    object Buscando : ResultadoBusqueda()
    object NoEncontrado : ResultadoBusqueda()
    data class Encontrado(val padre: ProductoEntity?, val variantes: List<ProductoEntity>) : ResultadoBusqueda()
}

sealed class EstadoCobro {
    object Inactivo : EstadoCobro()
    object Procesando : EstadoCobro()
    data class Exitoso(val ventaId: String, val folio: Long?) : EstadoCobro()
    data class Error(val mensaje: String) : EstadoCobro()
}

class VentaViewModel(private val app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()
    private val ventaDao = app.database.ventaDao()
    private val precioCalculator = app.precioCalculator

    private val _resultadoBusqueda = MutableStateFlow<ResultadoBusqueda>(ResultadoBusqueda.SinBuscar)
    val resultadoBusqueda: StateFlow<ResultadoBusqueda> = _resultadoBusqueda.asStateFlow()

    private val _carrito = MutableStateFlow<List<LineaCarrito>>(emptyList())
    val carrito: StateFlow<List<LineaCarrito>> = _carrito.asStateFlow()

    private val _estadoCobro = MutableStateFlow<EstadoCobro>(EstadoCobro.Inactivo)
    val estadoCobro: StateFlow<EstadoCobro> = _estadoCobro.asStateFlow()

    /** El total se calcula directamente en la UI con derivedStateOf sobre carrito.value */
    fun calcularTotal(lineas: List<LineaCarrito>): Double = lineas.sumOf { it.subtotal }

    /** Búsqueda por código de barras exacto (viene del escáner). */
    fun buscarPorCodigoBarras(codigo: String) {
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusqueda.Buscando
            val producto = productoDao.buscarPorCodigoBarras(codigo)
            resolverResultado(producto)
        }
    }

    /** Búsqueda manual por nombre o SKU — toma el primer resultado que haga match exacto de SKU o similar por nombre. */
    fun buscarPorTexto(texto: String) {
        if (texto.isBlank()) {
            _resultadoBusqueda.value = ResultadoBusqueda.SinBuscar
            return
        }
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusqueda.Buscando
            val producto = productoDao.buscarPorSku(texto)
                ?: productoDao.buscar(texto).first().firstOrNull { it.productoBaseId == null }
            resolverResultado(producto)
        }
    }

    private suspend fun resolverResultado(producto: ProductoEntity?) {
        if (producto == null) {
            _resultadoBusqueda.value = ResultadoBusqueda.NoEncontrado
            return
        }
        // Si el producto encontrado ya es una variante (tiene padre), lo tratamos
        // como si hubiéramos encontrado a su padre para mostrar todas las variantes hermanas.
        val esVariante = producto.productoBaseId != null
        val idPadre = if (esVariante) producto.productoBaseId!! else producto.id
        val padre = if (esVariante) productoDao.obtenerPorId(idPadre) else producto

        val variantes = productoDao.observarVariantes(idPadre).first()

        _resultadoBusqueda.value = if (variantes.isEmpty()) {
            // Producto simple, sin variantes de talla/color: se vende directo con su propio stock
            ResultadoBusqueda.Encontrado(padre = null, variantes = listOf(producto))
        } else {
            ResultadoBusqueda.Encontrado(padre = padre, variantes = variantes)
        }
    }

    fun limpiarBusqueda() {
        _resultadoBusqueda.value = ResultadoBusqueda.SinBuscar
    }

    /** Agrega una variante al carrito con la cantidad indicada, calculando el precio por escalón. */
    fun agregarAlCarrito(variante: ProductoEntity, cantidad: Int) {
        if (cantidad <= 0) return
        viewModelScope.launch {
            val precio = precioCalculator.calcularPrecioUnitario(variante, cantidad)
            val etiqueta = precioCalculator.obtenerEtiquetaEscalon(variante, cantidad)

            val lineaExistente = _carrito.value.find { it.producto.id == variante.id }
            if (lineaExistente != null) {
                // Ya estaba en el carrito: se suma la cantidad y se recalcula el precio
                // por si el nuevo total cambia de escalón (ej. pasa de "Unidad" a "Docena").
                val nuevaCantidad = lineaExistente.cantidad + cantidad
                val nuevoPrecio = precioCalculator.calcularPrecioUnitario(variante, nuevaCantidad)
                val nuevaEtiqueta = precioCalculator.obtenerEtiquetaEscalon(variante, nuevaCantidad)
                _carrito.value = _carrito.value.map {
                    if (it.id == lineaExistente.id) {
                        it.copy(cantidad = nuevaCantidad, precioUnitario = nuevoPrecio, etiquetaEscalon = nuevaEtiqueta)
                    } else it
                }
            } else {
                _carrito.value = _carrito.value + LineaCarrito(
                    id = UUID.randomUUID().toString(),
                    producto = variante,
                    cantidad = cantidad,
                    precioUnitario = precio,
                    etiquetaEscalon = etiqueta
                )
            }
            limpiarBusqueda()
        }
    }

    fun quitarDelCarrito(lineaId: String) {
        _carrito.value = _carrito.value.filterNot { it.id == lineaId }
    }

    fun vaciarCarrito() {
        _carrito.value = emptyList()
    }

    /** Confirma el cobro: arma la venta y sus detalles, y llama a la transacción atómica del DAO. */
    fun confirmarCobro(metodoPago: MetodoPago, montoRecibido: Double?) {
        val lineas = _carrito.value
        if (lineas.isEmpty()) return

        viewModelScope.launch {
            _estadoCobro.value = EstadoCobro.Procesando
            try {
                val ventaId = UUID.randomUUID().toString()
                val ahora = System.currentTimeMillis()
                val subtotalGeneral = lineas.sumOf { it.subtotal }
                // El impuesto ya viene incluido conceptualmente por producto; aquí se
                // deja en 0 a nivel de venta general y se documenta por línea si aplica.
                val impuestosGeneral = lineas.sumOf {
                    it.subtotal * (it.producto.impuestoPorcentaje / 100.0)
                }
                val totalGeneral = subtotalGeneral + impuestosGeneral
                val cambio = if (metodoPago == MetodoPago.EFECTIVO && montoRecibido != null) {
                    (montoRecibido - totalGeneral).coerceAtLeast(0.0)
                } else null

                val usuarioActual = app.sessionManager.usuarioActual.value

                val venta = VentaEntity(
                    id = ventaId,
                    fecha = ahora,
                    subtotal = subtotalGeneral,
                    impuestos = impuestosGeneral,
                    total = totalGeneral,
                    metodoPago = metodoPago,
                    montoRecibido = montoRecibido,
                    cambio = cambio,
                    cajaId = "caja-principal", // TODO: soporte multi-caja en un paso futuro
                    usuarioId = usuarioActual?.id
                )

                val detalles = lineas.map { linea ->
                    DetalleVentaEntity(
                        id = UUID.randomUUID().toString(),
                        ventaId = ventaId,
                        productoId = linea.producto.id,
                        nombreProducto = nombreCompletoVariante(linea.producto),
                        cantidad = linea.cantidad,
                        precioUnitario = linea.precioUnitario,
                        precioCompraUnitario = linea.producto.precioCompra,
                        impuestoPorcentaje = linea.producto.impuestoPorcentaje,
                        subtotal = linea.subtotal
                    )
                }

                ventaDao.registrarVentaCompleta(venta, detalles)

                _estadoCobro.value = EstadoCobro.Exitoso(ventaId, venta.folio)
                vaciarCarrito()
            } catch (e: StockInsuficienteException) {
                _estadoCobro.value = EstadoCobro.Error(
                    "Stock insuficiente para \"${e.nombreProducto}\": disponible ${e.stockDisponible}, solicitado ${e.cantidadSolicitada}"
                )
            } catch (e: Exception) {
                _estadoCobro.value = EstadoCobro.Error("No se pudo completar la venta. Intenta de nuevo.")
            }
        }
    }

    fun reiniciarEstadoCobro() {
        _estadoCobro.value = EstadoCobro.Inactivo
    }

    private fun nombreCompletoVariante(producto: ProductoEntity): String {
        val partes = listOfNotNull(
            producto.talla?.let { "Talla $it" },
            producto.color
        )
        return if (partes.isEmpty()) producto.nombre else "${producto.nombre} (${partes.joinToString(" / ")})"
    }
}
