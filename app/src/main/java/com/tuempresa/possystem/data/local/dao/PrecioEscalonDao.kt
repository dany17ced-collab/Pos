package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.possystem.data.local.entity.PrecioEscalonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrecioEscalonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(escalon: PrecioEscalonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(escalones: List<PrecioEscalonEntity>)

    @Update
    suspend fun actualizar(escalon: PrecioEscalonEntity)

    @Query("UPDATE precios_escalon SET eliminado = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarEliminado(id: String, ahora: Long = System.currentTimeMillis())

    /** Todos los escalones activos de un producto/variante, ordenados de menor a mayor cantidad. */
    @Query(
        """
        SELECT * FROM precios_escalon 
        WHERE productoId = :productoId AND eliminado = 0 
        ORDER BY cantidadMinima ASC
        """
    )
    fun observarEscalonesDeProducto(productoId: String): Flow<List<PrecioEscalonEntity>>

    @Query(
        """
        SELECT * FROM precios_escalon 
        WHERE productoId = :productoId AND eliminado = 0 
        ORDER BY cantidadMinima ASC
        """
    )
    suspend fun obtenerEscalonesDeProducto(productoId: String): List<PrecioEscalonEntity>

    /**
     * Devuelve el precio unitario correcto para una cantidad dada de una variante
     * específica: el escalón con la cantidadMinima más alta que no exceda la
     * cantidad solicitada. Ejemplo: escalones en 1, 3, 6, 12 y se piden 8 unidades
     * -> aplica el escalón de 6 (el de mayor cantidadMinima que sigue siendo <= 8).
     *
     * Si el producto no tiene escalones configurados, el llamador debe usar como
     * respaldo el precioVenta de ProductoEntity (ver PrecioCalculator).
     */
    @Query(
        """
        SELECT * FROM precios_escalon 
        WHERE productoId = :productoId AND eliminado = 0 AND cantidadMinima <= :cantidad
        ORDER BY cantidadMinima DESC
        LIMIT 1
        """
    )
    suspend fun obtenerEscalonParaCantidad(productoId: String, cantidad: Int): PrecioEscalonEntity?

    @Query("SELECT * FROM precios_escalon WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSincronizar(): List<PrecioEscalonEntity>

    @Query("UPDATE precios_escalon SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)
}
