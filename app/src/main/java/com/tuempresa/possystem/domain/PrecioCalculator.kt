package com.tuempresa.possystem.domain

import com.tuempresa.possystem.data.local.dao.PrecioEscalonDao
import com.tuempresa.possystem.data.local.entity.ProductoEntity

/**
 * Resuelve el precio unitario correcto para una variante (ProductoEntity hijo con
 * talla/color) dada una cantidad. Si existen escalones de precio configurados para
 * esa variante, usa el escalón correspondiente a la cantidad; si no hay ninguno
 * configurado, cae de vuelta al precioVenta base del producto.
 */
class PrecioCalculator(
    private val precioEscalonDao: PrecioEscalonDao
) {
    suspend fun calcularPrecioUnitario(producto: ProductoEntity, cantidad: Int): Double {
        val escalon = precioEscalonDao.obtenerEscalonParaCantidad(producto.id, cantidad)
        return escalon?.precioUnitario ?: producto.precioVenta
    }

    /** Etiqueta del escalón aplicado ("Unidad", "Docena", etc.) o "Unidad" si no hay escalones. */
    suspend fun obtenerEtiquetaEscalon(producto: ProductoEntity, cantidad: Int): String {
        val escalon = precioEscalonDao.obtenerEscalonParaCantidad(producto.id, cantidad)
        return escalon?.etiqueta ?: "Unidad"
    }
}
