package com.tuempresa.possystem.presentation.inventario

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)

@Composable
fun PantallaEditarPrecio(
    app: POSApplication,
    productoId: String,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val viewModel: EditarPrecioViewModel = viewModel(
        factory = fabricaSimple { EditarPrecioViewModel(app, productoId) }
    )

    val producto by viewModel.producto.collectAsState()
    val variantes by viewModel.variantes.collectAsState()
    val estadoGuardado by viewModel.estadoGuardado.collectAsState()

    LaunchedEffect(estadoGuardado) {
        if (estadoGuardado is EstadoGuardadoPrecio.Exitoso) onGuardado()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
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
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = "Editar precio",
                        color = TextoCrema,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    producto?.let {
                        Text(it.nombre, color = TextoCremaApagado, fontSize = 13.sp)
                    }
                }
            }

            SeccionAjusteRapido(onAplicar = viewModel::aplicarAjustePorcentual)

            variantes.forEach { variante ->
                TarjetaVariantePrecio(
                    variante = variante,
                    onPrecioBaseCambiado = { nuevo ->
                        viewModel.actualizarPrecioBase(variante.producto.id, nuevo)
                    },
                    onPrecioEscalonCambiado = { escalonId, nuevo ->
                        viewModel.actualizarPrecioEscalon(variante.producto.id, escalonId, nuevo)
                    }
                )
            }

            if (estadoGuardado is EstadoGuardadoPrecio.Error) {
                Text(
                    text = (estadoGuardado as EstadoGuardadoPrecio.Error).mensaje,
                    color = ColorError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.guardar() },
                enabled = estadoGuardado !is EstadoGuardadoPrecio.Guardando,
                colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                val texto = if (estadoGuardado is EstadoGuardadoPrecio.Guardando) "Guardando..." else "Guardar precio"
                Text(texto, color = FondoCarbon, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

/** Botones de ajuste rápido para aplicar un mismo porcentaje a todos los precios de golpe. */
@Composable
private fun SeccionAjusteRapido(onAplicar: (Double) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            "Ajuste rápido de temporada",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(-15.0, -10.0, 10.0, 15.0).forEach { porcentaje ->
                val etiqueta = if (porcentaje > 0) "+${porcentaje.toInt()}%" else "${porcentaje.toInt()}%"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FondoTarjeta)
                        .clickable { onAplicar(porcentaje) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(etiqueta, color = TextoCrema, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Text(
            "Aplica el porcentaje a todos los precios de abajo. Puedes seguir ajustando cada uno a mano antes de guardar.",
            color = TextoCremaApagado,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun TarjetaVariantePrecio(
    variante: VarianteEnEdicion,
    onPrecioBaseCambiado: (String) -> Unit,
    onPrecioEscalonCambiado: (String, String) -> Unit
) {
    val p = variante.producto
    val titulo = if (p.talla != null || p.color != null) {
        listOfNotNull(p.talla?.let { "Talla $it" }, p.color).joinToString(" · ")
    } else {
        "Precio base"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FondoTarjeta)
            .padding(16.dp)
    ) {
        Text(titulo, color = TextoCrema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

        FilaPrecio(
            etiqueta = "Precio unidad (S/)",
            valor = variante.precioTexto,
            onCambio = onPrecioBaseCambiado
        )

        if (variante.escalones.isNotEmpty()) {
            Text(
                "Precios por mayoreo",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            variante.escalones.forEach { escalon ->
                FilaPrecio(
                    etiqueta = "${escalon.etiqueta} (desde ${escalon.cantidadMinima})",
                    valor = escalon.precioTexto,
                    onCambio = { nuevo -> onPrecioEscalonCambiado(escalon.id, nuevo) }
                )
            }
        }
    }
}

@Composable
private fun FilaPrecio(etiqueta: String, valor: String, onCambio: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            etiqueta,
            color = TextoCremaApagado,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text("S/", color = TextoCremaApagado, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        TextField(
            value = valor,
            onValueChange = { nuevo ->
                if (nuevo.isEmpty() || nuevo.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onCambio(nuevo)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .width(100.dp)
                .height(56.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FondoCarbon,
                unfocusedContainerColor = FondoCarbon,
                focusedTextColor = TextoCrema,
                unfocusedTextColor = TextoCrema,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AcentoTerracota
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}
