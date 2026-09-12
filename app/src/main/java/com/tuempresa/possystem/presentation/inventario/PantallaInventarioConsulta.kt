package com.tuempresa.possystem.presentation.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val FondoVariante = Color(0xFF3A2F32)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorAlerta = Color(0xFFE08585)

@Composable
fun PantallaInventarioConsulta(
    app: POSApplication,
    onVolver: () -> Unit
) {
    val viewModel: InventarioConsultaViewModel = viewModel(
        factory = fabricaSimple { InventarioConsultaViewModel(app) }
    )

    val productos by viewModel.productos.collectAsState()
    val textoBusqueda by viewModel.textoBusqueda.collectAsState()
    val variantesPorProducto by viewModel.variantesPorProducto.collectAsState()

    var productoExpandidoId by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    color = TextoCrema,
                    fontSize = 26.sp,
                    modifier = Modifier.clickable(onClick = onVolver)
                )
                Text(
                    text = "Consultar inventario",
                    color = TextoCrema,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            TextField(
                value = textoBusqueda,
                onValueChange = { viewModel.buscar(it) },
                placeholder = { Text("Buscar por nombre, SKU o código...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FondoTarjeta,
                    unfocusedContainerColor = FondoTarjeta,
                    focusedTextColor = TextoCrema,
                    unfocusedTextColor = TextoCrema
                ),
                singleLine = true
            )

            if (productos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (textoBusqueda.isBlank()) "No hay productos registrados." else "Sin resultados para \"$textoBusqueda\".",
                        color = TextoCremaApagado,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productos, key = { it.id }) { producto ->
                        val expandido = productoExpandidoId == producto.id
                        TarjetaProductoConsulta(
                            producto = producto,
                            expandido = expandido,
                            variantes = variantesPorProducto[producto.id],
                            onToggle = {
                                productoExpandidoId = if (expandido) null else producto.id
                                if (!expandido) viewModel.cargarVariantesSiHaceFalta(producto.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProductoConsulta(
    producto: ProductoEntity,
    expandido: Boolean,
    variantes: List<ProductoEntity>?,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FondoTarjeta)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "S/ ${"%.2f".format(producto.precioVenta)} · SKU: ${producto.sku}",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            EtiquetaStock(stock = producto.stockActual, minimo = producto.stockMinimo)
            Text(
                text = if (expandido) "▲" else "▼",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (expandido) {
            if (variantes == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AcentoTerracota, modifier = Modifier.padding(8.dp))
                }
            } else if (variantes.isEmpty()) {
                Text(
                    "Este producto no tiene tallas/colores registrados; el stock de arriba es el total.",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    variantes.forEach { variante ->
                        FilaVariante(variante)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaVariante(variante: ProductoEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FondoVariante)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val etiqueta = listOfNotNull(
            variante.talla?.let { "Talla $it" },
            variante.color
        ).joinToString(" · ").ifEmpty { variante.nombreVariante ?: "Variante" }

        Text(
            etiqueta,
            color = TextoCrema,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        EtiquetaStock(stock = variante.stockActual, minimo = variante.stockMinimo)
    }
}

@Composable
private fun EtiquetaStock(stock: Int, minimo: Int) {
    val bajoMinimo = stock <= minimo
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (bajoMinimo) ColorAlerta.copy(alpha = 0.2f) else AcentoTerracota.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$stock u.",
            color = if (bajoMinimo) ColorAlerta else AcentoTerracota,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
