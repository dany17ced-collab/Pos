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
        Index(value = ["codigoBarras"], unique = true),
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
    val nombreVariante: String? = null, // ej: "Talla M / Azul"

    val activo: Boolean = true,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
)
