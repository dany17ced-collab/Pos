package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

/**
 * Ajustes generales — preferencias de esta terminal (no se sincronizan,
 * viven en SharedPreferences vía [com.tuempresa.possystem.domain.PreferenciasRepository]).
 *
 * Se muestran solo las opciones que realmente cambian el comportamiento de
 * la app: "Ocultar anuladas del historial" filtra la lista de Mis ventas, y
 * "Devolver stock al anular" controla si anular una venta repone inventario
 * (ver MisVentasViewModel.anularVenta / VentaDao.anularVenta). Se quitaron
 * "Mostrar decimales", "Orden de productos nuevos" y "Sonido al escaneo": no
 * tenían ningún efecto real, eran controles muertos que confundían.
 */
@Composable
fun PantallaAjustesGenerales(app: POSApplication, onVolver: () -> Unit) {
    val preferencias by app.preferenciasRepository.preferencias.collectAsState()
    var mostrandoConfirmacionRestaurar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoPosColors.FondoNegro)
            .verticalScroll(rememberScrollState())
    ) {
        CabeceraSimple(
            titulo = "Ajustes generales",
            onVolver = onVolver
        )
        Text(
            "Preferencias de esta terminal: no se comparten con otras tiendas",
            color = EcoPosColors.TextoGrisApagado,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        SeccionAjustes(titulo = "Moneda") {
            FilaAjusteValor(
                icono = Icons.Filled.AttachMoney,
                titulo = "Divisa",
                descripcion = "Símbolo usado en toda la app",
                valor = preferencias.divisa
            )
        }

        SeccionAjustes(titulo = "Ventas anuladas") {
            FilaAjusteToggle(
                icono = Icons.Filled.VisibilityOff,
                titulo = "Ocultar del historial de ventas",
                descripcion = "No mostrarlas en Mis ventas del día",
                activo = preferencias.ocultarTransaccionAnulada,
                onCambiar = { app.preferenciasRepository.actualizarOcultarTransaccionAnulada(it) }
            )
            FilaAjusteToggle(
                icono = Icons.Filled.RestartAlt,
                titulo = "Devolver stock al anular",
                descripcion = "Repone automáticamente el inventario vendido",
                activo = preferencias.anuladaRepondraStock,
                onCambiar = { app.preferenciasRepository.actualizarAnuladaRepondraStock(it) }
            )
        }

        OutlinedButton(
            onClick = { mostrandoConfirmacionRestaurar = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EcoPosColors.TextoGrisApagado),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth()
        ) {
            Text("Restaurar valores por defecto")
        }
    }

    if (mostrandoConfirmacionRestaurar) {
        AlertDialog(
            onDismissRequest = { mostrandoConfirmacionRestaurar = false },
            title = { Text("¿Restaurar valores por defecto?") },
            text = {
                Text(
                    "Vuelve la divisa y las preferencias de ventas anuladas de esta terminal a su " +
                        "configuración original. No afecta productos, ventas ni usuarios."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    app.preferenciasRepository.restaurarValoresPorDefecto()
                    mostrandoConfirmacionRestaurar = false
                }) {
                    Text("Restaurar", color = EcoPosColors.RojoSalmon)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoConfirmacionRestaurar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SeccionAjustes(titulo: String, contenido: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Text(
        titulo,
        color = EcoPosColors.TextoGrisApagado,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(EcoPosShapes.Tarjeta)
            .background(EcoPosColors.FondoTarjeta),
        content = contenido
    )
}

@Composable
private fun FilaAjusteValor(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    valor: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoAjuste(icono)
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(titulo, color = EcoPosColors.TextoBlanco, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text(descripcion, color = EcoPosColors.TextoGrisApagado, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            valor,
            color = if (onClick != null) EcoPosColors.AcentoAmbar else EcoPosColors.TextoGris,
            fontSize = 13.sp,
            fontWeight = if (onClick != null) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FilaAjusteToggle(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoAjuste(icono)
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(titulo, color = EcoPosColors.TextoBlanco, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text(descripcion, color = EcoPosColors.TextoGrisApagado, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = activo,
            onCheckedChange = onCambiar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EcoPosColors.TextoBlanco,
                checkedTrackColor = EcoPosColors.AcentoAmbar
            )
        )
    }
}

/** Mismo tratamiento visual (círculo con fondo suave) que usan los íconos de las demás pantallas de ajustes. */
@Composable
private fun IconoAjuste(icono: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(EcoPosColors.AcentoAmbar.copy(alpha = 0.14f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = EcoPosColors.AcentoAmbar, modifier = Modifier.size(18.dp))
    }
}
