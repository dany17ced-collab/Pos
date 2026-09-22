package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Paleta fija de colores que el usuario puede asignar a una categoría. */
val COLORES_CATEGORIA_HEX = listOf(
    "#FF2E63", // Rosa vivo
    "#3EC1FF", // Azul vivo
    "#FFC93E", // Ámbar vivo
    "#3EFFB0", // Verde vivo
    "#B57BFF", // Morado vivo
    "#FF6B8A"  // Rojo vivo
)

/**
 * Gestiona el CRUD de categorías desde Ajustes → Categorías y colores.
 * A diferencia de PantallaInventarioCategorias (que solo LEE categorías para
 * mostrar el grid de inventario), esta pantalla es la única que puede crear,
 * renombrar, recolorear o eliminar categorías.
 */
class CategoriasColoresViewModel(private val app: POSApplication) : ViewModel() {

    private val categoriaDao = app.database.categoriaDao()
    private val productoDao = app.database.productoDao()

    private val _categorias = MutableStateFlow<List<CategoriaEntity>>(emptyList())
    val categorias: StateFlow<List<CategoriaEntity>> = _categorias.asStateFlow()

    /** Cuántos productos usan cada categoría, para avisar antes de eliminar. */
    private val _conteoPorCategoria = MutableStateFlow<Map<String, Int>>(emptyMap())
    val conteoPorCategoria: StateFlow<Map<String, Int>> = _conteoPorCategoria.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            categoriaDao.observarTodas().collect { lista ->
                _categorias.value = lista
                recalcularConteo()
            }
        }
    }

    private suspend fun recalcularConteo() {
        val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
        val productos = productoDao.obtenerTodosActivos(tiendaId)
        _conteoPorCategoria.value = productos
            .mapNotNull { it.categoriaId }
            .groupingBy { it }
            .eachCount()
    }

    fun crearCategoria(nombre: String, colorHex: String) {
        val limpio = nombre.trim()
        if (limpio.isBlank()) {
            _error.value = "El nombre no puede estar vacío"
            return
        }
        if (_categorias.value.any { it.nombre.equals(limpio, ignoreCase = true) }) {
            _error.value = "Ya existe una categoría con ese nombre"
            return
        }
        viewModelScope.launch {
            categoriaDao.insertar(
                CategoriaEntity(
                    id = UUID.randomUUID().toString(),
                    nombre = limpio,
                    colorHex = colorHex
                )
            )
            _error.value = null
        }
    }

    fun actualizarCategoria(categoria: CategoriaEntity, nuevoNombre: String, nuevoColorHex: String) {
        val limpio = nuevoNombre.trim()
        if (limpio.isBlank()) {
            _error.value = "El nombre no puede estar vacío"
            return
        }
        if (_categorias.value.any { it.id != categoria.id && it.nombre.equals(limpio, ignoreCase = true) }) {
            _error.value = "Ya existe una categoría con ese nombre"
            return
        }
        viewModelScope.launch {
            categoriaDao.actualizar(
                categoria.copy(
                    nombre = limpio,
                    colorHex = nuevoColorHex,
                    actualizadoEn = System.currentTimeMillis(),
                    sincronizado = false
                )
            )
            _error.value = null
        }
    }

    fun eliminarCategoria(categoria: CategoriaEntity) {
        viewModelScope.launch {
            categoriaDao.marcarEliminada(categoria.id)
        }
    }

    fun limpiarError() { _error.value = null }
}
