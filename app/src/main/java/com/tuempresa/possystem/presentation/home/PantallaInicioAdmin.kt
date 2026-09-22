package com.tuempresa.possystem.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosBottomNav
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

/**
 * Inicio del rol ADMINISTRADOR — calcado del mockup aprobado (2-inicio-admin):
 * saludo + chip "Todas", badge morado "MODO ADMINISTRADOR", tarjetas de
 * tienda deslizables (la activa resaltada en morado) con botón "+ Agregar
 * tienda", grid 2x2 de stats (verde/azul/ámbar/rojo), banner de alerta de
 * stock bajo, y accesos de gestión Vendedores/Reportes.
 */
@Composable
fun PantallaInicioAdmin(
    app: POSApplication,
    onSeleccionarTienda: (String) -> Unit,
    onAgregarTienda: () -> Unit,
    onVendedores: () -> Unit,
    onReportes: () -> Unit,
    onNavegarDestino: (EcoPosDestino) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = fabricaViewModel(app) { HomeViewModel(app) })
    val stats by viewModel.statsAdmin.collectAsState()
    val tiendasResumen by viewModel.tiendasResumen.collectAsState()
    val usuario by app.sessionManager.usuarioActual.collectAsState()
    val tiendaActiva by app.sessionManager.tiendaActiva.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarStatsAdmin() }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        bottomBar = {
            EcoPosBottomNav(seleccionado = EcoPosDestino.VENDER, esAdmin = true, onSeleccionar = onNavegarDestino)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hola, ${usuario?.nombre ?: ""} 👋", color = EcoPosColors.TextoGrisApagado, fontSize = 13.sp)
                    Text("Resumen", color = EcoPosColors.TextoBlanco, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                ChipTienda(nombre = "Todas")
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(EcoPosShapes.TarjetaChica)
                    .background(EcoPosColors.MoradoVivo.copy(alpha = 0.14f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "MODO ADMINISTRADOR — todas las tiendas",
                    color = EcoPosColors.MoradoVivo,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Tus tiendas",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(tiendasResumen, key = { it.tienda.id }) { resumen ->
                    TarjetaTiendaHome(
                        nombre = resumen.tienda.nombre,
                        vendedoresActivos = resumen.vendedoresActivos,
                        ventasHoy = resumen.ventasHoy,
                        esActiva = resumen.tienda.id == tiendaActiva?.id,
                        onClick = { onSeleccionarTienda(resumen.tienda.id) }
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(EcoPosShapes.Tarjeta)
                            .background(EcoPosColors.FondoTarjeta)
                            .border(1.5.dp, EcoPosColors.FondoBorde, EcoPosShapes.Tarjeta)
                            .clickable(onClick = onAgregarTienda)
                            .padding(12.dp)
                    ) {
                        Text("+ Agregar tienda", color = EcoPosColors.TextoBlanco, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Nueva sucursal", color = EcoPosColors.TextoGrisApagado, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaStat(
                    valor = "S/ ${"%.0f".format(stats.ventasHoy)}",
                    etiqueta = "Ventas hoy (todas)",
                    colorValor = EcoPosColors.VerdeVivo,
                    modifier = Modifier.weight(1f)
                )
                TarjetaStat(
                    valor = "${stats.transaccionesHoy}",
                    etiqueta = "Transacciones",
                    colorValor = EcoPosColors.AzulVivo,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaStat(
                    valor = "${stats.productosStockBajo}",
                    etiqueta = "Stock bajo",
                    colorValor = EcoPosColors.AmbarVivo,
                    modifier = Modifier.weight(1f)
                )
                TarjetaStat(
                    valor = "${stats.vendedoresActivosAhora}",
                    etiqueta = "Vendedores activos ahora",
                    colorValor = EcoPosColors.RojoVivo,
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.productosStockBajo > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp)
                        .clip(EcoPosShapes.TarjetaChica)
                        .background(EcoPosColors.AmbarVivoOscuro.copy(alpha = 0.14f))
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚠️ ${stats.productosStockBajo} productos con stock bajo entre tus ${tiendasResumen.size} tiendas",
                        color = EcoPosColors.AmbarVivo,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Gestión",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BotonGestion(
                    texto = "Vendedores",
                    emoji = "👥",
                    colores = listOf(EcoPosColors.AzulVivo, EcoPosColors.AzulVivoOscuro),
                    colorTexto = EcoPosColors.TextoBlanco,
                    onClick = onVendedores,
                    modifier = Modifier.weight(1f)
                )
                BotonGestion(
                    texto = "Reportes",
                    emoji = "📊",
                    colores = listOf(EcoPosColors.AmbarVivo, EcoPosColors.AmbarVivoOscuro),
                    colorTexto = Color(0xFF21201C),
                    onClick = onReportes,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TarjetaTiendaHome(
    nombre: String,
    vendedoresActivos: Int,
    ventasHoy: Double,
    esActiva: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(EcoPosShapes.Tarjeta)
            .background(if (esActiva) EcoPosColors.MoradoVivo.copy(alpha = 0.08f) else EcoPosColors.FondoTarjeta)
            .border(1.5.dp, if (esActiva) EcoPosColors.MoradoVivo else EcoPosColors.FondoBorde, EcoPosShapes.Tarjeta)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(nombre, color = EcoPosColors.TextoBlanco, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "$vendedoresActivos vendedores activos",
            color = EcoPosColors.TextoGrisApagado,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
        Text(
            "S/ ${"%.0f".format(ventasHoy)} hoy",
            color = EcoPosColors.VerdeVivo,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun BotonGestion(
    texto: String,
    emoji: String,
    colores: List<Color>,
    colorTexto: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(EcoPosShapes.TarjetaChica)
            .background(Brush.linearGradient(colores))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(texto, color = colorTexto, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
    }
}
