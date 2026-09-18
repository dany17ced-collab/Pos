package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.PrecioEscalonEntity
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

sealed class EstadoBusquedaProducto {
    object SinBuscar : EstadoBusquedaProducto()
    object Buscando : EstadoBusquedaProducto()
    object NoEncontrado : EstadoBusquedaProducto()
    data class Encontrado(val padre: ProductoEntity, val variantes: List<ProductoEntity>) : EstadoBusquedaProducto()
}

sealed class EstadoRegistroEntrada {
    object Inactivo : EstadoRegistroEntrada()
    object Guardando : EstadoRegistroEntrada()
    object Exitoso : EstadoRegistroEntrada()
    data class Error(val mensaje: String) : EstadoRegistroEntrada()
}

class EntradaMercaderiaViewModel(private val app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()
    private val movimientoInventarioDao = app.database.movimientoInventarioDao()
    private val precioEscalonDao = app.database.precioEscalonDao()

    // ---- Búsqueda de resultados por texto libre (nombre/SKU/código) mientras se escribe ----
    private val _resultadosBusqueda = MutableStateFlow<List<ProductoEntity>>(emptyList())
    val resultadosBusqueda: StateFlow<List<ProductoEntity>> = _resultadosBusqueda.asStateFlow()

    fun buscarPorTexto(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _resultadosBusqueda.value = emptyList()
                return@launch
            }
            // Solo productos padre (agrupadores); las variantes se muestran después de elegir uno.
            val resultados = productoDao.buscar(query).first().filter { it.productoBaseId == null }
            _resultadosBusqueda.value = resultados
        }
    }

    // ---- Producto seleccionado y sus variantes con cantidad a ingresar ----
    private val _estadoBusqueda = MutableStateFlow<EstadoBusquedaProducto>(EstadoBusquedaProducto.SinBuscar)
    val estadoBusqueda: StateFlow<EstadoBusquedaProducto> = _estadoBusqueda.asStateFlow()

    /** Cantidades a ingresar por variante, mapeadas por id de variante. */
    private val _cantidadesPorVariante = MutableStateFlow<Map<String, String>>(emptyMap())
    val cantidadesPorVariante: StateFlow<Map<String, String>> = _cantidadesPorVariante.asStateFlow()

    fun seleccionarProducto(padre: ProductoEntity) {
        viewModelScope.launch {
            _estadoBusqueda.value = EstadoBusquedaProducto.Buscando
            val variantes = productoDao.observarVariantes(padre.id).first()
                .sortedWith(compareBy({ it.talla }, { it.color }))
            _cantidadesPorVariante.value = emptyMap()
            _estadoBusqueda.value = EstadoBusquedaProducto.Encontrado(padre, variantes)
        }
    }

    /** Busca directamente por código de barras (usado tras escanear). */
    fun buscarPorCodigoBarras(codigo: String) {
        viewModelScope.launch {
            _estadoBusqueda.value = EstadoBusquedaProducto.Buscando
            val padre = productoDao.buscarPorCodigoBarras(codigo)
            if (padre == null) {
                _estadoBusqueda.value = EstadoBusquedaProducto.NoEncontrado
                return@launch
            }
            // buscarPorCodigoBarras ya prioriza el padre (productoBaseId == null);
            // si el producto no tiene padre explícito (raro, dado el flujo actual),
            // se usa el mismo registro encontrado como referencia.
            val padreReal = if (padre.productoBaseId == null) padre else productoDao.obtenerPorId(padre.productoBaseId) ?: padre
            seleccionarProducto(padreReal)
        }
    }

    fun actualizarCantidad(varianteId: String, cantidadTexto: String) {
        if (cantidadTexto.all { it.isDigit() }) {
            _cantidadesPorVariante.value = _cantidadesPorVariante.value + (varianteId to cantidadTexto)
        }
    }

    fun limpiarBusqueda() {
        _estadoBusqueda.value = EstadoBusquedaProducto.SinBuscar
        _cantidadesPorVariante.value = emptyMap()
        _resultadosBusqueda.value = emptyList()
        _mostrandoAgregarColor.value = false
    }

    // ---- Agregar color nuevo al producto ya encontrado, heredando precios por talla ----
    private val _mostrandoAgregarColor = MutableStateFlow(false)
    val mostrandoAgregarColor: StateFlow<Boolean> = _mostrandoAgregarColor.asStateFlow()

    fun abrirAgregarColor() { _mostrandoAgregarColor.value = true }
    fun cerrarAgregarColor() { _mostrandoAgregarColor.value = false }

    /**
     * Tallas que YA existen en alguna variante de este producto (en cualquier
     * color), en el orden estándar. Sirven de referencia: solo se puede agregar
     * un color nuevo usando tallas que el producto ya maneja, porque el precio
     * de cada talla nueva se copia de una variante existente de esa misma talla.
     */
    fun tallasExistentesDelProducto(): List<String> {
        val estado = _estadoBusqueda.value
        if (estado !is EstadoBusquedaProducto.Encontrado) return emptyList()
        val tallasUnicas = estado.variantes.mapNotNull { it.talla }.distinct()
        return TALLAS_DISPONIBLES.filter { it in tallasUnicas }
    }

    /** Estado del guardado de un color nuevo (independiente del registro de entrada normal). */
    private val _estadoAgregarColor = MutableStateFlow<EstadoRegistroEntrada>(EstadoRegistroEntrada.Inactivo)
    val estadoAgregarColor: StateFlow<EstadoRegistroEntrada> = _estadoAgregarColor.asStateFlow()

    /**
     * Crea un color nuevo con las tallas marcadas, copiando el precio (escalones)
     * de una variante existente de cada talla, y registra la entrada inicial de
     * stock con `MovimientoInventarioDao.registrarEntrada`.
     */
    fun agregarColorNuevo(colorNuevo: String, stockPorTalla: Map<String, StockTallaEnCaptura>) {
        val estado = _estadoBusqueda.value
        if (estado !is EstadoBusquedaProducto.Encontrado) return
        if (colorNuevo.isBlank() || stockPorTalla.isEmpty()) {
            _estadoAgregarColor.value = EstadoRegistroEntrada.Error("Escribe un color y marca al menos una talla")
            return
        }

        viewModelScope.launch {
            _estadoAgregarColor.value = EstadoRegistroEntrada.Guardando
            try {
                val usuarioId = app.sessionManager.usuarioActual.value?.id
                val padre = estado.padre

                for ((talla, stockCaptura) in stockPorTalla) {
                    // Variante existente de la misma talla (cualquier color) para copiar su precio.
                    val referencia = estado.variantes.firstOrNull { it.talla == talla }
                        ?: continue // sin referencia de precio para esa talla, se omite

                    val escalonesReferencia = precioEscalonDao.obtenerEscalonesDeProducto(referencia.id)
                    val precioUnidad = escalonesReferencia.find { it.etiqueta == "Unidad" }?.precioUnitario
                        ?: referencia.precioVenta

                    val varianteId = UUID.randomUUID().toString()
                    val nuevaVariante = ProductoEntity(
                        id = varianteId,
                        sku = "SKU-${System.currentTimeMillis()}-$talla-${colorNuevo.take(3)}",
                        codigoBarras = padre.codigoBarras,
                        nombre = padre.nombre,
                        descripcion = padre.descripcion,
                        categoriaId = padre.categoriaId,
                        precioCompra = referencia.precioCompra,
                        precioVenta = precioUnidad,
                        stockActual = 0, // se incrementa abajo vía registrarEntrada, para dejar rastro en la bitácora
                        productoBaseId = padre.id,
                        nombreVariante = "Talla $talla / $colorNuevo",
                        talla = talla,
                        color = colorNuevo.trim()
                    )
                    productoDao.insertar(nuevaVariante)

                    val escalonesNuevos = escalonesReferencia.map { escalon ->
                        PrecioEscalonEntity(
                            id = UUID.randomUUID().toString(),
                            productoId = varianteId,
                            cantidadMinima = escalon.cantidadMinima,
                            etiqueta = escalon.etiqueta,
                            precioUnitario = escalon.precioUnitario
                        )
                    }
                    precioEscalonDao.insertarTodos(escalonesNuevos)

                    val stock = stockCaptura.stockTexto.toIntOrNull() ?: 0
                    if (stock > 0) {
                        movimientoInventarioDao.registrarEntrada(
                            productoId = varianteId,
                            cantidad = stock,
                            costoUnitario = referencia.precioCompra,
                            referenciaId = null,
                            motivo = "Color nuevo: $colorNuevo",
                            usuarioId = usuarioId
                        )
                    }
                }

                // Refresca la lista de variantes del producto para reflejar el color recién creado.
                seleccionarProducto(padre)
                _mostrandoAgregarColor.value = false
                _estadoAgregarColor.value = EstadoRegistroEntrada.Exitoso
            } catch (e: Exception) {
                _estadoAgregarColor.value = EstadoRegistroEntrada.Error("No se pudo agregar el color. Intenta de nuevo.")
            }
        }
    }

    fun reiniciarEstadoAgregarColor() {
        _estadoAgregarColor.value = EstadoRegistroEntrada.Inactivo
    }

    // ---- Registro de la entrada ----
    private val _estadoRegistro = MutableStateFlow<EstadoRegistroEntrada>(EstadoRegistroEntrada.Inactivo)
    val estadoRegistro: StateFlow<EstadoRegistroEntrada> = _estadoRegistro.asStateFlow()

    /** Al menos una variante debe tener una cantidad positiva para poder guardar. */
    fun hayCantidadesValidas(): Boolean =
        _cantidadesPorVariante.value.values.any { (it.toIntOrNull() ?: 0) > 0 }

    fun registrarEntrada(motivo: String) {
        val estado = _estadoBusqueda.value
        if (estado !is EstadoBusquedaProducto.Encontrado) return
        if (!hayCantidadesValidas()) {
            _estadoRegistro.value = EstadoRegistroEntrada.Error("Ingresa al menos una cantidad")
            return
        }

        viewModelScope.launch {
            _estadoRegistro.value = EstadoRegistroEntrada.Guardando
            try {
                val usuarioId = app.sessionManager.usuarioActual.value?.id
                val motivoFinal = motivo.ifBlank { "Entrada de mercadería" }

                for (variante in estado.variantes) {
                    val cantidad = _cantidadesPorVariante.value[variante.id]?.toIntOrNull() ?: 0
                    if (cantidad > 0) {
                        movimientoInventarioDao.registrarEntrada(
                            productoId = variante.id,
                            cantidad = cantidad,
                            costoUnitario = variante.precioCompra,
                            referenciaId = null,
                            motivo = motivoFinal,
                            usuarioId = usuarioId
                        )
                    }
                }

                _estadoRegistro.value = EstadoRegistroEntrada.Exitoso
            } catch (e: Exception) {
                _estadoRegistro.value = EstadoRegistroEntrada.Error("No se pudo registrar la entrada. Intenta de nuevo.")
            }
        }
    }

    fun reiniciarEstadoRegistro() {
        _estadoRegistro.value = EstadoRegistroEntrada.Inactivo
    }
}
