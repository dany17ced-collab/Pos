package com.tuempresa.possystem.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosBottomNav
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

/**
 * Inicio del rol VENDEDOR — calcado del mockup aprobado (1-inicio-vendedor):
 * saludo + chip de tienda activa, badge azul "MODO VENDEDOR", tarjeta rosa
 * grande "Nueva venta", dos stats (vendido hoy en verde / ventas hoy en
 * ámbar), y categorías en fila deslizable con degradados de color.
 */
@Composable
fun PantallaInicioVendedor(
    app: POSApplication,
    onNuevaVenta: () -> Unit,
    onCategoria: (String) -> Unit,
    onNavegarDestino: (EcoPosDestino) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = fabricaViewModel(app) { HomeViewModel(app) })
    val stats by viewModel.statsVendedor.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val usuario by app.sessionManager.usuarioActual.collectAsState()
    val tienda by app.sessionManager.tiendaActiva.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarStatsVendedor() }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        bottomBar = {
            EcoPosBottomNav(seleccionado = EcoPosDestino.VENDER, esAdmin = false, onSeleccionar = onNavegarDestino)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hola, ${usuario?.nombre ?: ""} 👋", color = EcoPosColors.TextoGrisApagado, fontSize = 13.sp)
                    Text("Vender", color = EcoPosColors.TextoBlanco, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                ChipTienda(nombre = tienda?.nombre ?: "")
            }

            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(EcoPosShapes.TarjetaChica)
                    .background(EcoPosColors.AzulCeleste.copy(alpha = 0.14f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "MODO VENDEDOR — solo cobro y stock",
                    color = EcoPosColors.AzulCeleste,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(EcoPosShapes.Tarjeta)
                    .background(Brush.linearGradient(listOf(EcoPosColors.RojoSalmon, EcoPosColors.RojoSalmon)))
                    .clickable(onClick = onNuevaVenta)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🧾", fontSize = 34.sp)
                Text("Nueva venta", color = EcoPosColors.TextoBlanco, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
                Text("Escanea, busca o toca un producto", color = EcoPosColors.TextoBlanco.copy(alpha = 0.85f), fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaStat(
                    valor = "S/ ${"%.0f".format(stats.ventasHoy)}",
                    etiqueta = "Vendido hoy",
                    colorValor = EcoPosColors.VerdeMenta,
                    modifier = Modifier.weight(1f)
                )
                TarjetaStat(
                    valor = "${stats.numeroVentasHoy}",
                    etiqueta = "Ventas hoy",
                    colorValor = EcoPosColors.AcentoAmbar,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "Ir a categoría",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
            )

            if (categorias.isEmpty()) {
                Text(
                    "Aún no hay categorías creadas.",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    items(categorias, key = { it.id }) { categoria ->
                        val indice = categorias.indexOf(categoria)
                        TarjetaCategoriaShelf(categoria = categoria, indice = indice, onClick = { onCategoria(categoria.id) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
internal fun ChipTienda(nombre: String) {
    Row(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .background(EcoPosColors.FondoTarjeta)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(EcoPosColors.VerdeMentaOscuro))
        Text(nombre, color = EcoPosColors.TextoBlanco, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
internal fun TarjetaStat(
    valor: String,
    etiqueta: String,
    colorValor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(EcoPosShapes.Tarjeta)
            .background(EcoPosColors.FondoTarjeta)
            .padding(14.dp)
    ) {
        Text(valor, color = colorValor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(etiqueta, color = EcoPosColors.TextoGrisApagado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Emoji por defecto según el nombre de la categoría, para acercarse al mockup sin depender de un ícono asignado en BD. */
private fun emojiParaCategoria(nombre: String): String = when {
    nombre.contains("polo", true) -> "👕"
    nombre.contains("pantal", true) || nombre.contains("jean", true) -> "👖"
    nombre.contains("casaca", true) || nombre.contains("chaqueta", true) -> "🧥"
    nombre.contains("vestido", true) -> "👗"
    nombre.contains("calzado", true) || nombre.contains("zapat", true) -> "👟"
    nombre.contains("accesorio", true) -> "🧦"
    else -> "🏷️"
}

private val degradadosCategoria = listOf(
    listOf(EcoPosColors.RojoSalmon, EcoPosColors.RojoSalmon),
    listOf(EcoPosColors.AzulCeleste, EcoPosColors.AzulCeleste),
    listOf(EcoPosColors.AcentoAmbar, EcoPosColors.AcentoAmbar),
    listOf(EcoPosColors.VerdeMenta, EcoPosColors.VerdeMentaOscuro)
)
private val textoOscuroCategoria = setOf(2, 3) // ámbar y verde necesitan texto oscuro para contraste, como en el mockup

@Composable
internal fun TarjetaCategoriaShelf(categoria: CategoriaEntity, indice: Int, onClick: () -> Unit) {
    val paleta = degradadosCategoria[indice % degradadosCategoria.size]
    val colorTexto = if (indice % degradadosCategoria.size in textoOscuroCategoria) Color(0xFF21201C) else EcoPosColors.TextoBlanco
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(EcoPosShapes.Tarjeta)
            .background(Brush.verticalGradient(paleta))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Text(emojiParaCategoria(categoria.nombre), fontSize = 24.sp)
        Text(
            categoria.nombre,
            color = colorTexto,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
