package com.tuempresa.possystem.data.local.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.Query
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracionTiendaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(configuracion: ConfiguracionTiendaEntity)

    @Query("SELECT * FROM configuracion_tienda WHERE tiendaId = :tiendaId LIMIT 1")
    suspend fun obtenerPorTienda(tiendaId: String): ConfiguracionTiendaEntity?

    @Query("SELECT * FROM configuracion_tienda WHERE tiendaId = :tiendaId LIMIT 1")
    fun observarPorTienda(tiendaId: String): Flow<ConfiguracionTiendaEntity?>
}
