package com.tuempresa.possystem.presentation.descuentos

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.tuempresa.possystem.data.local.entity.DescuentoEntity
import com.tuempresa.possystem.data.local.entity.TipoValorDescuento
import com.tuempresa.possystem.presentation.clientes.BotonGuardar
import com.tuempresa.possystem.presentation.clientes.CabeceraModal
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.clientes.CampoEtiquetado
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import com.tuempresa.possystem.presentation.theme.ecoPosCamposTextoColores

@Composable
fun PantallaDescuentos(
    app: POSApplication,
    onVolver: () -> Unit,
    onNuevoDescuento: () -> Unit
) {
    val viewModel: DescuentosViewModel = viewModel(factory = fabricaViewModel(app) { DescuentosViewModel(app) })
    val descuentos by viewModel.descuentos.collectAsState()

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoDescuento, containerColor = EcoPosColors.AcentoAmbar) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir descuento")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Descuento", onVolver = onVolver)

            if (descuentos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay descuentos.", color = EcoPosColors.TextoGrisApagado, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(descuentos, key = { it.id }) { descuento ->
                        TarjetaDescuento(descuento)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaDescuento(descuento: DescuentoEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(descuento.nombre, color = EcoPosColors.TextoBlanco, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (!descuento.codigoPromocional.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(EcoPosShapes.Chip)
                        .background(EcoPosColors.VerdeMenta)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(descuento.codigoPromocional, color = EcoPosColors.FondoNegro, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        val textoValor = if (descuento.tipoValor == TipoValorDescuento.PORCENTAJE) {
            "${"%.0f".format(descuento.valor)}%"
        } else {
            "S/.${"%.2f".format(descuento.valor)}"
        }
        Text(textoValor, color = EcoPosColors.TextoBlanco, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PantallaAnadirDescuento(
    app: POSApplication,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val viewModel: DescuentosViewModel = viewModel(factory = fabricaViewModel(app) { DescuentosViewModel(app) })

    var tabSeleccionada by remember { mutableStateOf(0) }
    var nombre by remember { mutableStateOf("") }
    var codigoPromocional by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var tipoValor by remember { mutableStateOf(TipoValorDescuento.FIJO) }
    var valorTexto by remember { mutableStateOf("") }
    var menuTipoAbierto by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(EcoPosColors.FondoNegro)) {
        CabeceraModal(titulo = "Añadir Descuento", onCerrar = onVolver)

        TabRow(
            selectedTabIndex = tabSeleccionada,
            containerColor = EcoPosColors.FondoNegro,
            contentColor = EcoPosColors.TextoBlanco
        ) {
            Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 }, text = { Text("INFO") })
            Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 }, text = { Text("PRODUCTO") })
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (tabSeleccionada == 0) {
                CampoEtiquetado(etiqueta = "Nombre", valor = nombre, onValorCambia = { nombre = it })
                CampoEtiquetado(
                    etiqueta = "Código promocional (Opcional)",
                    valor = codigoPromocional,
                    onValorCambia = { codigoPromocional = it }
                )
                Text(
                    "Se imprimirá en el recibo",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                CampoEtiquetado(
                    etiqueta = "Descripción (Opcional)",
                    valor = descripcion,
                    onValorCambia = { descripcion = it },
                    lineas = 3
                )

                Text(
                    "Valor",
                    color = EcoPosColors.TextoBlanco,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(EcoPosShapes.Campo)
                            .background(EcoPosColors.FondoInput)
                            .clickable { menuTipoAbierto = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (tipoValor == TipoValorDescuento.FIJO) "Fijo" else "Porcentaje",
                                color = EcoPosColors.TextoBlanco,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = EcoPosColors.TextoBlanco)
                        }
                        DropdownMenu(expanded = menuTipoAbierto, onDismissRequest = { menuTipoAbierto = false }) {
                            DropdownMenuItem(text = { Text("Fijo") }, onClick = { tipoValor = TipoValorDescuento.FIJO; menuTipoAbierto = false })
                            DropdownMenuItem(text = { Text("Porcentaje") }, onClick = { tipoValor = TipoValorDescuento.PORCENTAJE; menuTipoAbierto = false })
                        }
                    }
                    OutlinedTextField(
                        value = valorTexto,
                        onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) valorTexto = nuevo },
                        modifier = Modifier.weight(1f),
                        shape = EcoPosShapes.Campo,
                        colors = ecoPosCamposTextoColores(),
                        prefix = { if (tipoValor == TipoValorDescuento.FIJO) Text("S/.", color = EcoPosColors.TextoGrisApagado) },
                        suffix = { if (tipoValor == TipoValorDescuento.PORCENTAJE) Text("%", color = EcoPosColors.TextoGrisApagado) },
                        singleLine = true
                    )
                }
            } else {
                Text(
                    "Selecciona el producto al que aplicará este descuento desde el carrito al momento de vender.",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }

        BotonGuardar(
            habilitado = nombre.isNotBlank(),
            onClick = {
                viewModel.guardar(
                    nombre = nombre,
                    codigoPromocional = codigoPromocional,
                    descripcion = descripcion,
                    tipoValor = tipoValor,
                    valor = valorTexto.toDoubleOrNull() ?: 0.0,
                    productoId = null,
                    onGuardado = onGuardado
                )
            }
        )
    }
}
