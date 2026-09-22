package com.tuempresa.possystem.data.local.repository

import com.tuempresa.possystem.data.local.dao.CorteCajaDao
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.TipoCorte
import com.tuempresa.possystem.domain.cajaIdDeTienda
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Snapshot de en qué punto del turno está la caja de una tienda ahora mismo. */
data class EstadoTurnoCaja(
    val fechaInicioTurno: Long,
    val fondoInicialVigente: Double?, // null = turno cerrado (Z) o app recién instalada: hay que abrir turno
)

/**
 * Toda la lógica de apertura/cierre de caja (turnos, corte X, corte Z), en un
 * solo lugar en vez de duplicada entre la pantalla de Vender (que exige abrir
 * turno antes de cobrar) y la de Cierre de Caja (donde se hacen los cortes).
 * Todo queda filtrado por tienda vía [cajaIdDeTienda], para que dos sucursales
 * nunca mezclen sus turnos ni sus totales.
 */
class CajaRepository(private val corteCajaDao: CorteCajaDao) {

    fun observarHistorial(tiendaId: String): Flow<List<CorteCajaEntity>> =
        corteCajaDao.observarHistorial(tiendaId)

    suspend fun obtenerEstadoTurno(tiendaId: String): EstadoTurnoCaja {
        val cajaId = cajaIdDeTienda(tiendaId)
        val ultimoCorte = corteCajaDao.obtenerUltimoCorte(cajaId)
        val desde = when (ultimoCorte?.tipo) {
            TipoCorte.Z -> ultimoCorte.fechaCorte
            TipoCorte.X -> ultimoCorte.fechaInicio
            TipoCorte.APERTURA -> ultimoCorte.fechaCorte
            null -> 0L
        }
        val fondoVigente = when (ultimoCorte?.tipo) {
            TipoCorte.APERTURA, TipoCorte.X -> ultimoCorte.fondoInicial
            TipoCorte.Z, null -> null
        }
        return EstadoTurnoCaja(fechaInicioTurno = desde, fondoInicialVigente = fondoVigente)
    }

    /** Registra el fondo inicial contado al empezar un turno (o la primera vez que se usa la app). */
    suspend fun abrirTurno(tiendaId: String, fondoInicial: Double, usuarioId: String?) {
        val ahora = System.currentTimeMillis()
        corteCajaDao.insertar(
            CorteCajaEntity(
                id = UUID.randomUUID().toString(),
                tiendaId = tiendaId,
                cajaId = cajaIdDeTienda(tiendaId),
                tipo = TipoCorte.APERTURA,
                fechaInicio = ahora,
                fechaCorte = ahora,
                fondoInicial = fondoInicial,
                efectivoEsperado = fondoInicial,
                usuarioId = usuarioId
            )
        )
    }

    /** Corte X: informativo, no cierra el turno. Se puede repetir cuantas veces se quiera en el día. */
    suspend fun realizarCorteX(tiendaId: String, usuarioId: String?): CorteCajaEntity {
        val cajaId = cajaIdDeTienda(tiendaId)
        val estado = obtenerEstadoTurno(tiendaId)
        val ahora = System.currentTimeMillis()
        val resumen = corteCajaDao.calcularResumen(cajaId, estado.fechaInicioTurno, ahora)
        val fondoInicial = estado.fondoInicialVigente ?: 0.0

        val corte = CorteCajaEntity(
            id = UUID.randomUUID().toString(),
            tiendaId = tiendaId,
            cajaId = cajaId,
            tipo = TipoCorte.X,
            fechaInicio = estado.fechaInicioTurno,
            fechaCorte = ahora,
            totalVentas = resumen.totalVentas,
            totalEfectivo = resumen.totalEfectivo,
            totalTarjeta = resumen.totalTarjeta,
            totalTransferencia = resumen.totalTransferencia,
            totalDescuentos = resumen.totalDescuentos,
            totalImpuestos = resumen.totalImpuestos,
            numeroTransacciones = resumen.numeroTransacciones,
            fondoInicial = fondoInicial,
            efectivoEsperado = fondoInicial + resumen.totalEfectivo,
            usuarioId = usuarioId
        )
        corteCajaDao.insertar(corte)
        return corte
    }

    /**
     * Corte Z: cierra el turno. Si se declara fondoInicialSiguienteTurno > 0, ya
     * queda abierto el siguiente turno de una vez (no hay que volver a pedirlo
     * apenas se reabra la caja).
     */
    suspend fun realizarCorteZ(
        tiendaId: String,
        efectivoContado: Double,
        fondoInicialSiguienteTurno: Double,
        usuarioId: String?
    ): CorteCajaEntity {
        val cajaId = cajaIdDeTienda(tiendaId)
        val estado = obtenerEstadoTurno(tiendaId)
        val ahora = System.currentTimeMillis()
        val resumen = corteCajaDao.calcularResumen(cajaId, estado.fechaInicioTurno, ahora)
        val fondoInicial = estado.fondoInicialVigente ?: 0.0
        val efectivoEsperado = fondoInicial + resumen.totalEfectivo

        val corte = CorteCajaEntity(
            id = UUID.randomUUID().toString(),
            tiendaId = tiendaId,
            cajaId = cajaId,
            tipo = TipoCorte.Z,
            fechaInicio = estado.fechaInicioTurno,
            fechaCorte = ahora,
            totalVentas = resumen.totalVentas,
            totalEfectivo = resumen.totalEfectivo,
            totalTarjeta = resumen.totalTarjeta,
            totalTransferencia = resumen.totalTransferencia,
            totalDescuentos = resumen.totalDescuentos,
            totalImpuestos = resumen.totalImpuestos,
            numeroTransacciones = resumen.numeroTransacciones,
            fondoInicial = fondoInicial,
            efectivoEsperado = efectivoEsperado,
            efectivoContado = efectivoContado,
            diferencia = efectivoContado - efectivoEsperado,
            usuarioId = usuarioId
        )
        corteCajaDao.insertar(corte)

        if (fondoInicialSiguienteTurno > 0) {
            abrirTurno(tiendaId, fondoInicialSiguienteTurno, usuarioId)
        }
        return corte
    }
}
