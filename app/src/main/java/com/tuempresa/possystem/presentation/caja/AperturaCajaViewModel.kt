package com.tuempresa.possystem.presentation.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.TipoCorte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Identifica el dispositivo/caja actual — igual que en VentaViewModel y ReportesViewModel. */
private const val CAJA_ID = "caja-principal"

sealed class EstadoAperturaCaja {
    /** Aún no se sabe si hay fondo declarado — evita parpadear el diálogo mientras carga. */
    object Verificando : EstadoAperturaCaja()
    /** Hay un fondo inicial vigente para el turno actual: se puede operar con normalidad. */
    object Lista : EstadoAperturaCaja()
    /** No hay fondo declarado para el turno vigente: hay que pedirlo antes de vender. */
    object RequiereApertura : EstadoAperturaCaja()
}

sealed class EstadoGuardadoApertura {
    object Inactivo : EstadoGuardadoApertura()
    object Guardando : EstadoGuardadoApertura()
    data class Error(val mensaje: String) : EstadoGuardadoApertura()
}

/**
 * Verifica si el turno vigente ya tiene un fondo inicial declarado y, si no,
 * permite declararlo. Usa el mismo mecanismo de CorteCajaDao/TipoCorte.APERTURA
 * que ReportesViewModel, así que un fondo declarado desde Vender (vendedor) o
 * desde Reportes (admin) es el mismo turno para toda la caja.
 *
 * Esto habilita que CUALQUIER vendedor, al entrar a Vender por primera vez en
 * el turno (es decir, mientras no haya un fondo inicial vigente desde el
 * último Corte Z), deba contar y declarar el dinero con el que arranca la
 * caja antes de poder cobrar.
 */
class AperturaCajaViewModel(private val app: POSApplication) : ViewModel() {

    private val corteCajaDao = app.database.corteCajaDao()
    private val sessionManager = app.sessionManager

    private val _estado = MutableStateFlow<EstadoAperturaCaja>(EstadoAperturaCaja.Verificando)
    val estado: StateFlow<EstadoAperturaCaja> = _estado.asStateFlow()

    private val _estadoGuardado = MutableStateFlow<EstadoGuardadoApertura>(EstadoGuardadoApertura.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardadoApertura> = _estadoGuardado.asStateFlow()

    init {
        verificar()
    }

    fun verificar() {
        viewModelScope.launch {
            _estado.value = EstadoAperturaCaja.Verificando
            val ultimoCorte = corteCajaDao.obtenerUltimoCorte(CAJA_ID)
            val hayFondoVigente = fondoInicialVigente(ultimoCorte) != null
            _estado.value = if (hayFondoVigente) EstadoAperturaCaja.Lista else EstadoAperturaCaja.RequiereApertura
        }
    }

    private fun fondoInicialVigente(ultimoCorte: CorteCajaEntity?): Double? = when (ultimoCorte?.tipo) {
        TipoCorte.APERTURA, TipoCorte.X -> ultimoCorte.fondoInicial
        TipoCorte.Z, null -> null
    }

    /**
     * Declara el fondo inicial contado por quien esté abriendo el turno (el
     * primer vendedor o admin que entra a vender después de un Corte Z, o la
     * primera vez que se usa la app). Queda registrado con el usuario logueado
     * en este momento, sea vendedor o admin.
     */
    fun declararFondoInicial(fondoInicial: Double) {
        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardadoApertura.Guardando
            try {
                val ahora = System.currentTimeMillis()
                corteCajaDao.insertar(
                    CorteCajaEntity(
                        id = UUID.randomUUID().toString(),
                        cajaId = CAJA_ID,
                        tipo = TipoCorte.APERTURA,
                        fechaInicio = ahora,
                        fechaCorte = ahora,
                        fondoInicial = fondoInicial,
                        efectivoEsperado = fondoInicial,
                        usuarioId = sessionManager.usuarioActual.value?.id
                    )
                )
                _estadoGuardado.value = EstadoGuardadoApertura.Inactivo
                _estado.value = EstadoAperturaCaja.Lista
            } catch (ex: Exception) {
                _estadoGuardado.value = EstadoGuardadoApertura.Error(
                    ex.message ?: "No se pudo registrar el fondo inicial."
                )
            }
        }
    }
}
