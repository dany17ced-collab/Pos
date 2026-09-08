package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM productos WHERE eliminado = 0 AND productoBaseId IS NULL ORDER BY nombre ASC")
    fun observarProductosActivos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE productoBaseId = :productoBaseId AND eliminado = 0")
    fun observarVariantes(productoBaseId: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ProductoEntity?

    /** Búsqueda exacta por código de barras — usada por el escáner en el checkout. */
    @Query("SELECT * FROM productos WHERE codigoBarras = :codigoBarras AND eliminado = 0 LIMIT 1")
    suspend fun buscarPorCodigoBarras(codigoBarras: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE sku = :sku AND eliminado = 0 LIMIT 1")
    suspend fun buscarPorSku(sku: String): ProductoEntity?

    /** Búsqueda de texto libre para la pantalla de inventario y venta manual. */
    @Query(
        """
        SELECT * FROM productos 
        WHERE eliminado = 0 AND (
            nombre LIKE '%' || :query || '%' 
            OR sku LIKE '%' || :query || '%' 
            OR codigoBarras LIKE '%' || :query || '%'
        )
        ORDER BY nombre ASC
        """
    )
    fun buscar(query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE categoriaId = :categoriaId AND eliminado = 0 ORDER BY nombre ASC")
    fun observarPorCategoria(categoriaId: String): Flow<List<ProductoEntity>>

    /** Productos con stock por debajo del mínimo — alimenta la alerta de stock bajo. */
    @Query(
        """
        SELECT * FROM productos 
        WHERE eliminado = 0 AND activo = 1 AND stockActual <= stockMinimo 
        ORDER BY (stockActual - stockMinimo) ASC
        """
    )
    fun observarStockBajo(): Flow<List<ProductoEntity>>

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
