package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class MovimientoInventarioDao {

    @Insert
    protected abstract suspend fun insertar(movimiento: MovimientoInventarioEntity)

    @Query("SELECT stockActual FROM productos WHERE id = :productoId")
    protected abstract suspend fun obtenerStockActual(productoId: String): Int?

    @Query(
        "UPDATE productos SET stockActual = :nuevoStock, sincronizado = 0, actualizadoEn = :ahora WHERE id = :productoId"
    )
    protected abstract suspend fun actualizarStock(productoId: String, nuevoStock: Int, ahora: Long)

    /**
     * Registra una ENTRADA (compra a proveedor): incrementa stock y opcionalmente
     * actualiza el costo unitario del producto (útil si el precio de compra cambió).
     */
    @Transaction
    open suspend fun registrarEntrada(
        productoId: String,
        cantidad: Int,
        costoUnitario: Double?,
        referenciaId: String?,
        motivo: String?,
        usuarioId: String?
    ) {
        require(cantidad > 0) { "La cantidad de una entrada debe ser positiva" }
        val stockAntes = obtenerStockActual(productoId) ?: throw IllegalStateException("Producto no existe")
        val ahora = System.currentTimeMillis()
        val stockNuevo = stockAntes + cantidad

        actualizarStock(productoId, stockNuevo, ahora)
        insertar(
            MovimientoInventarioEntity(
                id = UUID.randomUUID().toString(),
                productoId = productoId,
                tipo = TipoMovimiento.ENTRADA,
                cantidad = cantidad,
                stockAnterior = stockAntes,
                stockNuevo = stockNuevo,
                costoUnitario = costoUnitario,
                referenciaId = referenciaId,
                motivo = motivo,
                usuarioId = usuarioId,
                fecha = ahora
            )
        )
    }

    /**
     * Registra una SALIDA (merma, pérdida, robo). Falla si no hay stock suficiente.
     */
    @Transaction
    open suspend fun registrarSalida(
        productoId: String,
        cantidad: Int,
        motivo: String,
        usuarioId: String?
    ) {
        require(cantidad > 0) { "La cantidad de una salida debe ser positiva" }
        val stockAntes = obtenerStockActual(productoId) ?: throw IllegalStateException("Producto no existe")
        if (stockAntes < cantidad) {
            throw StockInsuficienteException(
                productoId = productoId,
                nombreProducto = productoId,
                stockDisponible = stockAntes,
                cantidadSolicitada = cantidad
            )
        }
        val ahora = System.currentTimeMillis()
        val stockNuevo = stockAntes - cantidad

        actualizarStock(productoId, stockNuevo, ahora)
        insertar(
            MovimientoInventarioEntity(
                id = UUID.randomUUID().toString(),
                productoId = productoId,
                tipo = TipoMovimiento.SALIDA,
                cantidad = cantidad,
                stockAnterior = stockAntes,
                stockNuevo = stockNuevo,
                motivo = motivo,
                usuarioId = usuarioId,
                fecha = ahora
            )
        )
    }

    /**
     * AJUSTE manual: fija el stock a un valor exacto (ej: después de un conteo físico),
     * en vez de sumar/restar una cantidad. Calcula automáticamente la diferencia.
     */
    @Transaction
    open suspend fun registrarAjuste(
        productoId: String,
        stockContado: Int,
        motivo: String,
        usuarioId: String?
    ) {
        require(stockContado >= 0) { "El stock contado no puede ser negativo" }
        val stockAntes = obtenerStockActual(productoId) ?: throw IllegalStateException("Producto no existe")
        val ahora = System.currentTimeMillis()
        val diferencia = kotlin.math.abs(stockContado - stockAntes)

        actualizarStock(productoId, stockContado, ahora)
        insertar(
            MovimientoInventarioEntity(
                id = UUID.randomUUID().toString(),
                productoId = productoId,
                tipo = TipoMovimiento.AJUSTE,
                cantidad = diferencia,
                stockAnterior = stockAntes,
                stockNuevo = stockContado,
                motivo = motivo,
                usuarioId = usuarioId,
                fecha = ahora
            )
        )
    }

    @Query("SELECT * FROM movimientos_inventario WHERE productoId = :productoId ORDER BY fecha DESC")
    abstract fun observarHistorialDeProducto(productoId: String): Flow<List<MovimientoInventarioEntity>>

    @Query("SELECT * FROM movimientos_inventario WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC")
    abstract fun observarEnRango(desde: Long, hasta: Long): Flow<List<MovimientoInventarioEntity>>

    @Query("SELECT * FROM movimientos_inventario WHERE sincronizado = 0")
    abstract suspend fun obtenerPendientesDeSincronizar(): List<MovimientoInventarioEntity>

    @Query("UPDATE movimientos_inventario SET sincronizado = 1 WHERE id IN (:ids)")
    abstract suspend fun marcarSincronizados(ids: List<String>)
}
