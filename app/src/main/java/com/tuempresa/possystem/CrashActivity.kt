package com.tuempresa.possystem

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Mecanismo de diagnóstico SOLO PARA DEPURACIÓN: en vez de intentar lanzar una
 * Activity nueva desde el manejador de excepciones no controladas (frágil, puede
 * no funcionar según el estado del proceso), el stack trace del último crash se
 * guarda en SharedPreferences. La pantalla de Login lo lee al arrancar y lo
 * muestra en texto si existe, para poder diagnosticarlo sin herramientas de PC.
 *
 * Quitar este mecanismo una vez resuelto el problema de estabilidad actual.
 */
object CrashHandler {
    private const val PREFS_NAME = "diagnostico_crash"
    private const val CLAVE_ULTIMO_ERROR = "ultimo_error"

    fun instalar(app: POSApplication) {
        val manejadorPrevio = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val texto = sw.toString()

                val prefs = app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                prefs.edit().putString(CLAVE_ULTIMO_ERROR, texto).apply()
            } catch (e: Exception) {
                // si ni esto funciona, seguimos con el comportamiento normal de todos modos
            }
            manejadorPrevio?.uncaughtException(thread, throwable)
        }
    }

    /** Lee el último error guardado, o null si no hay ninguno. */
    fun leerUltimoError(app: POSApplication): String? {
        val prefs = app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getString(CLAVE_ULTIMO_ERROR, null)
    }

    /** Borra el error guardado (se llama después de mostrarlo, para no repetirlo). */
    fun limpiarUltimoError(app: POSApplication) {
        val prefs = app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(CLAVE_ULTIMO_ERROR).apply()
    }
}
