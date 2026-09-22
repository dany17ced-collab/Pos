package com.tuempresa.possystem.presentation.inventario

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.tuempresa.possystem.presentation.home.ChipTienda
import com.tuempresa.possystem.presentation.theme.EcoPosBottomNav
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import com.tuempresa.possystem.presentation.theme.ecoPosCamposTextoColores

private val degradadosCategoria = listOf(
    listOf(EcoPosColors.RosaVivoClaro, EcoPosColors.RosaVivo),
    listOf(EcoPosColors.AzulVivo, EcoPosColors.AzulVivoOscuro),
    listOf(EcoPosColors.AmbarVivo, EcoPosColors.AmbarVivoOscuro),
    listOf(EcoPosColors.VerdeVivo, EcoPosColors.VerdeVivoOscuro)
)
private val textoOscuroCategoria = setOf(2, 3)

/**
 * Pantalla de entrada de Inventario — calcada del mockup aprobado
 * (3-inventario): buscador + botón de escaneo, alerta de stock bajo, y grid
 * 2x2 de categorías con conteo y rango de precio. Buscar o tocar una
 * categoría lleva a PantallaInventarioConsulta con ese filtro aplicado.
 */
@Composable
fun PantallaInventarioCategorias(
    app: POSApplication,
    esAdmin: Boolean,
    onBuscar: (String) -> Unit,
    onEscanear: () -> Unit,
    onCategoria: (String) -> Unit,
    onNavegarDestino: (EcoPosDestino) -> Unit
) {
    val viewModel: InventarioResumenViewModel = viewModel(factory = fabricaViewModel(app) { InventarioResumenViewModel(app) })
    val total by viewModel.totalProductos.collectAsState()
    val stockBajo by viewModel.productosStockBajo.collectAsState()
    val resumenes by viewModel.resumenPorCategoria.collectAsState()
    val tienda by app.sessionManager.tiendaActiva.collectAsState()

    var textoBusqueda by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        bottomBar = {
            EcoPosBottomNav(seleccionado = EcoPosDestino.INVENTARIO, esAdmin = esAdmin, onSeleccionar = onNavegarDestino)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inventario", color = EcoPosColors.TextoBlanco, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$total productos", color = EcoPosColors.TextoGrisApagado, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                ChipTienda(nombre = tienda?.nombre ?: "")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textoBusqueda,
                    onValueChange = {
                        textoBusqueda = it
                        onBuscar(it)
                    },
                    placeholder = { Text("Buscar nombre, talla, código...") },
                    singleLine = true,
                    colors = ecoPosCamposTextoColores(),
                    shape = EcoPosShapes.Tarjeta,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(52.dp)
                        .clip(EcoPosShapes.TarjetaChica)
                        .background(EcoPosColors.RosaVivo)
                        .clickable(onClick = onEscanear),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 20.sp)
                }
            }

            if (stockBajo > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                        .clip(EcoPosShapes.TarjetaChica)
                        .background(EcoPosColors.AmbarVivoOscuro.copy(alpha = 0.14f))
                        .padding(vertical = 12.dp, horizontal = 14.dp)
                ) {
                    Text(
                        "⚠️ $stockBajo productos con stock bajo — revisar",
                        color = EcoPosColors.AmbarVivo,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Categorías",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
            )

            if (resumenes.isEmpty()) {
                Text(
                    "Aún no hay productos con categoría asignada.",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(resumenes, key = { it.categoria.id }) { resumen ->
                        val indice = resumenes.indexOf(resumen)
                        TileCategoriaInventario(
                            resumen = resumen,
                            indice = indice,
                            onClick = { onCategoria(resumen.categoria.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCategoriaInventario(resumen: ResumenCategoria, indice: Int, onClick: () -> Unit) {
    val paleta = degradadosCategoria[indice % degradadosCategoria.size]
    val colorTexto = if (indice % degradadosCategoria.size in textoOscuroCategoria) Color(0xFF21201C) else EcoPosColors.TextoBlanco
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(EcoPosShapes.Tarjeta)
            .background(Brush.linearGradient(paleta))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(EcoPosShapes.Chip)
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("${resumen.cantidadProductos}", color = colorTexto, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(emojiParaCategoria(resumen.categoria.nombre), fontSize = 22.sp)
            Text(
                resumen.categoria.nombre,
                color = colorTexto,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "S/ ${"%.0f".format(resumen.precioMinimo)} – S/ ${"%.0f".format(resumen.precioMaximo)}",
                color = colorTexto.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun emojiParaCategoria(nombre: String): String = when {
    nombre.contains("polo", true) -> "👕"
    nombre.contains("pantal", true) || nombre.contains("jean", true) -> "👖"
    nombre.contains("casaca", true) || nombre.contains("chaqueta", true) -> "🧥"
    nombre.contains("vestido", true) -> "👗"
    nombre.contains("calzado", true) || nombre.contains("zapat", true) -> "👟"
    nombre.contains("accesorio", true) -> "🧦"
    else -> "🏷️"
}
