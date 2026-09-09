package com.tuempresa.possystem.presentation.venta

import com.tuempresa.possystem.data.local.entity.ProductoEntity

/**
 * Una línea del carrito de venta, en memoria, antes de confirmarse la venta.
 * `producto` es siempre la VARIANTE exacta (talla/color), nunca el producto padre.
 */
data class LineaCarrito(
    val id: String, // id local único de esta línea (no del producto), para poder editar/quitar
    val producto: ProductoEntity,
    val cantidad: Int,
    val precioUnitario: Double,
    val etiquetaEscalon: String // "Unidad", "Docena", etc. — informativo para el vendedor
) {
    val subtotal: Double get() = cantidad * precioUnitario
}
