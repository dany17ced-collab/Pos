package com.tuempresa.possystem.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FondoCarbon = Color(0xFF221B1D)
private val TextoCrema = Color(0xFFF3E9E1)
private val AcentoTerracota = Color(0xFFD98E73)

/** Se usa para rutas que aún no tienen pantalla real construida. */
@Composable
fun PantallaProximamente(titulo: String, onVolver: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = titulo, color = TextoCrema, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Esta sección se construye en el siguiente paso.",
                color = TextoCrema.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "‹ Volver",
                color = AcentoTerracota,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 32.dp)
                    .clickable(onClick = onVolver)
            )
        }
    }
}
