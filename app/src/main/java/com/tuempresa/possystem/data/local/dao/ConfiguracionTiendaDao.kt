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

    @Query("SELECT * FROM configuracion_tienda WHERE id = :id LIMIT 1")
    suspend fun obtener(id: String = ConfiguracionTiendaEntity.ID_UNICO): ConfiguracionTiendaEntity?

    @Query("SELECT * FROM configuracion_tienda WHERE id = :id LIMIT 1")
    fun observar(id: String = ConfiguracionTiendaEntity.ID_UNICO): Flow<ConfiguracionTiendaEntity?>
}
