package com.tuempresa.possystem.presentation.clientes

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.tuempresa.possystem.data.local.entity.ClienteEntity
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import com.tuempresa.possystem.presentation.theme.ecoPosCamposTextoColores

/** Listado de clientes con FAB "+" para añadir uno nuevo, estilo Eco POS. */
@Composable
fun PantallaClientes(
    app: POSApplication,
    onVolver: () -> Unit,
    onNuevoCliente: () -> Unit
) {
    val viewModel: ClientesViewModel = viewModel(factory = fabricaViewModel(app) { ClientesViewModel(app) })
    val clientes by viewModel.clientes.collectAsState()

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoCliente, containerColor = EcoPosColors.LilaAzulado) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir cliente")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Cliente", onVolver = onVolver)

            if (clientes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay clientes registrados.", color = EcoPosColors.TextoGrisApagado, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clientes, key = { it.id }) { cliente ->
                        TarjetaCliente(cliente)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCliente(cliente: ClienteEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .padding(16.dp)
    ) {
        Text(cliente.nombre, color = EcoPosColors.TextoBlanco, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        val subtitulo = listOfNotNull(cliente.telefono, cliente.email).joinToString(" · ")
        if (subtitulo.isNotBlank()) {
            Text(subtitulo, color = EcoPosColors.TextoGrisApagado, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Formulario "Añadir Cliente": nombre obligatorio, resto opcional. */
@Composable
fun PantallaAnadirCliente(
    app: POSApplication,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val viewModel: ClientesViewModel = viewModel(factory = fabricaViewModel(app) { ClientesViewModel(app) })

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var codigoBarras by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(EcoPosColors.FondoNegro)) {
        CabeceraModal(titulo = "Añadir Cliente", onCerrar = onVolver)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            CampoEtiquetado(etiqueta = "Nombre", valor = nombre, onValorCambia = { nombre = it })

            CampoEtiquetado(
                etiqueta = "Número de teléfono (Opcional)",
                valor = telefono,
                onValorCambia = { telefono = it }
            )

            Text(
                "Código de barras (Opcional)",
                color = EcoPosColors.TextoBlanco,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = codigoBarras,
                onValueChange = { codigoBarras = it },
                modifier = Modifier.fillMaxWidth(),
                shape = EcoPosShapes.Campo,
                colors = ecoPosCamposTextoColores(),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = EcoPosColors.VerdeMenta, modifier = Modifier.size(18.dp))
                        Text("Escanear", color = EcoPosColors.VerdeMenta, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                },
                singleLine = true
            )
            Text(
                "El escaneo para el código del cliente está disponible durante el pago.",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            CampoEtiquetado(etiqueta = "E-mail (Opcional)", valor = email, onValorCambia = { email = it })
            CampoEtiquetado(etiqueta = "Dirección (Opcional)", valor = direccion, onValorCambia = { direccion = it }, lineas = 3)
        }

        BotonGuardar(
            habilitado = nombre.isNotBlank(),
            onClick = {
                viewModel.guardar(
                    nombre = nombre,
                    telefono = telefono,
                    codigoBarras = codigoBarras,
                    email = email,
                    direccion = direccion,
                    onGuardado = onGuardado
                )
            }
        )
    }
}

@Composable
internal fun CabeceraSimple(titulo: String, onVolver: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = EcoPosColors.TextoBlanco,
            modifier = Modifier.clickable(onClick = onVolver)
        )
        Text(
            titulo,
            color = EcoPosColors.TextoBlanco,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
internal fun CabeceraModal(titulo: String, onCerrar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Cerrar",
            tint = EcoPosColors.TextoBlanco,
            modifier = Modifier.clickable(onClick = onCerrar)
        )
        Text(
            titulo,
            color = EcoPosColors.TextoBlanco,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
internal fun CampoEtiquetado(
    etiqueta: String,
    valor: String,
    onValorCambia: (String) -> Unit,
    lineas: Int = 1
) {
    Text(
        etiqueta,
        color = EcoPosColors.TextoBlanco,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        modifier = Modifier.fillMaxWidth(),
        shape = if (lineas > 1) EcoPosShapes.Tarjeta else EcoPosShapes.Campo,
        colors = ecoPosCamposTextoColores(),
        singleLine = lineas == 1,
        minLines = lineas
    )
}

@Composable
internal fun BotonGuardar(habilitado: Boolean, texto: String = "GUARDAR", onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .height(52.dp)
            .clip(EcoPosShapes.Boton)
            .background(if (habilitado) EcoPosColors.VerdeMenta else EcoPosColors.FondoTarjetaClara)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            color = if (habilitado) EcoPosColors.FondoNegro else EcoPosColors.TextoGrisApagado,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
