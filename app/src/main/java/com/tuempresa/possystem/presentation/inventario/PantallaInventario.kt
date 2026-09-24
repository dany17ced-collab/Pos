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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

@Composable
fun PantallaInventario(
    app: POSApplication,
    onVolver: () -> Unit,
    onNuevoProducto: () -> Unit,
    onEntradaMercaderia: () -> Unit,
    onEditarPrecio: (String) -> Unit
) {
    val viewModel: InventarioViewModel = viewModel(factory = fabricaSimple { InventarioViewModel(app) })
    val productos by viewModel.productos.collectAsState()

    var productoAEliminar by remember { mutableStateOf<ProductoEntity?>(null) }
    var menuFabAbierto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { menuFabAbierto = true },
                    containerColor = EcoPosColors.AcentoAmbar
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = EcoPosColors.FondoNegro)
                }
                DropdownMenu(expanded = menuFabAbierto, onDismissRequest = { menuFabAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Nuevo producto") },
                        onClick = { menuFabAbierto = false; onNuevoProducto() }
                    )
                    DropdownMenuItem(
                        text = { Text("Entrada de mercadería") },
                        onClick = { menuFabAbierto = false; onEntradaMercaderia() }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Inventario", onVolver = onVolver)

            if (productos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aún no hay productos.\nToca el botón + para agregar el primero.",
                        color = EcoPosColors.TextoGrisApagado,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(productos, key = { it.id }) { producto ->
                        TarjetaProducto(
                            producto = producto,
                            onEliminar = { productoAEliminar = producto },
                            onEditarPrecio = { onEditarPrecio(producto.id) }
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
                    Text("Eliminar", color = EcoPosColors.ColorError)
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
private fun TarjetaProducto(
    producto: ProductoEntity,
    onEliminar: () -> Unit,
    onEditarPrecio: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(EcoPosShapes.TarjetaChica)
                .background(EcoPosColors.FondoTarjetaClara),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Inventory, contentDescription = null, tint = EcoPosColors.TextoGris, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(producto.nombre, color = EcoPosColors.TextoBlanco, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (producto.descripcion != null) {
                Text(
                    producto.descripcion,
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "S/.${"%.2f".format(producto.precioVenta)} · SKU: ${producto.sku}",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Icon(
            Icons.Filled.Edit,
            contentDescription = "Editar precio",
            tint = EcoPosColors.VerdeMenta,
            modifier = Modifier.size(20.dp).padding(start = 8.dp).clickable(onClick = onEditarPrecio)
        )
        Icon(
            Icons.Filled.Delete,
            contentDescription = "Eliminar",
            tint = EcoPosColors.RojoSalmon,
            modifier = Modifier.size(20.dp).padding(start = 16.dp).clickable(onClick = onEliminar)
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
