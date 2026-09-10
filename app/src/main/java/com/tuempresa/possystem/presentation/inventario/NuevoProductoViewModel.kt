package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.data.local.entity.PrecioEscalonEntity
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class EstadoGuardado {
    object Inactivo : EstadoGuardado()
    object Guardando : EstadoGuardado()
    object Exitoso : EstadoGuardado()
    data class Error(val mensaje: String) : EstadoGuardado()
}

class NuevoProductoViewModel(private val app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()
    private val precioEscalonDao = app.database.precioEscalonDao()
    private val categoriaDao = app.database.categoriaDao()

    private val _categorias = MutableStateFlow<List<CategoriaEntity>>(emptyList())
    val categorias: StateFlow<List<CategoriaEntity>> = _categorias.asStateFlow()

    init {
        viewModelScope.launch {
            categoriaDao.observarTodas().collect { _categorias.value = it }
        }
    }

    // ---- Datos generales ----
    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()
    fun actualizarNombre(valor: String) { _nombre.value = valor }

    private val _descripcion = MutableStateFlow("")
    val descripcion: StateFlow<String> = _descripcion.asStateFlow()
    fun actualizarDescripcion(valor: String) { _descripcion.value = valor }

    private val _codigoBarras = MutableStateFlow("")
    val codigoBarras: StateFlow<String> = _codigoBarras.asStateFlow()
    fun actualizarCodigoBarras(valor: String) { _codigoBarras.value = valor }

    private val _categoriaId = MutableStateFlow<String?>(null)
    val categoriaId: StateFlow<String?> = _categoriaId.asStateFlow()
    fun seleccionarCategoria(id: String?) { _categoriaId.value = id }

    private val _precioCompraTexto = MutableStateFlow("")
    val precioCompraTexto: StateFlow<String> = _precioCompraTexto.asStateFlow()
    fun actualizarPrecioCompra(valor: String) { _precioCompraTexto.value = valor }

    // ---- Escalones de precio (aplican a todas las variantes) ----
    private val _escalones = MutableStateFlow(escalonesPorDefecto())
    val escalones: StateFlow<List<EscalonEnCaptura>> = _escalones.asStateFlow()

    fun actualizarPrecioEscalon(etiqueta: String, precioTexto: String) {
        _escalones.value = _escalones.value.map {
            if (it.etiqueta == etiqueta) it.copy(precioTexto = precioTexto) else it
        }
    }

    // ---- Colores capturados (cada uno con sus tallas y stock) ----
    private val _colores = MutableStateFlow<List<ColorEnCaptura>>(emptyList())
    val colores: StateFlow<List<ColorEnCaptura>> = _colores.asStateFlow()

    fun agregarColor(color: String, stockPorTalla: Map<String, Int>) {
        if (color.isBlank() || stockPorTalla.isEmpty()) return
        _colores.value = _colores.value + ColorEnCaptura(
            id = UUID.randomUUID().toString(),
            color = color.trim(),
            stockPorTalla = stockPorTalla
        )
    }

    fun quitarColor(id: String) {
        _colores.value = _colores.value.filterNot { it.id == id }
    }

    // ---- Guardado ----
    private val _estadoGuardado = MutableStateFlow<EstadoGuardado>(EstadoGuardado.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardado> = _estadoGuardado.asStateFlow()

    fun validar(): String? {
        if (_nombre.value.isBlank()) return "El nombre es obligatorio"
        if (_precioCompraTexto.value.toDoubleOrNull() == null) return "El precio de compra no es válido"
        if (_colores.value.isEmpty()) return "Agrega al menos un color con tallas"
        val escalonUnidad = _escalones.value.find { it.etiqueta == "Unidad" }
        if (escalonUnidad?.precioTexto?.toDoubleOrNull() == null) {
            return "El precio de \"Unidad\" es obligatorio"
        }
        return null
    }

    fun guardarProducto() {
        val error = validar()
        if (error != null) {
            _estadoGuardado.value = EstadoGuardado.Error(error)
            return
        }

        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardado.Guardando
            try {
                val precioCompra = _precioCompraTexto.value.toDouble()
                val precioUnidadReferencia = _escalones.value
                    .find { it.etiqueta == "Unidad" }!!
                    .precioTexto.toDouble()

                val productoPadreId = UUID.randomUUID().toString()
                val codigo = _codigoBarras.value.ifBlank { null }

                val productoPadre = ProductoEntity(
                    id = productoPadreId,
                    sku = "SKU-${System.currentTimeMillis()}",
                    codigoBarras = codigo,
                    nombre = _nombre.value.trim(),
                    descripcion = _descripcion.value.ifBlank { null },
                    categoriaId = _categoriaId.value,
                    precioCompra = precioCompra,
                    precioVenta = precioUnidadReferencia,
                    stockActual = 0, // el padre no vende stock directo, es solo agrupador
                    productoBaseId = null
                )
                productoDao.insertar(productoPadre)

                val escalonesValidos = _escalones.value.mapNotNull { escalon ->
                    val precio = escalon.precioTexto.toDoubleOrNull() ?: return@mapNotNull null
                    escalon to precio
                }

                for (colorCaptura in _colores.value) {
                    for ((talla, stock) in colorCaptura.stockPorTalla) {
                        val varianteId = UUID.randomUUID().toString()
                        val variante = ProductoEntity(
                            id = varianteId,
                            sku = "SKU-${System.currentTimeMillis()}-$talla-${colorCaptura.color.take(3)}",
                            codigoBarras = codigo, // comparte el mismo código de barras que el padre
                            nombre = _nombre.value.trim(),
                            descripcion = _descripcion.value.ifBlank { null },
                            categoriaId = _categoriaId.value,
                            precioCompra = precioCompra,
                            precioVenta = precioUnidadReferencia,
                            stockActual = stock,
                            productoBaseId = productoPadreId,
                            nombreVariante = "Talla $talla / ${colorCaptura.color}",
                            talla = talla,
                            color = colorCaptura.color
                        )
                        productoDao.insertar(variante)

                        val escalonesDeVariante = escalonesValidos.map { (escalon, precio) ->
                            PrecioEscalonEntity(
                                id = UUID.randomUUID().toString(),
                                productoId = varianteId,
                                cantidadMinima = escalon.cantidadMinima,
                                etiqueta = escalon.etiqueta,
                                precioUnitario = precio
                            )
                        }
                        precioEscalonDao.insertarTodos(escalonesDeVariante)
                    }
                }

                _estadoGuardado.value = EstadoGuardado.Exitoso
            } catch (e: Exception) {
                _estadoGuardado.value = EstadoGuardado.Error("No se pudo guardar el producto. Intenta de nuevo.")
            }
        }
    }

    fun reiniciarEstado() {
        _estadoGuardado.value = EstadoGuardado.Inactivo
    }
}
