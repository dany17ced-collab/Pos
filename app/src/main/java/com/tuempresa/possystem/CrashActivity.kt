package com.tuempresa.possystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Actividad de emergencia SOLO PARA DIAGNÓSTICO: muestra el stack trace completo
 * de un crash en pantalla en vez de que la app se cierre sin explicación.
 * Se activa desde CrashHandler (ver POSApplication). Quitar una vez resuelto
 * el problema de estabilidad — no es parte del producto final.
 *
 * Hereda de ComponentActivity (no de Activity puro) porque setContent { }
 * de Compose requiere ComponentActivity para funcionar correctamente.
 */
class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mensajeError = intent.getStringExtra("mensaje_error") ?: "Error desconocido (sin mensaje)"

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A0000)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("La app encontró un error:", color = Color.White, fontSize = 16.sp)
                        Text(mensajeError, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

object CrashHandler {
    fun instalar(app: POSApplication) {
        val manejadorPrevio = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val texto = sw.toString()

                val intent = android.content.Intent(app, CrashActivity::class.java).apply {
                    putExtra("mensaje_error", texto)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
            } catch (e: Exception) {
                // si ni esto funciona, seguimos con el comportamiento normal
            }
            manejadorPrevio?.uncaughtException(thread, throwable)
        }
    }
}
