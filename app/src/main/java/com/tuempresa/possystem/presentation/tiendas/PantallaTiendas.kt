package com.tuempresa.possystem.presentation.tiendas

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import com.tuempresa.possystem.presentation.theme.ecoPosCamposTextoColores

/**
 * Gestión de sucursales (solo ADMIN): lista todas las tiendas, permite crear
 * una nueva, cambiar cuál está activa en este dispositivo, renombrar, o
 * desactivar una que ya no opera. El inventario, ventas y reportes que ve el
 * resto de la app siempre corresponden a la tienda marcada como "activa" aquí.
 */
@Composable
fun PantallaTiendas(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: TiendasViewModel = viewModel(factory = fabricaViewModel(app) { TiendasViewModel(app) })
    val tiendas by viewModel.tiendas.collectAsState()
    val tiendaActiva by viewModel.tiendaActiva.collectAsState()

    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var tiendaAEditar by remember { mutableStateOf<TiendaEntity?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoNueva = true }, containerColor = EcoPosColors.AcentoAmbar) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar tienda")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Tiendas", subtitulo = "Gestiona tus sucursales y cuál está activa", onVolver = onVolver)

            Text(
                "La tienda marcada como activa determina qué inventario, ventas y reportes ves en el resto de la app.",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (tiendas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay tiendas registradas.", color = EcoPosColors.TextoGrisApagado, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tiendas, key = { it.id }) { tienda ->
                        TarjetaTienda(
                            tienda = tienda,
                            esActiva = tienda.id == tiendaActiva?.id,
                            onSeleccionar = { viewModel.seleccionarComoActiva(tienda) },
                            onEditar = { tiendaAEditar = tienda },
                            onDesactivar = {
                                viewModel.desactivar(tienda) { error -> mensajeError = error }
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoNueva) {
        DialogoNombreTienda(
            titulo = "Nueva tienda",
            valorInicial = "",
            onConfirmar = { nombre ->
                viewModel.crearTienda(nombre) { mostrarDialogoNueva = false }
            },
            onCancelar = { mostrarDialogoNueva = false }
        )
    }

    tiendaAEditar?.let { tienda ->
        DialogoNombreTienda(
            titulo = "Renombrar tienda",
            valorInicial = tienda.nombre,
            onConfirmar = { nombre ->
                viewModel.renombrar(tienda, nombre)
                tiendaAEditar = null
            },
            onCancelar = { tiendaAEditar = null }
        )
    }

    mensajeError?.let { error ->
        AlertDialog(
            onDismissRequest = { mensajeError = null },
            confirmButton = {
                Text(
                    "Entendido",
                    color = EcoPosColors.AcentoAmbar,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { mensajeError = null }
                        .padding(8.dp)
                )
            },
            title = { Text("No se puede desactivar", color = EcoPosColors.TextoBlanco) },
            text = { Text(error, color = EcoPosColors.TextoGrisApagado) },
            containerColor = EcoPosColors.FondoTarjeta
        )
    }
}

@Composable
private fun TarjetaTienda(
    tienda: TiendaEntity,
    esActiva: Boolean,
    onSeleccionar: () -> Unit,
    onEditar: () -> Unit,
    onDesactivar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(if (esActiva) EcoPosColors.FondoTarjetaClara else EcoPosColors.FondoTarjeta)
            .clickable(onClick = onSeleccionar)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (esActiva) EcoPosColors.VerdeMenta else EcoPosColors.FondoNegro),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Store,
                contentDescription = null,
                tint = if (esActiva) EcoPosColors.FondoNegro else EcoPosColors.TextoGrisApagado,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(tienda.nombre, color = EcoPosColors.TextoBlanco, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                if (esActiva) "Activa en este dispositivo" else "Toca para activar",
                color = if (esActiva) EcoPosColors.VerdeMenta else EcoPosColors.TextoGrisApagado,
                fontSize = 12.sp
            )
        }

        if (esActiva) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Activa", tint = EcoPosColors.VerdeMenta, modifier = Modifier.size(20.dp))
        }

        Icon(
            Icons.Filled.Edit,
            contentDescription = "Renombrar",
            tint = EcoPosColors.TextoGrisApagado,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(18.dp)
                .clickable(onClick = onEditar)
        )

        Icon(
            Icons.Filled.PowerSettingsNew,
            contentDescription = "Desactivar",
            tint = EcoPosColors.ColorError,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(18.dp)
                .clickable(onClick = onDesactivar)
        )
    }
}

@Composable
private fun DialogoNombreTienda(
    titulo: String,
    valorInicial: String,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(valorInicial) }

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
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = { Text("Ej. Tienda Norte") },
                singleLine = true,
                shape = EcoPosShapes.Campo,
                colors = ecoPosCamposTextoColores(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(EcoPosShapes.Boton)
                    .background(if (nombre.isNotBlank()) EcoPosColors.VerdeMenta else EcoPosColors.FondoTarjetaClara)
                    .clickable(enabled = nombre.isNotBlank()) { onConfirmar(nombre) },
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
