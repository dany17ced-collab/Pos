package com.tuempresa.possystem.domain

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreferenciasGenerales(
    val divisa: String = "PEN",
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
        ocultarTransaccionAnulada = prefs.getBoolean(CLAVE_OCULTAR_ANULADA, true),
        anuladaRepondraStock = prefs.getBoolean(CLAVE_ANULADA_REPONE_STOCK, true)
    )

    fun actualizarDivisa(valor: String) {
        prefs.edit().putString(CLAVE_DIVISA, valor).apply()
        _preferencias.value = _preferencias.value.copy(divisa = valor)
    }

    fun actualizarOcultarTransaccionAnulada(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_OCULTAR_ANULADA, valor).apply()
        _preferencias.value = _preferencias.value.copy(ocultarTransaccionAnulada = valor)
    }

    fun actualizarAnuladaRepondraStock(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_ANULADA_REPONE_STOCK, valor).apply()
        _preferencias.value = _preferencias.value.copy(anuladaRepondraStock = valor)
    }

    /** Vuelve las preferencias de esta terminal a sus valores de fábrica.
     * No afecta productos, ventas, usuarios ni ninguna otra tabla del negocio. */
    fun restaurarValoresPorDefecto() {
        prefs.edit().clear().apply()
        _preferencias.value = PreferenciasGenerales()
    }

    companion object {
        private const val CLAVE_DIVISA = "divisa"
        private const val CLAVE_OCULTAR_ANULADA = "ocultar_anulada"
        private const val CLAVE_ANULADA_REPONE_STOCK = "anulada_repone_stock"
    }
}
