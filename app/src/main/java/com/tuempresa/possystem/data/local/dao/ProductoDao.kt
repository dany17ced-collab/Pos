package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

/** Fila agregada de COUNT(*) agrupado por tiendaId — usada en reportes consolidados. */
data class ConteoPorTienda(val tiendaId: String, val cantidad: Int)

@Dao
interface ProductoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: ProductoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(productos: List<ProductoEntity>)

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    /** Soft-delete: nunca borramos físicamente hasta confirmar sincronización. */
    @Query("UPDATE productos SET eliminado = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarEliminado(id: String, ahora: Long = System.currentTimeMillis())

    /**
     * Actualiza únicamente el precio de venta de un producto/variante — usado
     * en Inventario para ajustar precios por temporada sin tocar el resto de
     * los campos (stock, nombre, etc).
     */
    @Query(
        """
        UPDATE productos 
        SET precioVenta = :nuevoPrecio, sincronizado = 0, actualizadoEn = :ahora
        WHERE id = :productoId
        """
    )
    suspend fun actualizarPrecioVenta(
        productoId: String,
        nuevoPrecio: Double,
        ahora: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM productos WHERE tiendaId = :tiendaId AND eliminado = 0 AND productoBaseId IS NULL ORDER BY nombre ASC")
    fun observarProductosActivos(tiendaId: String): Flow<List<ProductoEntity>>

    /** Igual que observarProductosActivos, pero de un solo disparo — usada para generar reportes. */
    @Query("SELECT * FROM productos WHERE tiendaId = :tiendaId AND eliminado = 0 ORDER BY nombre ASC")
    suspend fun obtenerTodosActivos(tiendaId: String): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE productoBaseId = :productoBaseId AND eliminado = 0")
    fun observarVariantes(productoBaseId: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ProductoEntity?

    /**
     * Busca el producto "padre" por código de barras dentro de una tienda —
     * usada por el escáner en el checkout y en inventario. Como padre e hijas
     * comparten código de barras, se prioriza el registro sin productoBaseId
     * (el padre); si no existe un padre explícito (producto simple sin
     * variantes), devuelve ese único registro. El código de barras solo es
     * único dentro de la misma tienda: dos sucursales pueden imprimir la misma
     * etiqueta en prendas distintas.
     */
    @Query(
        """
        SELECT * FROM productos 
        WHERE tiendaId = :tiendaId AND codigoBarras = :codigoBarras AND eliminado = 0 
        ORDER BY (CASE WHEN productoBaseId IS NULL THEN 0 ELSE 1 END) ASC
        LIMIT 1
        """
    )
    suspend fun buscarPorCodigoBarras(tiendaId: String, codigoBarras: String): ProductoEntity?

    /** Todas las variantes (talla/color) de un producto, buscadas por su código de barras compartido dentro de una tienda. */
    @Query(
        """
        SELECT v.* FROM productos v
        INNER JOIN productos p ON p.id = v.productoBaseId
        WHERE p.tiendaId = :tiendaId AND p.codigoBarras = :codigoBarras AND v.eliminado = 0
        ORDER BY v.talla ASC, v.color ASC
        """
    )
    fun observarVariantesPorCodigoBarras(tiendaId: String, codigoBarras: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE tiendaId = :tiendaId AND sku = :sku AND eliminado = 0 LIMIT 1")
    suspend fun buscarPorSku(tiendaId: String, sku: String): ProductoEntity?

    /** Búsqueda de texto libre para la pantalla de inventario y venta manual, dentro de una tienda. */
    @Query(
        """
        SELECT * FROM productos 
        WHERE tiendaId = :tiendaId AND eliminado = 0 AND (
            nombre LIKE '%' || :query || '%' 
            OR sku LIKE '%' || :query || '%' 
            OR codigoBarras LIKE '%' || :query || '%'
        )
        ORDER BY nombre ASC
        """
    )
    fun buscar(tiendaId: String, query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE tiendaId = :tiendaId AND categoriaId = :categoriaId AND eliminado = 0 ORDER BY nombre ASC")
    fun observarPorCategoria(tiendaId: String, categoriaId: String): Flow<List<ProductoEntity>>

    /** Productos con stock por debajo del mínimo en una tienda — alimenta la alerta de stock bajo. */
    @Query(
        """
        SELECT * FROM productos 
        WHERE tiendaId = :tiendaId AND eliminado = 0 AND activo = 1 AND stockActual <= stockMinimo 
        ORDER BY (stockActual - stockMinimo) ASC
        """
    )
    fun observarStockBajo(tiendaId: String): Flow<List<ProductoEntity>>

    /** Cuenta de productos con stock bajo en TODAS las tiendas, agrupado por tienda — para el dashboard de administrador. */
    @Query(
        """
        SELECT COUNT(*) FROM productos 
        WHERE eliminado = 0 AND activo = 1 AND stockActual <= stockMinimo
        """
    )
    suspend fun contarStockBajoGlobal(): Int

    /** Igual que contarStockBajoGlobal, pero desglosado por tienda — para el reporte consolidado. */
    @Query(
        """
        SELECT tiendaId, COUNT(*) as cantidad FROM productos 
        WHERE eliminado = 0 AND activo = 1 AND stockActual <= stockMinimo
        GROUP BY tiendaId
        """
    )
    suspend fun contarStockBajoPorTienda(): List<ConteoPorTienda>

    /**
     * Descuenta stock de forma atómica cuando se usa dentro de una @Transaction externa
     * (ver VentaDao.registrarVentaCompleta). Retorna las filas afectadas: si es 0,
     * significa que no había stock suficiente y el llamador debe abortar la transacción.
     */
    @Query(
        """
        UPDATE productos 
        SET stockActual = stockActual - :cantidad, sincronizado = 0, actualizadoEn = :ahora
        WHERE id = :productoId AND stockActual >= :cantidad
        """
    )
    suspend fun descontarStockSiHayDisponible(
        productoId: String,
        cantidad: Int,
        ahora: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE productos 
        SET stockActual = stockActual + :cantidad, sincronizado = 0, actualizadoEn = :ahora
        WHERE id = :productoId
        """
    )
    suspend fun incrementarStock(
        productoId: String,
        cantidad: Int,
        ahora: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM productos WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<ProductoEntity>

    @Query("UPDATE productos SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
