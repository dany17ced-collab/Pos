package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Fila agregada para el reporte de "productos más vendidos". */
data class ProductoMasVendido(
    val productoId: String,
    val nombreProducto: String,
    val unidadesVendidas: Int,
    val totalVendido: Double,
    val margenTotal: Double
)

@Dao
interface DetalleVentaDao {

    /**
     * Productos más vendidos en un rango de fechas, ordenados por unidades.
     * El margen se calcula con el precioCompraUnitario "congelado" al momento
     * de la venta, así el histórico no se distorsiona si el costo cambió después.
     */
    @Query(
        """
        SELECT 
            dv.productoId as productoId,
            dv.nombreProducto as nombreProducto,
            SUM(dv.cantidad) as unidadesVendidas,
            SUM(dv.subtotal) as totalVendido,
            SUM((dv.precioUnitario - dv.precioCompraUnitario) * dv.cantidad) as margenTotal
        FROM detalle_venta dv
        INNER JOIN ventas v ON v.id = dv.ventaId
        WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta
        GROUP BY dv.productoId, dv.nombreProducto
        ORDER BY unidadesVendidas DESC
        LIMIT :limite
        """
    )
    fun observarProductosMasVendidos(desde: Long, hasta: Long, limite: Int = 20): Flow<List<ProductoMasVendido>>

    @Query(
        """
        SELECT COALESCE(SUM((dv.precioUnitario - dv.precioCompraUnitario) * dv.cantidad), 0.0)
        FROM detalle_venta dv
        INNER JOIN ventas v ON v.id = dv.ventaId
        WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta
        """
    )
    suspend fun calcularMargenTotal(desde: Long, hasta: Long): Double
}
