package com.tuempresa.possystem.presentation.venta

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.VentaEntity
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)
private val ColorExito = Color(0xFF8FBF8A)

private val formatoHora = SimpleDateFormat("HH:mm", Locale("es", "PE"))

@Composable
fun PantallaMisVentas(
    app: POSApplication,
    onVolver: () -> Unit
) {
    val viewModel: MisVentasViewModel = viewModel(factory = fabricaSimple { MisVentasViewModel(app) })

    val ventas by viewModel.ventasDeHoy.collectAsState()
    val detallesPorVenta by viewModel.detallesPorVenta.collectAsState()
    val estadoAnulacion by viewModel.estadoAnulacion.collectAsState()

    var ventaExpandidaId by remember { mutableStateOf<String?>(null) }
    var ventaAAnular by remember { mutableStateOf<VentaEntity?>(null) }

    LaunchedEffect(estadoAnulacion) {
        if (estadoAnulacion is EstadoAnulacion.Exitoso) {
            ventaAAnular = null
            viewModel.limpiarEstadoAnulacion()
        }
    }

    val totalDelDia = ventas.filter { it.estado == EstadoVenta.COMPLETADA }.sumOf { it.total }

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
                    text = "Mis ventas de hoy",
                    color = TextoCrema,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(
                    "Total del día: S/ ${"%.2f".format(totalDelDia)}  ·  ${ventas.count { it.estado == EstadoVenta.COMPLETADA }} ventas",
                    color = TextoCremaApagado,
                    fontSize = 13.sp
                )
            }

            if (ventas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aún no has registrado ventas hoy.",
                        color = TextoCremaApagado,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ventas, key = { it.id }) { venta ->
                        val expandida = ventaExpandidaId == venta.id
                        TarjetaVenta(
                            venta = venta,
                            expandida = expandida,
                            detalles = detallesPorVenta[venta.id],
                            onToggle = {
                                ventaExpandidaId = if (expandida) null else venta.id
                                if (!expandida) viewModel.cargarDetalleSiHaceFalta(venta.id)
                            },
                            onAnular = { ventaAAnular = venta }
                        )
                    }
                }
            }
        }
    }

    val objetivo = ventaAAnular
    if (objetivo != null) {
        DialogoAnularVenta(
            venta = objetivo,
            estadoAnulacion = estadoAnulacion,
            onCancelar = {
                ventaAAnular = null
                viewModel.limpiarEstadoAnulacion()
            },
            onConfirmar = { motivo -> viewModel.anularVenta(objetivo.id, motivo) }
        )
    }
}

@Composable
private fun TarjetaVenta(
    venta: VentaEntity,
    expandida: Boolean,
    detalles: List<com.tuempresa.possystem.data.local.entity.DetalleVentaEntity>?,
    onToggle: () -> Unit,
    onAnular: () -> Unit
) {
    val anulada = venta.estado == EstadoVenta.ANULADA

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatoHora.format(Date(venta.fecha)),
                        color = TextoCrema,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "  ·  ${etiquetaMetodoPago(venta.metodoPago)}",
                        color = TextoCremaApagado,
                        fontSize = 13.sp
                    )
                    if (anulada) {
                        Text(
                            "  ·  ANULADA",
                            color = ColorError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (venta.folio != null) {
                    Text(
                        "Folio #${venta.folio}",
                        color = TextoCremaApagado,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Text(
                "S/ ${"%.2f".format(venta.total)}",
                color = if (anulada) TextoCremaApagado else TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (expandida) "▲" else "▼",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (expandida) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                if (detalles == null) {
                    Text("Cargando detalle...", color = TextoCremaApagado, fontSize = 12.sp)
                } else {
                    detalles.forEach { linea ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                "${linea.cantidad}x ${linea.nombreProducto}",
                                color = TextoCremaApagado,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "S/ ${"%.2f".format(linea.subtotal)}",
                                color = TextoCremaApagado,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (venta.notaAnulacion != null) {
                    Text(
                        "Motivo de anulación: ${venta.notaAnulacion}",
                        color = ColorError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (!anulada) {
                    Text(
                        "Anular esta venta",
                        color = ColorError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable(onClick = onAnular)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoAnularVenta(
    venta: VentaEntity,
    estadoAnulacion: EstadoAnulacion,
    onCancelar: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Anular esta venta?") },
        text = {
            Column {
                Text(
                    "Se repondrá el stock de los productos vendidos. Esta acción no se puede deshacer."
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true
                )
                if (estadoAnulacion is EstadoAnulacion.Error) {
                    Text(
                        estadoAnulacion.mensaje,
                        color = ColorError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(motivo.ifBlank { "Sin motivo especificado" }) },
                enabled = estadoAnulacion !is EstadoAnulacion.Procesando
            ) {
                Text(
                    if (estadoAnulacion is EstadoAnulacion.Procesando) "Anulando..." else "Anular venta",
                    color = ColorError
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

private fun etiquetaMetodoPago(metodo: MetodoPago): String = when (metodo) {
    MetodoPago.EFECTIVO -> "Efectivo"
    MetodoPago.TARJETA -> "Tarjeta"
    MetodoPago.TRANSFERENCIA -> "Transferencia"
    MetodoPago.QR -> "QR"
    MetodoPago.MIXTO -> "Mixto"
}
