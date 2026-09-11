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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)

@Composable
fun PantallaInventario(
    app: POSApplication,
    onVolver: () -> Unit,
    onNuevoProducto: () -> Unit
) {
    val viewModel: InventarioViewModel = viewModel(factory = fabricaSimple { InventarioViewModel(app) })
    val productos by viewModel.productos.collectAsState()

    var productoAEliminar by remember { mutableStateOf<ProductoEntity?>(null) }

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
                    text = "Inventario",
                    color = TextoCrema,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AcentoTerracota)
                        .clickable(onClick = onNuevoProducto)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("+ Nuevo", color = FondoCarbon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (productos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aún no hay productos.\nToca \"+ Nuevo\" para agregar el primero.",
                        color = TextoCremaApagado,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productos, key = { it.id }) { producto ->
                        TarjetaProducto(
                            producto = producto,
                            onEliminar = { productoAEliminar = producto }
                        )
                    }
                }
            }
        }
    }

    val productoObjetivo = productoAEliminar
    if (productoObjetivo != null) {
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("¿Eliminar producto?") },
            text = {
                Text("Se eliminará \"${productoObjetivo.nombre}\" y todas sus tallas y colores. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarProductoConVariantes(productoObjetivo.id)
                    productoAEliminar = null
                }) {
                    Text("Eliminar", color = ColorError)
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TarjetaProducto(producto: ProductoEntity, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FondoTarjeta)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(producto.nombre, color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (producto.descripcion != null) {
                Text(
                    producto.descripcion,
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "S/ ${"%.2f".format(producto.precioVenta)} · SKU: ${producto.sku}",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Text(
            text = "🗑",
            fontSize = 18.sp,
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable(onClick = onEliminar)
        )
    }
}

/** Fábrica genérica y reutilizable de ViewModel simple (sin SavedStateHandle). */
fun <T : androidx.lifecycle.ViewModel> fabricaSimple(crear: () -> T): androidx.lifecycle.ViewModelProvider.Factory {
    return object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <U : androidx.lifecycle.ViewModel> create(modelClass: Class<U>): U {
            return crear() as U
        }
    }
}
