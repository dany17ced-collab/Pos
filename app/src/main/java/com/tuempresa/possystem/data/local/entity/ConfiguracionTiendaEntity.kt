package com.tuempresa.possystem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Configuración de boleta de UNA tienda/sucursal. Antes de multitienda esta
 * tabla tenía una sola fila con id fijo "config"; ahora tiene una fila por
 * cada [TiendaEntity], identificada por [tiendaId], porque cada sucursal
 * puede tener su propio nombre, dirección, RUC e impresora impresos en su
 * boleta. El [id] de cada fila debe generarse explícitamente (ej. un UUID)
 * al crearla; no hay valor por defecto para evitar que dos tiendas terminen
 * compartiendo la misma fila.
 */
@Entity(
    tableName = "configuracion_tienda",
    foreignKeys = [
        ForeignKey(
            entity = TiendaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tiendaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tiendaId"], unique = true)]
)
data class ConfiguracionTiendaEntity(
    @PrimaryKey
    val id: String,

    val tiendaId: String,

    val nombreTienda: String = "",
    val eslogan: String = "",

    /** Descripción opcional de la tienda (qué ofrece, especialidad, etc.). Se imprime debajo del eslogan solo si no está vacía. */
    val descripcion: String = "",

    val direccion: String = "",
    val telefono: String = "",
    val ruc: String = "",
    val piePagina: String = "¡Gracias por su compra!",

    /** Ruta absoluta al archivo del logo en almacenamiento interno de la app, o null si no hay logo. */
    val rutaLogo: String? = null,

    /** Dirección MAC de la última impresora Bluetooth usada, para reconectar sin pedirla de nuevo. */
    val macImpresora: String? = null,
    val nombreImpresora: String? = null,

    /**
     * Link único que se codifica como QR en el pie de la boleta (ej. una página
     * tipo "linktree" con Instagram/WhatsApp/Facebook). Null = no se imprime QR de redes.
     */
    val linkRedesSociales: String? = null,

    /** Días de plazo para aceptar un cambio por talla, contados desde la fecha de venta. */
    val diasPlazoCambioTalla: Int = 7,

    /** Días de plazo para aceptar un cambio por falla de fábrica, contados desde la fecha de venta. */
    val diasPlazoCambioFabrica: Int = 30,

    /**
     * Texto de política de cambios/devoluciones (ley aplicable, plazo, condiciones
     * como "prenda sin lavar"). Se imprime como advertencia debajo del QR de
     * cambios/devoluciones, solo si no está vacío.
     */
    val politicaCambios: String = "",

    val actualizadoEn: Long = System.currentTimeMillis()
) {
    companion object {
        const val ID_UNICO = "config"
    }
}
