package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tuempresa.possystem.data.local.entity.CambioEntity
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class CambioDao {

    @Insert
    protected abstract suspend fun insertarCambio(cambio: CambioEntity)

    @Insert
    protected abstract suspend fun insertarMovimientos(movimientos: List<MovimientoInventarioEntity>)

    @Query("SELECT stockActual FROM productos WHERE id = :productoId")
    protected abstract suspend fun obtenerStockActual(productoId: String): Int?

    @Query(
        """
        UPDATE productos 
        SET stockActual = stockActual + :cantidad, sincronizado = 0, actualizadoEn = :ahora
        WHERE id = :productoId
        """
    )
    protected abstract suspend fun incrementarStock(productoId: String, cantidad: Int, ahora: Long)

    @Query(
        """
        UPDATE productos 
        SET stockActual = stockActual - :cantidad, sincronizado = 0, actualizadoEn = :ahora
        WHERE id = :productoId AND stockActual >= :cantidad
        """
    )
    protected abstract suspend fun descontarStockSiHayDisponible(
        productoId: String,
        cantidad: Int,
        ahora: Long
    ): Int

    /**
     * Registra el cambio de forma atómica: reingresa el stock de la prenda
     * devuelta, descuenta el stock de la nueva variante entregada (validando
     * que haya suficiente), y deja rastro en la bitácora de movimientos.
     *
     * Si no hay stock suficiente de la variante entregada, se lanza
     * StockInsuficienteException y Room revierte todo (igual que en una venta).
     */
    @Transaction
    open suspend fun registrarCambio(cambio: CambioEntity) {
        val ahora = System.currentTimeMillis()

        // 1) Reingresa la prenda devuelta al inventario.
        val stockAntesDevuelto = obtenerStockActual(cambio.productoDevueltoId) ?: 0
        incrementarStock(cambio.productoDevueltoId, cambio.cantidad, ahora)

        // 2) Descuenta la nueva variante entregada — valida stock dentro de la transacción.
        val stockAntesEntregado = obtenerStockActual(cambio.productoEntregadoId)
            ?: throw IllegalStateException("Producto ${cambio.productoEntregadoId} no existe")

        val filasAfectadas = descontarStockSiHayDisponible(
            productoId = cambio.productoEntregadoId,
            cantidad = cambio.cantidad,
            ahora = ahora
        )
        if (filasAfectadas == 0) {
            throw StockInsuficienteException(
                productoId = cambio.productoEntregadoId,
                nombreProducto = cambio.nombreProductoEntregado,
                stockDisponible = stockAntesEntregado,
                cantidadSolicitada = cambio.cantidad
            )
        }

        val movimientos = listOf(
            MovimientoInventarioEntity(
                id = UUID.randomUUID().toString(),
                productoId = cambio.productoDevueltoId,
                tipo = TipoMovimiento.DEVOLUCION,
                cantidad = cambio.cantidad,
                stockAnterior = stockAntesDevuelto,
                stockNuevo = stockAntesDevuelto + cambio.cantidad,
                referenciaId = cambio.id,
                motivo = "Cambio: ${cambio.motivo}",
                usuarioId = cambio.usuarioId,
                fecha = ahora
            ),
            MovimientoInventarioEntity(
                id = UUID.randomUUID().toString(),
                productoId = cambio.productoEntregadoId,
                tipo = TipoMovimiento.CAMBIO_SALIDA,
                cantidad = cambio.cantidad,
                stockAnterior = stockAntesEntregado,
                stockNuevo = stockAntesEntregado - cambio.cantidad,
                referenciaId = cambio.id,
                motivo = "Cambio: ${cambio.motivo}",
                usuarioId = cambio.usuarioId,
                fecha = ahora
            )
        )

        insertarCambio(cambio)
        insertarMovimientos(movimientos)
    }

    @Query("SELECT * FROM cambios WHERE ventaOriginalId = :ventaId ORDER BY fecha DESC")
    abstract suspend fun obtenerCambiosDeVenta(ventaId: String): List<CambioEntity>

    @Query("SELECT * FROM cambios ORDER BY fecha DESC")
    abstract fun observarTodos(): Flow<List<CambioEntity>>

    /** Cambios por falla de fábrica en un rango de fechas — para el reporte de control de calidad. */
    @Query(
        """
        SELECT * FROM cambios 
        WHERE motivo = 'FALLA_FABRICA' AND fecha BETWEEN :desde AND :hasta 
        ORDER BY fecha DESC
        """
    )
    abstract suspend fun obtenerFallasDeFabricaEnRango(desde: Long, hasta: Long): List<CambioEntity>

    @Query("SELECT * FROM cambios WHERE id = :id LIMIT 1")
    abstract suspend fun obtenerPorId(id: String): CambioEntity?
}
