package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventarioViewModel(private val app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val productos: StateFlow<List<ProductoEntity>> = app.sessionManager.tiendaActiva
        .flatMapLatest { tienda ->
            if (tienda == null) flowOf(emptyList()) else productoDao.observarProductosActivos(tienda.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
