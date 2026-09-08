package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.TipoMovimiento
import com.tuempresa.possystem.data.local.entity.VentaEntity
import kotlinx.coroutines.flow.Flow

/**
 * Excepción específica para cuando no hay stock suficiente al confirmar una venta.
 * Se lanza DENTRO de la transacción para forzar el rollback automático de Room.
 */
class StockInsuficienteException(
    val productoId: String,
    val nombreProducto: String,
    val stockDisponible: Int,
    val cantidadSolicitada: Int
) : Exception(
    "Stock insuficiente para \"$nombreProducto\": disponible $stockDisponible, solicitado $cantidadSolicitada"
)

@Dao
abstract class VentaDao {

    @Insert
    protected abstract suspend fun insertarVenta(venta: VentaEntity)

    @Insert
    protected abstract suspend fun insertarDetalles(detalles: List<DetalleVentaEntity>)

    @Insert
    protected abstract suspend fun insertarMovimientos(movimientos: List<MovimientoInventarioEntity>)

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

    @Query("SELECT stockActual FROM productos WHERE id = :productoId")
    protected abstract suspend fun obtenerStockActual(productoId: String): Int?

    @Update
    abstract suspend fun actualizar(venta: VentaEntity)

    /**
     * ÚNICO punto de entrada para registrar una venta. Es atómica: o se aplican
     * TODOS los cambios (venta + detalles + descuento de stock + bitácora de
     * movimientos), o no se aplica ninguno.
     *
     * Room revierte automáticamente todo lo hecho dentro de este método si se
     * lanza cualquier excepción — por eso StockInsuficienteException es segura
     * de lanzar a mitad de la función.
     *
     * Se valida el stock producto por producto DENTRO de la transacción (no antes)
     * para evitar condiciones de carrera si dos cajeros venden el mismo producto
     * casi al mismo tiempo desde dispositivos distintos que comparten la misma
     * base local (poco común, pero cubre el caso de un solo dispositivo con
     * corrutinas concurrentes).
     */
    @Transaction
    open suspend fun registrarVentaCompleta(
        venta: VentaEntity,
        detalles: List<DetalleVentaEntity>
    ) {
        val ahora = System.currentTimeMillis()
        val movimientos = mutableListOf<MovimientoInventarioEntity>()

        for (detalle in detalles) {
            val stockAntes = obtenerStockActual(detalle.productoId)
                ?: throw IllegalStateException("Producto ${detalle.productoId} no existe")

            val filasAfectadas = descontarStockSiHayDisponible(
                productoId = detalle.productoId,
                cantidad = detalle.cantidad,
                ahora = ahora
            )

            if (filasAfectadas == 0) {
                // No había stock suficiente -> abortar toda la transacción
                throw StockInsuficienteException(
                    productoId = detalle.productoId,
                    nombreProducto = detalle.nombreProducto,
                    stockDisponible = stockAntes,
                    cantidadSolicitada = detalle.cantidad
                )
            }

            movimientos.add(
                MovimientoInventarioEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    productoId = detalle.productoId,
                    tipo = TipoMovimiento.VENTA,
                    cantidad = detalle.cantidad,
                    stockAnterior = stockAntes,
                    stockNuevo = stockAntes - detalle.cantidad,
                    referenciaId = venta.id,
                    fecha = ahora
                )
            )
        }

        insertarVenta(venta)
        insertarDetalles(detalles)
        insertarMovimientos(movimientos)
    }

    /**
     * Anula una venta ya registrada: revierte el stock y marca la venta como ANULADA.
     * También atómica, y también deja rastro en la bitácora de movimientos (DEVOLUCION).
     */
    @Transaction
    open suspend fun anularVenta(ventaId: String, motivo: String) {
        val detalles = obtenerDetallesDeVenta(ventaId)
        val ahora = System.currentTimeMillis()
        val movimientos = mutableListOf<MovimientoInventarioEntity>()

        for (detalle in detalles) {
            val stockAntes = obtenerStockActual(detalle.productoId) ?: 0
            incrementarStockInterno(detalle.productoId, detalle.cantidad, ahora)
            movimientos.add(
                MovimientoInventarioEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    productoId = detalle.productoId,
                    tipo = TipoMovimiento.DEVOLUCION,
                    cantidad = detalle.cantidad,
                    stockAnterior = stockAntes,
                    stockNuevo = stockAntes + detalle.cantidad,
                    referenciaId = ventaId,
                    motivo = motivo,
                    fecha = ahora
                )
            )
        }

        marcarVentaAnulada(ventaId, motivo, ahora)
        insertarMovimientos(movimientos)
    }

    @Query(
        """
        UPDATE productos 
        SET stockActual = stockActual + :cantidad, sincronizado = 0, actualizadoEn = :ahora
        WHERE id = :productoId
        """
    )
    protected abstract suspend fun incrementarStockInterno(productoId: String, cantidad: Int, ahora: Long)

    @Query(
        """
        UPDATE ventas SET estado = 'ANULADA', notaAnulacion = :motivo, sincronizado = 0, fecha = :ahora
        WHERE id = :ventaId
        """
    )
    protected abstract suspend fun marcarVentaAnulada(ventaId: String, motivo: String, ahora: Long)

    @Query("SELECT * FROM detalle_venta WHERE ventaId = :ventaId")
    abstract suspend fun obtenerDetallesDeVenta(ventaId: String): List<DetalleVentaEntity>

    @Query("SELECT * FROM ventas WHERE id = :id LIMIT 1")
    abstract suspend fun obtenerPorId(id: String): VentaEntity?

    @Query("SELECT * FROM ventas WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC")
    abstract fun observarVentasEnRango(desde: Long, hasta: Long): Flow<List<VentaEntity>>

    @Query(
        """
        SELECT * FROM ventas 
        WHERE cajaId = :cajaId AND estado = 'COMPLETADA' AND fecha BETWEEN :desde AND :hasta 
        ORDER BY fecha ASC
        """
    )
    abstract suspend fun obtenerVentasParaCorte(cajaId: String, desde: Long, hasta: Long): List<VentaEntity>

    @Query("SELECT * FROM ventas WHERE sincronizado = 0")
    abstract suspend fun obtenerPendientesDeSincronizar(): List<VentaEntity>

    @Query("UPDATE ventas SET sincronizado = 1 WHERE id IN (:ids)")
    abstract suspend fun marcarSincronizados(ids: List<String>)
}
