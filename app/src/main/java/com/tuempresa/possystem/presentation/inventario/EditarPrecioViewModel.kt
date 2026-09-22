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

/** Un escalón de precio en edición, con el valor como texto para el TextField. */
data class EscalonEnEdicion(
    val id: String,
    val cantidadMinima: Int,
    val etiqueta: String,
    var precioTexto: String
)

/** Una variante (talla/color) del producto padre, con su precio y escalones editables. */
data class VarianteEnEdicion(
    val producto: ProductoEntity,
    var precioTexto: String,
    val escalones: List<EscalonEnEdicion>
)

sealed class EstadoGuardadoPrecio {
    object Inactivo : EstadoGuardadoPrecio()
    object Guardando : EstadoGuardadoPrecio()
    object Exitoso : EstadoGuardadoPrecio()
    data class Error(val mensaje: String) : EstadoGuardadoPrecio()
}

/**
 * Carga el producto seleccionado desde Inventario (padre o simple) junto con
 * sus variantes (si las tiene) y los escalones de precio de cada una, para
 * permitir el ajuste de precios por temporada (subida o bajada general).
 */
class EditarPrecioViewModel(app: POSApplication, private val productoId: String) : ViewModel() {

    private val productoDao = app.database.productoDao()
    private val precioEscalonDao = app.database.precioEscalonDao()

    private val _producto = MutableStateFlow<ProductoEntity?>(null)
    val producto: StateFlow<ProductoEntity?> = _producto.asStateFlow()

    private val _variantes = MutableStateFlow<List<VarianteEnEdicion>>(emptyList())
    val variantes: StateFlow<List<VarianteEnEdicion>> = _variantes.asStateFlow()

    private val _estadoGuardado = MutableStateFlow<EstadoGuardadoPrecio>(EstadoGuardadoPrecio.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardadoPrecio> = _estadoGuardado.asStateFlow()

    init {
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            val productoBase = productoDao.obtenerPorId(productoId) ?: return@launch
            _producto.value = productoBase

            val hijas = productoDao.observarVariantes(productoId).first()

            if (hijas.isEmpty()) {
                // Producto simple: se edita él mismo como si fuera su propia "variante".
                val escalones = precioEscalonDao.obtenerEscalonesDeProducto(productoId)
                _variantes.value = listOf(
                    VarianteEnEdicion(
                        producto = productoBase,
                        precioTexto = formatearPrecio(productoBase.precioVenta),
                        escalones = escalones.map { it.aEdicion() }
                    )
                )
            } else {
                _variantes.value = hijas
                    .sortedWith(compareBy({ it.talla ?: "" }, { it.color ?: "" }))
                    .map { hija ->
                        val escalones = precioEscalonDao.obtenerEscalonesDeProducto(hija.id)
                        VarianteEnEdicion(
                            producto = hija,
                            precioTexto = formatearPrecio(hija.precioVenta),
                            escalones = escalones.map { it.aEdicion() }
                        )
                    }
            }
        }
    }

    private fun formatearPrecio(valor: Double): String {
        return if (valor == valor.toLong().toDouble()) valor.toLong().toString() else valor.toString()
    }

    private fun PrecioEscalonEntity.aEdicion() = EscalonEnEdicion(
        id = id,
        cantidadMinima = cantidadMinima,
        etiqueta = etiqueta,
        precioTexto = formatearPrecio(precioUnitario)
    )

    fun actualizarPrecioBase(varianteId: String, nuevoTexto: String) {
        _variantes.value = _variantes.value.map { v ->
            if (v.producto.id == varianteId) v.copy(precioTexto = nuevoTexto) else v
        }
    }

    fun actualizarPrecioEscalon(varianteId: String, escalonId: String, nuevoTexto: String) {
        _variantes.value = _variantes.value.map { v ->
            if (v.producto.id == varianteId) {
                v.copy(escalones = v.escalones.map { e ->
                    if (e.id == escalonId) e.copy(precioTexto = nuevoTexto) else e
                })
            } else v
        }
    }

    /**
     * Aplica un incremento o descuento porcentual a TODOS los precios visibles
     * (precio base y escalones de todas las variantes) — útil para ajustes
     * rápidos de temporada, ej. +10% o -15% en todo el producto de una vez.
     */
    fun aplicarAjustePorcentual(porcentaje: Double) {
        val factor = 1.0 + (porcentaje / 100.0)
        _variantes.value = _variantes.value.map { v ->
            val precioBase = v.precioTexto.toDoubleOrNull()
            val nuevoBase = if (precioBase != null) formatearPrecio(precioBase * factor) else v.precioTexto
            v.copy(
                precioTexto = nuevoBase,
                escalones = v.escalones.map { e ->
                    val actual = e.precioTexto.toDoubleOrNull()
                    if (actual != null) e.copy(precioTexto = formatearPrecio(actual * factor)) else e
                }
            )
        }
    }

    fun guardar() {
        val variantesActuales = _variantes.value
        if (variantesActuales.isEmpty()) return

        // Validación: todos los precios deben ser números válidos y positivos.
        for (v in variantesActuales) {
            val precio = v.precioTexto.toDoubleOrNull()
            if (precio == null || precio <= 0.0) {
                _estadoGuardado.value = EstadoGuardadoPrecio.Error("Revisa el precio de ${nombreVariante(v)}: debe ser un número mayor a 0.")
                return
            }
            for (e in v.escalones) {
                val precioEscalon = e.precioTexto.toDoubleOrNull()
                if (precioEscalon == null || precioEscalon <= 0.0) {
                    _estadoGuardado.value = EstadoGuardadoPrecio.Error(
                        "Revisa el precio \"${e.etiqueta}\" de ${nombreVariante(v)}: debe ser un número mayor a 0."
                    )
                    return
                }
            }
        }

        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardadoPrecio.Guardando
            try {
                for (v in variantesActuales) {
                    val nuevoPrecio = v.precioTexto.toDouble()
                    productoDao.actualizarPrecioVenta(v.producto.id, nuevoPrecio)

                    for (e in v.escalones) {
                        val nuevoPrecioEscalon = e.precioTexto.toDouble()
                        precioEscalonDao.actualizar(
                            PrecioEscalonEntity(
                                id = e.id,
                                productoId = v.producto.id,
                                cantidadMinima = e.cantidadMinima,
                                etiqueta = e.etiqueta,
                                precioUnitario = nuevoPrecioEscalon,
                                sincronizado = false
                            )
                        )
                    }
                }
                _estadoGuardado.value = EstadoGuardadoPrecio.Exitoso
            } catch (ex: Exception) {
                _estadoGuardado.value = EstadoGuardadoPrecio.Error(ex.message ?: "No se pudo guardar el precio.")
            }
        }
    }

    private fun nombreVariante(v: VarianteEnEdicion): String {
        val p = v.producto
        return if (p.talla != null || p.color != null) {
            listOfNotNull(p.talla?.let { "T$it" }, p.color).joinToString(" / ")
        } else {
            p.nombre
        }
    }
}
