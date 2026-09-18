package com.tuempresa.possystem.domain

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Cómo ordenar los productos recién agregados dentro del carrito de venta. */
enum class OrdenCarro { NUEVO_ARRIBA, NUEVO_ABAJO }

data class PreferenciasGenerales(
    val divisa: String = "PEN",
    val mostrarDecimales: Boolean = true,
    val ordenCarro: OrdenCarro = OrdenCarro.NUEVO_ARRIBA,
    val sonidoEscaneo: Boolean = true,
    val ocultarTransaccionAnulada: Boolean = true,
    val anuladaRepondraStock: Boolean = true
)

/**
 * Persistencia liviana (SharedPreferences) para los ajustes generales estilo
 * Eco POS que no ameritan su propia tabla Room: son un puñado de flags/strings
 * globales, no datos transaccionales que deban sincronizarse con Supabase.
 */
class PreferenciasRepository(context: Context) {

    private val prefs = context.getSharedPreferences("preferencias_generales", Context.MODE_PRIVATE)

    private val _preferencias = MutableStateFlow(cargar())
    val preferencias: StateFlow<PreferenciasGenerales> = _preferencias.asStateFlow()

    private fun cargar(): PreferenciasGenerales = PreferenciasGenerales(
        divisa = prefs.getString(CLAVE_DIVISA, "PEN") ?: "PEN",
        mostrarDecimales = prefs.getBoolean(CLAVE_DECIMALES, true),
        ordenCarro = if (prefs.getBoolean(CLAVE_ORDEN_CARRO_ARRIBA, true)) {
            OrdenCarro.NUEVO_ARRIBA
        } else {
            OrdenCarro.NUEVO_ABAJO
        },
        sonidoEscaneo = prefs.getBoolean(CLAVE_SONIDO_ESCANEO, true),
        ocultarTransaccionAnulada = prefs.getBoolean(CLAVE_OCULTAR_ANULADA, true),
        anuladaRepondraStock = prefs.getBoolean(CLAVE_ANULADA_REPONE_STOCK, true)
    )

    fun actualizarDivisa(valor: String) {
        prefs.edit().putString(CLAVE_DIVISA, valor).apply()
        _preferencias.value = _preferencias.value.copy(divisa = valor)
    }

    fun actualizarMostrarDecimales(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_DECIMALES, valor).apply()
        _preferencias.value = _preferencias.value.copy(mostrarDecimales = valor)
    }

    fun actualizarOrdenCarro(valor: OrdenCarro) {
        prefs.edit().putBoolean(CLAVE_ORDEN_CARRO_ARRIBA, valor == OrdenCarro.NUEVO_ARRIBA).apply()
        _preferencias.value = _preferencias.value.copy(ordenCarro = valor)
    }

    fun actualizarSonidoEscaneo(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_SONIDO_ESCANEO, valor).apply()
        _preferencias.value = _preferencias.value.copy(sonidoEscaneo = valor)
    }

    fun actualizarOcultarTransaccionAnulada(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_OCULTAR_ANULADA, valor).apply()
        _preferencias.value = _preferencias.value.copy(ocultarTransaccionAnulada = valor)
    }

    fun actualizarAnuladaRepondraStock(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_ANULADA_REPONE_STOCK, valor).apply()
        _preferencias.value = _preferencias.value.copy(anuladaRepondraStock = valor)
    }

    companion object {
        private const val CLAVE_DIVISA = "divisa"
        private const val CLAVE_DECIMALES = "mostrar_decimales"
        private const val CLAVE_ORDEN_CARRO_ARRIBA = "orden_carro_arriba"
        private const val CLAVE_SONIDO_ESCANEO = "sonido_escaneo"
        private const val CLAVE_OCULTAR_ANULADA = "ocultar_anulada"
        private const val CLAVE_ANULADA_REPONE_STOCK = "anulada_repone_stock"
    }
}
