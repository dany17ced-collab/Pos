package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import com.tuempresa.possystem.presentation.theme.EcoPosBottomNav
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import kotlinx.coroutines.launch

private data class ItemMenuAjustes(val emoji: String, val texto: String, val onClick: () -> Unit)

/**
 * Pantalla de Ajustes — calcada del mockup aprobado (5-ajustes): tarjeta de
 * perfil con avatar y rol, lista de tiendas con la activa marcada y un
 * "Cambiar →" para las demás, botón punteado "+ Agregar nueva tienda", y el
 * menú General con Vendedores y permisos, Comprobantes y boletas, Importar
 * Excel, Categorías y colores, y Seguridad.
 *
 * El vendedor ve una versión reducida: sin la lista de tiendas (solo opera en
 * la que el admin le asignó) y sin los ítems de gestión de negocio.
 */
@Composable
fun PantallaAjustes(
    app: POSApplication,
    onVerTodasLasTiendas: () -> Unit,
    onVendedoresYPermisos: () -> Unit,
    onComprobantesYBoletas: () -> Unit,
    onImportarExcel: () -> Unit,
    onCategoriasYColores: () -> Unit,
    onImpresora: () -> Unit,
    onCerrarSesion: () -> Unit,
    onClientes: () -> Unit,
    onDescuentos: () -> Unit,
    onCambios: () -> Unit,
    onUnidades: () -> Unit,
    onEtiquetasPago: () -> Unit,
    onAjustesGenerales: () -> Unit,
    onNavegarDestino: (EcoPosDestino) -> Unit
) {
    val usuario by app.sessionManager.usuarioActual.collectAsState()
    val tiendaActiva by app.sessionManager.tiendaActiva.collectAsState()
    val esAdmin = usuario?.rol == RolUsuario.ADMIN
    val coroutineScope = rememberCoroutineScope()

    var tiendas by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<TiendaEntity>>(emptyList()) }

    LaunchedEffect(esAdmin) {
        if (esAdmin) {
            app.tiendaRepository.observarTiendas().collect { tiendas = it }
        }
    }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        bottomBar = {
            EcoPosBottomNav(seleccionado = EcoPosDestino.MENU, esAdmin = esAdmin, onSeleccionar = onNavegarDestino)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 4.dp)) {
                    Text("Ajustes", color = EcoPosColors.TextoBlanco, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Cuenta y tiendas", color = EcoPosColors.TextoGrisApagado, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(EcoPosShapes.Tarjeta)
                        .background(EcoPosColors.FondoTarjeta)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(EcoPosShapes.TarjetaChica)
                            .background(Brush.linearGradient(listOf(EcoPosColors.AzulCeleste, EcoPosColors.AzulCeleste))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (usuario?.nombre?.firstOrNull() ?: '?').uppercase(),
                            color = EcoPosColors.TextoBlanco,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(usuario?.nombre ?: "", color = EcoPosColors.TextoBlanco, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (esAdmin) "ADMINISTRADOR" else "VENDEDOR",
                            color = EcoPosColors.AzulCeleste,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (esAdmin) {
                item {
                    Text(
                        "Tus tiendas",
                        color = EcoPosColors.TextoGrisApagado,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
                    )
                }
                items(tiendas, key = { it.id }) { tienda ->
                    val esActiva = tienda.id == tiendaActiva?.id
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clip(EcoPosShapes.TarjetaChica)
                            .background(EcoPosColors.FondoTarjeta)
                            .border(1.5.dp, if (esActiva) EcoPosColors.VerdeMentaOscuro else EcoPosColors.FondoTarjetaClara, EcoPosShapes.TarjetaChica)
                            .clickable(enabled = !esActiva) {
                                coroutineScope.launch { app.sessionManager.establecerTiendaActiva(tienda) }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (esActiva) EcoPosColors.VerdeMentaOscuro else EcoPosColors.TextoGrisApagado.copy(alpha = 0.5f))
                        )
                        Text(
                            tienda.nombre,
                            color = EcoPosColors.TextoBlanco,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 10.dp).weight(1f)
                        )
                        Text(
                            if (esActiva) "Activa ahora" else "Cambiar →",
                            color = EcoPosColors.TextoGrisApagado,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .clip(EcoPosShapes.TarjetaChica)
                            .background(EcoPosColors.RojoSalmon.copy(alpha = 0.08f))
                            .border(1.5.dp, EcoPosColors.RojoSalmon.copy(alpha = 0.4f), EcoPosShapes.TarjetaChica)
                            .clickable(onClick = onVerTodasLasTiendas)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Agregar nueva tienda", color = EcoPosColors.RojoSalmon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text(
                    "General",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
                )
            }

            val itemsMenu = buildList {
                if (esAdmin) {
                    add(ItemMenuAjustes("👥", "Vendedores y permisos", onVendedoresYPermisos))
                    add(ItemMenuAjustes("🧾", "Comprobantes y boletas", onComprobantesYBoletas))
                    add(ItemMenuAjustes("📥", "Importar Excel", onImportarExcel))
                    add(ItemMenuAjustes("🎨", "Categorías y colores", onCategoriasYColores))
                    add(ItemMenuAjustes("🧑‍🤝‍🧑", "Clientes", onClientes))
                    add(ItemMenuAjustes("🏷️", "Descuentos", onDescuentos))
                    add(ItemMenuAjustes("📏", "Unidades de medida", onUnidades))
                    add(ItemMenuAjustes("💳", "Etiquetas de pago", onEtiquetasPago))
                    add(ItemMenuAjustes("⚙️", "Ajustes generales", onAjustesGenerales))
                }
                // Cambios y devoluciones: visible para admin y vendedor, ambos
                // atienden al cliente en tienda y pueden necesitar registrar uno.
                add(ItemMenuAjustes("🔁", "Cambios y devoluciones", onCambios))
                add(ItemMenuAjustes("🖨️", "Impresora", onImpresora))
                // "Seguridad" se quitó del menú: apuntaba a la misma pantalla
                // que "Vendedores y permisos" (Rutas.USUARIOS), duplicando la opción.
                add(ItemMenuAjustes("🚪", "Cerrar sesión", onCerrarSesion))
            }

            items(itemsMenu) { item ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(EcoPosShapes.TarjetaChica)
                        .background(EcoPosColors.FondoTarjeta)
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.emoji, fontSize = 18.sp, modifier = Modifier.size(24.dp))
                    Text(
                        item.texto,
                        color = EcoPosColors.TextoBlanco,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp).weight(1f)
                    )
                    Text("›", color = EcoPosColors.TextoGrisApagado, fontSize = 16.sp)
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
        }
    }
}
