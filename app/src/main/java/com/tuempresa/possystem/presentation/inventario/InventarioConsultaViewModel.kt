package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Solo lectura: para que el vendedor consulte existencias por talla/color sin
 * poder editar precios ni eliminar productos (eso es exclusivo de Admin, ver
 * InventarioViewModel.kt).
 */
class InventarioConsultaViewModel(app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()

    private val _productos = MutableStateFlow<List<ProductoEntity>>(emptyList())
    val productos: StateFlow<List<ProductoEntity>> = _productos.asStateFlow()

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    // Variantes ya cargadas por producto padre, para no volver a consultar la
    // base de datos cada vez que el usuario expande/contrae la misma tarjeta.
    private val _variantesPorProducto = MutableStateFlow<Map<String, List<ProductoEntity>>>(emptyMap())
    val variantesPorProducto: StateFlow<Map<String, List<ProductoEntity>>> = _variantesPorProducto.asStateFlow()

    init {
        viewModelScope.launch {
            productoDao.observarProductosActivos().collect { lista ->
                _productos.value = if (_textoBusqueda.value.isBlank()) {
                    lista
                } else {
                    filtrar(lista, _textoBusqueda.value)
                }
            }
        }
    }

    fun buscar(texto: String) {
        _textoBusqueda.value = texto
        viewModelScope.launch {
            val todos = productoDao.obtenerTodosActivos().filter { it.productoBaseId == null }
            _productos.value = if (texto.isBlank()) todos else filtrar(todos, texto)
        }
    }

    private fun filtrar(lista: List<ProductoEntity>, texto: String): List<ProductoEntity> {
        return lista.filter {
            it.nombre.contains(texto, ignoreCase = true) ||
                it.sku.contains(texto, ignoreCase = true) ||
                (it.codigoBarras?.contains(texto, ignoreCase = true) == true)
        }
    }

    /** Carga (una sola vez) las variantes de un producto padre, cuando el usuario lo expande. */
    fun cargarVariantesSiHaceFalta(productoPadreId: String) {
        if (_variantesPorProducto.value.containsKey(productoPadreId)) return
        viewModelScope.launch {
            productoDao.observarVariantes(productoPadreId).collect { variantes ->
                _variantesPorProducto.value = _variantesPorProducto.value + (productoPadreId to variantes)
            }
        }
    }
}
