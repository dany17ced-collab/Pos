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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.domain.OrdenCarro
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors

/**
 * Pantalla "Ajustes" general estilo Eco POS. A diferencia de Ajustes de boleta
 * (datos que se imprimen) y Ajustes de impresora (conexión Bluetooth), esta
 * cubre preferencias de comportamiento de la app: divisa, decimales, orden
 * del carro y reglas de anulación. Se guardan en [PreferenciasRepository]
 * (SharedPreferences), no en Room, porque son ajustes de dispositivo, no
 * datos de negocio que deban sincronizarse.
 */
@Composable
fun PantallaAjustesGenerales(app: POSApplication, onVolver: () -> Unit) {
    val preferencias by app.preferenciasRepository.preferencias.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoPosColors.FondoNegro)
            .verticalScroll(rememberScrollState())
    ) {
        CabeceraSimple(titulo = "Ajustes", onVolver = onVolver)

        SeccionTitulo("GENERAL")
        FilaAjusteValor(
            icono = Icons.Filled.AttachMoney,
            titulo = "Divisa",
            valor = preferencias.divisa
        )
        FilaAjusteToggle(
            icono = Icons.Filled.Tag,
            titulo = "Mostrar puntos decimales",
            activo = preferencias.mostrarDecimales,
            onCambiar = { app.preferenciasRepository.actualizarMostrarDecimales(it) }
        )
        FilaAjusteValor(
            icono = Icons.Filled.Palette,
            titulo = "Tema",
            valor = "Oscuro"
        )

        SeccionTitulo("CARRO")
        FilaAjusteValor(
            icono = Icons.Filled.RestartAlt,
            titulo = "Ordenar",
            valor = if (preferencias.ordenCarro == OrdenCarro.NUEVO_ARRIBA) {
                "Nuevo producto en la parte superior"
            } else {
                "Nuevo producto en la parte inferior"
            },
            onClick = {
                val nuevoOrden = if (preferencias.ordenCarro == OrdenCarro.NUEVO_ARRIBA) {
                    OrdenCarro.NUEVO_ABAJO
                } else {
                    OrdenCarro.NUEVO_ARRIBA
                }
                app.preferenciasRepository.actualizarOrdenCarro(nuevoOrden)
            }
        )
        FilaAjusteToggle(
            icono = Icons.Filled.QrCodeScanner,
            titulo = "Escaneo de sonido",
            activo = preferencias.sonidoEscaneo,
            onCambiar = { app.preferenciasRepository.actualizarSonidoEscaneo(it) }
        )

        SeccionTitulo("TRANSACCIÓN")
        FilaAjusteValor(icono = Icons.Filled.Receipt, titulo = "Recibo", valor = "")
        FilaAjusteValor(icono = Icons.Filled.Discount, titulo = "Cálculo de tarifas", valor = "")
        FilaAjusteValor(icono = Icons.Filled.Numbers, titulo = "Número de factura", valor = "")
        FilaAjusteToggle(
            icono = Icons.Filled.VisibilityOff,
            titulo = "Ocultar transacción anulada",
            activo = preferencias.ocultarTransaccionAnulada,
            onCambiar = { app.preferenciasRepository.actualizarOcultarTransaccionAnulada(it) }
        )
        FilaAjusteToggle(
            icono = Icons.Filled.RestartAlt,
            titulo = "Anulada repondrá stock",
            activo = preferencias.anuladaRepondraStock,
            onCambiar = { app.preferenciasRepository.actualizarAnuladaRepondraStock(it) }
        )
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(
        texto,
        color = EcoPosColors.TextoBlanco,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun FilaAjusteValor(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = EcoPosColors.TextoBlanco, modifier = Modifier.size(20.dp))
            Text(titulo, color = EcoPosColors.TextoBlanco, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))
        }
        if (valor.isNotBlank()) {
            Text(valor, color = EcoPosColors.TextoGris, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FilaAjusteToggle(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    activo: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = EcoPosColors.TextoBlanco, modifier = Modifier.size(20.dp))
            Text(titulo, color = EcoPosColors.TextoBlanco, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))
        }
        Switch(
            checked = activo,
            onCheckedChange = onCambiar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EcoPosColors.TextoBlanco,
                checkedTrackColor = EcoPosColors.LilaAzulado
            )
        )
    }
}
