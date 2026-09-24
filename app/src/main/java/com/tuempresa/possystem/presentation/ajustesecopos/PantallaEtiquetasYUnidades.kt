package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

@Composable
fun PantallaEtiquetasPago(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: EtiquetasPagoViewModel = viewModel(factory = fabricaViewModel(app) { EtiquetasPagoViewModel(app) })
    val etiquetas by viewModel.etiquetas.collectAsState()
    var dialogoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogoAbierto = true }, containerColor = EcoPosColors.AcentoAmbar) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir etiqueta de pago")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Etiqueta de Pago", onVolver = onVolver)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(etiquetas, key = { it.id }) { etiqueta ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(etiqueta.nombre, color = EcoPosColors.TextoBlanco, fontSize = 17.sp)
                        if (etiqueta.esEditable) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar",
                                tint = EcoPosColors.TextoBlanco,
                                modifier = Modifier.clickable { viewModel.eliminar(etiqueta.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (dialogoAbierto) {
        var nombre by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogoAbierto = false },
            title = { Text("Añadir Etiqueta de Pago") },
            text = {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.anadir(nombre)
                    dialogoAbierto = false
                }) { Text("AÑADIR", color = EcoPosColors.VerdeMenta, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoAbierto = false }) { Text("CANCELAR") }
            }
        )
    }
}

@Composable
fun PantallaUnidades(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: UnidadesViewModel = viewModel(factory = fabricaViewModel(app) { UnidadesViewModel(app) })
    val unidades by viewModel.unidades.collectAsState()
    var dialogoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogoAbierto = true }, containerColor = EcoPosColors.AcentoAmbar) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir unidad")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Unidad", onVolver = onVolver)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(unidades, key = { it.id }) { unidad ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(unidad.nombre, color = EcoPosColors.TextoBlanco, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (unidad.esFraccional) "Fraccional" else "Entero",
                                color = EcoPosColors.TextoGrisApagado,
                                fontSize = 13.sp
                            )
                        }
                        if (unidad.esEditable) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar",
                                tint = EcoPosColors.TextoGrisApagado,
                                modifier = Modifier.clickable { viewModel.eliminar(unidad.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (dialogoAbierto) {
        var nombre by remember { mutableStateOf("") }
        var esFraccional by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { dialogoAbierto = false },
            title = { Text("Añadir Unidad") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp).clickable { esFraccional = !esFraccional },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = esFraccional, onCheckedChange = { esFraccional = it })
                        Text("Fraccional", color = EcoPosColors.AcentoAmbar)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.anadir(nombre, esFraccional)
                    dialogoAbierto = false
                }) { Text("AÑADIR", color = EcoPosColors.VerdeMenta, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoAbierto = false }) { Text("CANCELAR") }
            }
        )
    }
}
