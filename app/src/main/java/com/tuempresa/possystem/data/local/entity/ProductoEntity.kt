package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Producto del inventario. Si tiene variantes (talla/color), cada variante
 * es en sí misma otro ProductoEntity con `productoBaseId` apuntando al "padre".
 * Esto permite que cada variante tenga su propio SKU, código de barras y stock,
 * que es como se maneja en la práctica (cada talla/color se vende y controla por separado).
 *
 * Para ropa: el producto "padre" (productoBaseId = null) representa la prenda en
 * general (ej. "Blusa floral") y comparte el mismo código de barras para escanear.
 * Cada variante hija tiene su propia `talla` y `color`, su propio stock, y sus propios
 * escalones de precio por cantidad en PrecioEscalonEntity (unidad/cuarto/media/docena).
 * `precioVenta` en la variante actúa como precio de referencia (equivalente al escalón
 * "Unidad") y respaldo por si aún no se configuran escalones para esa variante.
 */
@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoBaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoriaId"),
        Index("productoBaseId"),
        // codigoBarras YA NO es único: el producto padre y todas sus variantes
        // (tallas/colores) comparten el mismo código de barras físico impreso
        // en la prenda; se escanea el código y luego se elige talla/color en pantalla.
        Index(value = ["codigoBarras"]),
        Index(value = ["sku"], unique = true)
    ]
)
data class ProductoEntity(
    @PrimaryKey
    val id: String,

    val sku: String,
    val codigoBarras: String?, // puede no tener código de barras (venta por nombre/SKU manual)
    val nombre: String,
    val descripcion: String? = null,
    val categoriaId: String?,

    val precioCompra: Double,
    val precioVenta: Double,
    val impuestoPorcentaje: Double = 0.0, // ej: 16.0 para IVA 16%

    val stockActual: Int,
    val stockMinimo: Int = 0,
    val stockMaximo: Int? = null,

    val fotoUrl: String? = null, // URL en Supabase Storage; null hasta que sincronice

    // ---- Variantes ----
    val productoBaseId: String? = null, // null = producto simple o producto "padre" de variantes
    val nombreVariante: String? = null, // ej: "Talla M / Azul" (para mostrar en listas rápido)
    val talla: String? = null,          // "2","4","6","8","10","12","14","16" — solo en variantes hijas
    val color: String? = null,          // solo en variantes hijas

    val activo: Boolean = true,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
