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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

/**
 * Etiquetas de pago personalizadas (ej. "Yape", "Plin" mapeados a un método
 * base). Fila de tarjeta con nombre + eliminar; agregar es un diálogo con la
 * misma paleta de marca.
 */
@Composable
fun PantallaEtiquetasPago(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: EtiquetasPagoViewModel = viewModel(factory = fabricaViewModel(app) { EtiquetasPagoViewModel(app) })
    val etiquetas by viewModel.etiquetas.collectAsState()
    var dialogoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogoAbierto = true }, containerColor = EcoPosColors.RosaVivo) {
                Text("+", fontSize = 22.sp, color = EcoPosColors.TextoBlanco, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Etiquetas de pago", subtitulo = "Nombres personalizados para tus métodos de pago", onVolver = onVolver)
            if (etiquetas.isEmpty()) {
                Text(
                    "Aún no hay etiquetas personalizadas.",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(etiquetas, key = { it.id }) { etiqueta ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(EcoPosShapes.TarjetaChica)
                            .background(EcoPosColors.FondoTarjeta)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(etiqueta.nombre, color = EcoPosColors.TextoBlanco, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        if (etiqueta.esEditable) {
                            Text(
                                "✕",
                                color = EcoPosColors.ColorError,
                                fontSize = 15.sp,
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
        DialogoNombre(
            titulo = "Nueva etiqueta de pago",
            valor = nombre,
            onCambio = { nombre = it },
            onConfirmar = {
                viewModel.anadir(nombre)
                dialogoAbierto = false
            },
            onCancelar = { dialogoAbierto = false }
        )
    }
}

/** Unidades de medida (ej. kg, docena) para productos que no se venden por unidad. */
@Composable
fun PantallaUnidades(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: UnidadesViewModel = viewModel(factory = fabricaViewModel(app) { UnidadesViewModel(app) })
    val unidades by viewModel.unidades.collectAsState()
    var dialogoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogoAbierto = true }, containerColor = EcoPosColors.RosaVivo) {
                Text("+", fontSize = 22.sp, color = EcoPosColors.TextoBlanco, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Unidades de medida", subtitulo = "Para productos que no se venden por unidad", onVolver = onVolver)
            if (unidades.isEmpty()) {
                Text(
                    "Aún no hay unidades personalizadas.",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unidades, key = { it.id }) { unidad ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(EcoPosShapes.TarjetaChica)
                            .background(EcoPosColors.FondoTarjeta)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(unidad.nombre, color = EcoPosColors.TextoBlanco, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (unidad.esFraccional) "Fraccional" else "Entero",
                                color = EcoPosColors.TextoGrisApagado,
                                fontSize = 12.sp
                            )
                        }
                        if (unidad.esEditable) {
                            Text(
                                "✕",
                                color = EcoPosColors.ColorError,
                                fontSize = 15.sp,
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
            containerColor = EcoPosColors.FondoTarjeta,
            title = { Text("Nueva unidad", color = EcoPosColors.TextoBlanco, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = { Text("Ej. Kilogramo, Docena") },
                        singleLine = true,
                        colors = camposTextoColoresAjustes(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.padding(top = 14.dp).clickable { esFraccional = !esFraccional },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = esFraccional,
                            onCheckedChange = { esFraccional = it },
                            colors = CheckboxDefaults.colors(checkedColor = EcoPosColors.RosaVivo)
                        )
                        Text("Admite decimales (ej. 1.5 kg)", color = EcoPosColors.TextoBlanco, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Text(
                    "Guardar",
                    color = EcoPosColors.RosaVivo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = nombre.isNotBlank()) {
                            viewModel.anadir(nombre, esFraccional)
                            dialogoAbierto = false
                        }
                        .padding(8.dp)
                )
            },
            dismissButton = {
                Text(
                    "Cancelar",
                    color = EcoPosColors.TextoGrisApagado,
                    modifier = Modifier.clickable { dialogoAbierto = false }.padding(8.dp)
                )
            }
        )
    }
}

@Composable
private fun DialogoNombre(
    titulo: String,
    valor: String,
    onCambio: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = EcoPosColors.FondoTarjeta,
        title = { Text(titulo, color = EcoPosColors.TextoBlanco, fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = valor,
                onValueChange = onCambio,
                placeholder = { Text("Nombre") },
                singleLine = true,
                colors = camposTextoColoresAjustes(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Text(
                "Guardar",
                color = EcoPosColors.RosaVivo,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = valor.isNotBlank(), onClick = onConfirmar)
                    .padding(8.dp)
            )
        },
        dismissButton = {
            Text(
                "Cancelar",
                color = EcoPosColors.TextoGrisApagado,
                modifier = Modifier.clickable(onClick = onCancelar).padding(8.dp)
            )
        }
    )
}

@Composable
private fun camposTextoColoresAjustes() = TextFieldDefaults.colors(
    focusedContainerColor = EcoPosColors.FondoNegro,
    unfocusedContainerColor = EcoPosColors.FondoNegro,
    focusedTextColor = EcoPosColors.TextoBlanco,
    unfocusedTextColor = EcoPosColors.TextoBlanco,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = EcoPosColors.RosaVivo
)
