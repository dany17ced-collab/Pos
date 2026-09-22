package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una sucursal/local física del negocio. Cada tienda tiene su propio inventario,
 * ventas y configuración de boleta (nombre, dirección, RUC, impresora), pero
 * comparte la misma base de datos y usuarios de la app.
 *
 * La primera tienda ("Tienda Principal") se crea automáticamente al migrar
 * desde versiones anteriores de la app que no tenían soporte multitienda,
 * para que ningún producto o venta existente quede huérfano.
 */
@Entity(tableName = "tiendas")
data class TiendaEntity(
    @PrimaryKey
    val id: String,

    val nombre: String,
    val activa: Boolean = true,

    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),

    val sincronizado: Boolean = false,
    val eliminado: Boolean = false
) {
    companion object {
        /** id fijo de la tienda creada automáticamente al migrar desde v9. */
        const val ID_TIENDA_PRINCIPAL = "tienda-principal"
    }
}
