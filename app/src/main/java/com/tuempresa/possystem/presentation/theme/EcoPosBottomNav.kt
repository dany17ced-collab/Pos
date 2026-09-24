package com.tuempresa.possystem.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Los 5 destinos fijos de la barra inferior, igual que en Eco POS.
 *  El destino VENDER se muestra como "Inicio" en la barra: lleva siempre
 *  al resumen (home_admin / home_vendedor), no a la pantalla de venta. */
enum class EcoPosDestino { VENDER, INVENTARIO, HISTORIAL, REPORTES, MENU }

private data class ItemNav(val destino: EcoPosDestino, val emoji: String, val etiqueta: String)

private fun itemsNav(esAdmin: Boolean) = listOf(
    ItemNav(EcoPosDestino.VENDER, "🏠", "Inicio"),
    ItemNav(EcoPosDestino.INVENTARIO, "📦", "Inventario"),
    ItemNav(EcoPosDestino.HISTORIAL, "📄", "Historial"),
    ItemNav(EcoPosDestino.REPORTES, "📊", if (esAdmin) "Reportes" else "Caja"),
    ItemNav(EcoPosDestino.MENU, "⚙️", "Ajustes")
)

@Composable
fun EcoPosBottomNav(
    seleccionado: EcoPosDestino,
    esAdmin: Boolean = true,
    onSeleccionar: (EcoPosDestino) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EcoPosColors.FondoNavBar)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        itemsNav(esAdmin).forEach { item ->
            val activo = item.destino == seleccionado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSeleccionar(item.destino) }
            ) {
                if (activo) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EcoPosColors.VerdeMenta)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(item.emoji, fontSize = 20.sp)
                    }
                } else {
                    Text(
                        item.emoji,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (!activo) {
                    Text(
                        item.etiqueta,
                        color = EcoPosColors.TextoGrisApagado,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
