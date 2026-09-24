package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import com.tuempresa.possystem.presentation.theme.ecoPosCamposTextoColores

/**
 * Gestión real de categorías de producto (Ajustes → Categorías y colores):
 * crear, renombrar, cambiar el color identificador y eliminar. El color
 * elegido aquí es el mismo que se usa luego en el grid de
 * PantallaInventarioCategorias, así que un cambio acá se refleja ahí.
 */
@Composable
fun PantallaCategoriasColores(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: CategoriasColoresViewModel = viewModel(factory = fabricaViewModel(app) { CategoriasColoresViewModel(app) })
    val categorias by viewModel.categorias.collectAsState()
    val conteoPorCategoria by viewModel.conteoPorCategoria.collectAsState()
    val error by viewModel.error.collectAsState()

    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var categoriaAEditar by remember { mutableStateOf<CategoriaEntity?>(null) }
    var categoriaAEliminar by remember { mutableStateOf<CategoriaEntity?>(null) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoNueva = true }, containerColor = EcoPosColors.AcentoAmbar) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva categoría", tint = EcoPosColors.FondoNegro)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(
                titulo = "Categorías y colores",
                subtitulo = "Organiza tus productos y su color en el grid de inventario",
                onVolver = onVolver
            )

            if (categorias.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aún no tienes categorías.", color = EcoPosColors.TextoGrisApagado, fontSize = 14.sp)
                        Text(
                            "Toca + para crear la primera (ej. Polos, Pantalones).",
                            color = EcoPosColors.TextoGrisApagado,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categorias, key = { it.id }) { categoria ->
                        TarjetaCategoria(
                            categoria = categoria,
                            cantidadProductos = conteoPorCategoria[categoria.id] ?: 0,
                            onEditar = { categoriaAEditar = categoria },
                            onEliminar = { categoriaAEliminar = categoria }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoNueva) {
        DialogoCategoria(
            titulo = "Nueva categoría",
            nombreInicial = "",
            colorInicial = COLORES_CATEGORIA_HEX.first(),
            errorExterno = error,
            onConfirmar = { nombre, color -> viewModel.crearCategoria(nombre, color) },
            onCancelar = { mostrarDialogoNueva = false; viewModel.limpiarError() },
            onCerrarSiExitoso = { if (error == null) mostrarDialogoNueva = false }
        )
    }

    categoriaAEditar?.let { categoria ->
        DialogoCategoria(
            titulo = "Editar categoría",
            nombreInicial = categoria.nombre,
            colorInicial = categoria.colorHex ?: COLORES_CATEGORIA_HEX.first(),
            errorExterno = error,
            onConfirmar = { nombre, color -> viewModel.actualizarCategoria(categoria, nombre, color) },
            onCancelar = { categoriaAEditar = null; viewModel.limpiarError() },
            onCerrarSiExitoso = { if (error == null) categoriaAEditar = null }
        )
    }

    categoriaAEliminar?.let { categoria ->
        val cantidad = conteoPorCategoria[categoria.id] ?: 0
        AlertDialog(
            onDismissRequest = { categoriaAEliminar = null },
            containerColor = EcoPosColors.FondoTarjeta,
            title = { Text("Eliminar \"${categoria.nombre}\"", color = EcoPosColors.TextoBlanco, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (cantidad > 0) {
                        "Hay $cantidad producto${if (cantidad == 1) "" else "s"} en esta categoría. Se quedarán sin categoría, pero no se borran."
                    } else {
                        "Esta categoría no tiene productos asignados."
                    },
                    color = EcoPosColors.TextoGrisApagado
                )
            },
            confirmButton = {
                Text(
                    "Eliminar",
                    color = EcoPosColors.ColorError,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            viewModel.eliminarCategoria(categoria)
                            categoriaAEliminar = null
                        }
                        .padding(8.dp)
                )
            },
            dismissButton = {
                Text(
                    "Cancelar",
                    color = EcoPosColors.TextoGrisApagado,
                    modifier = Modifier.clickable { categoriaAEliminar = null }.padding(8.dp)
                )
            }
        )
    }
}

@Composable
private fun TarjetaCategoria(
    categoria: CategoriaEntity,
    cantidadProductos: Int,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val colorCategoria = parsearColorHexSeguro(categoria.colorHex)
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
                .size(36.dp)
                .clip(CircleShape)
                .background(colorCategoria)
        )

        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(categoria.nombre, color = EcoPosColors.TextoBlanco, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "$cantidadProductos producto${if (cantidadProductos == 1) "" else "s"}",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 12.sp
            )
        }

        Icon(
            Icons.Filled.Edit,
            contentDescription = "Editar",
            tint = EcoPosColors.TextoGrisApagado,
            modifier = Modifier.padding(start = 10.dp).size(18.dp).clickable(onClick = onEditar)
        )
        Icon(
            Icons.Filled.Delete,
            contentDescription = "Eliminar",
            tint = EcoPosColors.ColorError,
            modifier = Modifier.padding(start = 14.dp).size(18.dp).clickable(onClick = onEliminar)
        )
    }
}

@Composable
private fun DialogoCategoria(
    titulo: String,
    nombreInicial: String,
    colorInicial: String,
    errorExterno: String?,
    onConfirmar: (String, String) -> Unit,
    onCancelar: () -> Unit,
    onCerrarSiExitoso: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var colorSeleccionado by remember { mutableStateOf(colorInicial) }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = EcoPosColors.FondoTarjeta,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(titulo, color = EcoPosColors.TextoBlanco, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Cancelar",
                    tint = EcoPosColors.TextoGrisApagado,
                    modifier = Modifier.size(20.dp).clickable(onClick = onCancelar)
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = { Text("Ej. Polos, Pantalones, Casacas") },
                    singleLine = true,
                    shape = EcoPosShapes.Campo,
                    colors = ecoPosCamposTextoColores(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorExterno != null) {
                    Text(errorExterno, color = EcoPosColors.ColorError, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }

                Text("Color", color = EcoPosColors.TextoGrisApagado, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLORES_CATEGORIA_HEX.forEach { hex ->
                        val seleccionado = hex == colorSeleccionado
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsearColorHexSeguro(hex))
                                .border(
                                    width = if (seleccionado) 2.5.dp else 0.dp,
                                    color = EcoPosColors.TextoBlanco,
                                    shape = CircleShape
                                )
                                .clickable { colorSeleccionado = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(EcoPosShapes.Boton)
                    .background(if (nombre.isNotBlank()) EcoPosColors.VerdeMenta else EcoPosColors.FondoTarjetaClara)
                    .clickable(enabled = nombre.isNotBlank()) {
                        onConfirmar(nombre, colorSeleccionado)
                        onCerrarSiExitoso()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "GUARDAR",
                    color = if (nombre.isNotBlank()) EcoPosColors.FondoNegro else EcoPosColors.TextoGrisApagado,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    )
}

private fun parsearColorHexSeguro(hex: String?): Color {
    if (hex.isNullOrBlank()) return EcoPosColors.TextoGrisApagado
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        EcoPosColors.TextoGrisApagado
    }
}
