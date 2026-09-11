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
