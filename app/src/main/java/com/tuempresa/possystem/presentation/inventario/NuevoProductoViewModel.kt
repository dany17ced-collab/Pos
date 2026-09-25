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

    /**
     * COMPARTIDO (por defecto): una sola etiqueta física para toda la prenda,
     * el mismo código en todas las variantes. INDEPENDIENTE: cada combinación
     * talla/color trae su propia etiqueta y se captura su propio código al
     * agregar el color (ver PantallaAgregarColor).
     */
    private val _modoCodigoBarras = MutableStateFlow(ModoCodigoBarras.COMPARTIDO)
    val modoCodigoBarras: StateFlow<ModoCodigoBarras> = _modoCodigoBarras.asStateFlow()
    fun seleccionarModoCodigoBarras(modo: ModoCodigoBarras) { _modoCodigoBarras.value = modo }

    private val _categoriaId = MutableStateFlow<String?>(null)
    val categoriaId: StateFlow<String?> = _categoriaId.asStateFlow()
    fun seleccionarCategoria(id: String?) { _categoriaId.value = id }

    private val _precioCompraTexto = MutableStateFlow("")
    val precioCompraTexto: StateFlow<String> = _precioCompraTexto.asStateFlow()
    fun actualizarPrecioCompra(valor: String) { _precioCompraTexto.value = valor }

    // ---- Plantilla de tallas y PRECIOS: se define UNA VEZ y se comparte entre colores ----
    private val _tallas = MutableStateFlow<Map<String, TallaEnCaptura>>(emptyMap())
    val tallas: StateFlow<Map<String, TallaEnCaptura>> = _tallas.asStateFlow()

    fun alternarTalla(talla: String, activa: Boolean) {
        _tallas.value = if (activa) {
            _tallas.value + (talla to (_tallas.value[talla] ?: TallaEnCaptura(talla = talla)))
        } else {
            _tallas.value - talla
            // Nota: no se limpia el stock ya capturado por color para esa talla;
            // si se vuelve a marcar, el stock previamente ingresado reaparece.
        }
    }

    fun actualizarPrecioEscalon(talla: String, etiqueta: String, precioTexto: String) {
        val actual = _tallas.value[talla] ?: return
        val nuevosEscalones = actual.escalones.map {
            if (it.etiqueta == etiqueta) it.copy(precioTexto = precioTexto) else it
        }
        _tallas.value = _tallas.value + (talla to actual.copy(escalones = nuevosEscalones))
    }

    /** Tallas marcadas, en el orden estándar de TALLAS_DISPONIBLES. */
    fun tallasMarcadasOrdenadas(): List<TallaEnCaptura> =
        TALLAS_DISPONIBLES.mapNotNull { _tallas.value[it] }

    // ---- Colores: cada uno con su propio stock por talla (precios vienen de la plantilla) ----
    private val _colores = MutableStateFlow<List<ColorEnCaptura>>(emptyList())
    val colores: StateFlow<List<ColorEnCaptura>> = _colores.asStateFlow()

    fun agregarColor(color: String, stockPorTalla: Map<String, StockTallaEnCaptura>) {
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
        if (_tallas.value.isEmpty()) return "Marca al menos una talla y define su precio"
        for (tallaCaptura in tallasMarcadasOrdenadas()) {
            val escalonUnidad = tallaCaptura.escalones.find { it.etiqueta == "Unidad" }
            if (escalonUnidad?.precioTexto?.toDoubleOrNull() == null) {
                return "Falta el precio \"Unidad\" en talla ${tallaCaptura.talla}"
            }
        }
        if (_colores.value.isEmpty()) return "Agrega al menos un color con su stock"
        if (_modoCodigoBarras.value == ModoCodigoBarras.INDEPENDIENTE) {
            for (colorCaptura in _colores.value) {
                for (stockCaptura in colorCaptura.stockPorTalla.values) {
                    if (stockCaptura.codigoBarrasTexto.isBlank()) {
                        return "Falta el código de barras de ${colorCaptura.color} talla ${stockCaptura.talla}"
                    }
                }
            }
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
                val codigo = _codigoBarras.value.ifBlank { null }
                val tallasOrdenadas = tallasMarcadasOrdenadas()

                // Precio de referencia del producto padre: el de "Unidad" de la primera
                // talla marcada (solo informativo, no afecta la venta real).
                val precioReferenciaPadre = tallasOrdenadas.firstOrNull()
                    ?.escalones?.find { it.etiqueta == "Unidad" }
                    ?.precioTexto?.toDoubleOrNull() ?: 0.0

                val productoPadreId = UUID.randomUUID().toString()
                val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
                val productoPadre = ProductoEntity(
                    id = productoPadreId,
                    tiendaId = tiendaId,
                    sku = "SKU-${System.currentTimeMillis()}",
                    codigoBarras = codigo,
                    nombre = _nombre.value.trim(),
                    descripcion = _descripcion.value.ifBlank { null },
                    categoriaId = _categoriaId.value,
                    precioCompra = precioCompra,
                    precioVenta = precioReferenciaPadre,
                    stockActual = 0, // el padre no vende stock directo, es solo agrupador
                    productoBaseId = null
                )
                productoDao.insertar(productoPadre)

                for (colorCaptura in _colores.value) {
                    for (tallaCaptura in tallasOrdenadas) {
                        val stockCaptura = colorCaptura.stockPorTalla[tallaCaptura.talla]
                        val stock = stockCaptura?.stockTexto?.toIntOrNull() ?: 0
                        val precioUnidad = tallaCaptura.escalones
                            .find { it.etiqueta == "Unidad" }!!
                            .precioTexto.toDouble()

                        // COMPARTIDO: todas las variantes llevan el mismo código del
                        // producto. INDEPENDIENTE: cada una lleva el código propio
                        // capturado en PantallaAgregarColor para esa talla exacta.
                        val codigoVariante = if (_modoCodigoBarras.value == ModoCodigoBarras.INDEPENDIENTE) {
                            stockCaptura?.codigoBarrasTexto?.ifBlank { null }
                        } else {
                            codigo
                        }

                        val varianteId = UUID.randomUUID().toString()
                        val variante = ProductoEntity(
                            id = varianteId,
                            tiendaId = tiendaId,
                            sku = "SKU-${System.currentTimeMillis()}-${tallaCaptura.talla}-${colorCaptura.color.take(3)}",
                            codigoBarras = codigoVariante,
                            nombre = _nombre.value.trim(),
                            descripcion = _descripcion.value.ifBlank { null },
                            categoriaId = _categoriaId.value,
                            precioCompra = precioCompra,
                            precioVenta = precioUnidad,
                            stockActual = stock,
                            productoBaseId = productoPadreId,
                            nombreVariante = "Talla ${tallaCaptura.talla} / ${colorCaptura.color}",
                            talla = tallaCaptura.talla,
                            color = colorCaptura.color
                        )
                        productoDao.insertar(variante)

                        val escalonesValidos = tallaCaptura.escalones.mapNotNull { escalon ->
                            val precio = escalon.precioTexto.toDoubleOrNull() ?: return@mapNotNull null
                            PrecioEscalonEntity(
                                id = UUID.randomUUID().toString(),
                                productoId = varianteId,
                                cantidadMinima = escalon.cantidadMinima,
                                etiqueta = escalon.etiqueta,
                                precioUnitario = precio
                            )
                        }
                        precioEscalonDao.insertarTodos(escalonesValidos)
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
