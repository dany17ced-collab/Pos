package com.tuempresa.possystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.tuempresa.possystem.presentation.navegacion.GrafoNavegacionPrincipal

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as POSApplication

        setContent {
            // El POS siempre usa esquema oscuro cálido, independiente del tema
            // del sistema: es una pantalla de mostrador, no una app de consumo
            // que deba adaptarse al modo claro/oscuro del teléfono personal.
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                GrafoNavegacionPrincipal(app = app)
            }
        }
    }
}
