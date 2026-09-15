package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Configuración global de la tienda que se imprime en TODAS las boletas.
 * Es una tabla de una sola fila (id fijo "config") — no hay una configuración
 * distinta por venta, es un ajuste único que aplica a todo lo que se emita.
 */
@Entity(tableName = "configuracion_tienda")
data class ConfiguracionTiendaEntity(
    @PrimaryKey
    val id: String = ID_UNICO,

    val nombreTienda: String = "",
    val eslogan: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val ruc: String = "",
    val piePagina: String = "¡Gracias por su compra!",

    /** Ruta absoluta al archivo del logo en almacenamiento interno de la app, o null si no hay logo. */
    val rutaLogo: String? = null,

    /** Dirección MAC de la última impresora Bluetooth usada, para reconectar sin pedirla de nuevo. */
    val macImpresora: String? = null,
    val nombreImpresora: String? = null,

    val actualizadoEn: Long = System.currentTimeMillis()
) {
    companion object {
        const val ID_UNICO = "config"
    }
}
