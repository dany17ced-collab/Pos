package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InventarioViewModel(app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()

    private val _productos = MutableStateFlow<List<ProductoEntity>>(emptyList())
    val productos: StateFlow<List<ProductoEntity>> = _productos.asStateFlow()

    init {
        viewModelScope.launch {
            productoDao.observarProductosActivos().collect { _productos.value = it }
        }
    }

    /**
     * Elimina (soft-delete) un producto padre junto con todas sus variantes
     * (talla/color). Útil para corregir un producto capturado incorrectamente,
     * ej. si se escribieron varios colores juntos en un solo campo por error.
     */
    fun eliminarProductoConVariantes(productoPadreId: String) {
        viewModelScope.launch {
            val variantes = productoDao.observarVariantes(productoPadreId).first()
            variantes.forEach { variante -> productoDao.marcarEliminado(variante.id) }
            productoDao.marcarEliminado(productoPadreId)
        }
    }
}
