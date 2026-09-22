package com.tuempresa.possystem.data.local.repository

import com.tuempresa.possystem.data.local.dao.ConfiguracionTiendaDao
import com.tuempresa.possystem.data.local.dao.TiendaDao
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TiendaRepository(
    private val tiendaDao: TiendaDao,
    private val configuracionTiendaDao: ConfiguracionTiendaDao
) {
    fun observarTiendas(): Flow<List<TiendaEntity>> = tiendaDao.observarTodas()

    suspend fun obtenerTiendasActivas(): List<TiendaEntity> = tiendaDao.obtenerActivas()

    suspend fun obtenerPorId(id: String): TiendaEntity? = tiendaDao.obtenerPorId(id)

    /**
     * Crea una tienda nueva junto con su configuración de boleta vacía
     * (nombre = el mismo de la tienda, para que el ticket no salga en blanco
     * antes de que el admin la termine de configurar en Ajustes).
     */
    suspend fun crearTienda(nombre: String): TiendaEntity {
        val tienda = TiendaEntity(id = UUID.randomUUID().toString(), nombre = nombre)
        tiendaDao.insertar(tienda)
        configuracionTiendaDao.guardar(
            ConfiguracionTiendaEntity(
                id = UUID.randomUUID().toString(),
                tiendaId = tienda.id,
                nombreTienda = nombre
            )
        )
        return tienda
    }

    suspend fun renombrar(tienda: TiendaEntity, nuevoNombre: String) {
        tiendaDao.actualizar(
            tienda.copy(nombre = nuevoNombre, actualizadoEn = System.currentTimeMillis())
        )
    }

    suspend fun desactivar(tienda: TiendaEntity) {
        tiendaDao.actualizar(
            tienda.copy(activa = false, actualizadoEn = System.currentTimeMillis())
        )
    }
}
