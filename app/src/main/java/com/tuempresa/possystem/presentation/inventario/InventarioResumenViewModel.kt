package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resumen de una categoría para el grid de Inventario: cuántos productos y en qué rango de precio. */
data class ResumenCategoria(
    val categoria: CategoriaEntity,
    val cantidadProductos: Int,
    val precioMinimo: Double,
    val precioMaximo: Double
)

/**
 * Alimenta la pantalla de entrada de Inventario (grid de categorías, total de
 * productos, alerta de stock bajo) de la TIENDA ACTIVA. Las variantes
 * (productoBaseId != null) no se cuentan aparte: solo se cuenta el producto
 * "padre" (o el producto simple sin variantes), igual que en el mockup
 * ("214 productos" es la cantidad de prendas distintas, no de tallas/colores).
 */
class InventarioResumenViewModel(private val app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()
    private val categoriaDao = app.database.categoriaDao()

    private val _totalProductos = MutableStateFlow(0)
    val totalProductos: StateFlow<Int> = _totalProductos.asStateFlow()

    private val _productosStockBajo = MutableStateFlow(0)
    val productosStockBajo: StateFlow<Int> = _productosStockBajo.asStateFlow()

    private val _resumenPorCategoria = MutableStateFlow<List<ResumenCategoria>>(emptyList())
    val resumenPorCategoria: StateFlow<List<ResumenCategoria>> = _resumenPorCategoria.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
            val categorias = categoriaDao.obtenerTodas()
            val productos = productoDao.obtenerTodosActivos(tiendaId)
                .filter { it.productoBaseId == null } // solo padres/simples: una fila por prenda, no por variante

            _totalProductos.value = productos.size
            _productosStockBajo.value = productos.count { it.stockActual <= it.stockMinimo }

            _resumenPorCategoria.value = categorias.mapNotNull { categoria ->
                val deLaCategoria = productos.filter { it.categoriaId == categoria.id }
                if (deLaCategoria.isEmpty()) return@mapNotNull null
                ResumenCategoria(
                    categoria = categoria,
                    cantidadProductos = deLaCategoria.size,
                    precioMinimo = deLaCategoria.minOf { it.precioVenta },
                    precioMaximo = deLaCategoria.maxOf { it.precioVenta }
                )
            }
        }
    }
}
