package com.tuempresa.possystem.presentation.reportes

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.TipoCorte
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

private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

@Composable
fun PantallaReportes(
    app: POSApplication,
    onVolver: () -> Unit
) {
    val viewModel: ReportesViewModel = viewModel(factory = fabricaSimple { ReportesViewModel(app) })

    val resumenTurno by viewModel.resumenTurno.collectAsState()
    val fechaInicioTurno by viewModel.fechaInicioTurno.collectAsState()
    val historialCortes by viewModel.historialCortes.collectAsState()
    val masVendidos by viewModel.masVendidosHoy.collectAsState()
    val estadoCorte by viewModel.estadoCorte.collectAsState()

    var mostrarDialogoCorteZ by remember { mutableStateOf(false) }
    var mostrarHistorialCompleto by remember { mutableStateOf(false) }

    LaunchedEffect(estadoCorte) {
        if (estadoCorte is EstadoCorte.Exitoso) {
            mostrarDialogoCorteZ = false
        }
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
                Text(
                    text = "Reportes y caja",
                    color = TextoCrema,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            TarjetaResumenTurno(
                resumen = resumenTurno,
                desde = fechaInicioTurno
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.realizarCorteX() },
                    enabled = estadoCorte !is EstadoCorte.Procesando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Corte X", color = TextoCrema)
                }
                Button(
                    onClick = { mostrarDialogoCorteZ = true },
                    enabled = estadoCorte !is EstadoCorte.Procesando,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Corte Z (cerrar)", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
                }
            }

            if (estadoCorte is EstadoCorte.Exitoso) {
                val tipo = (estadoCorte as EstadoCorte.Exitoso).tipo
                val mensaje = if (tipo == TipoCorte.X) "Corte X generado." else "Caja cerrada correctamente."
                Text(
                    mensaje,
                    color = ColorExito,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            if (estadoCorte is EstadoCorte.Error) {
                Text(
                    (estadoCorte as EstadoCorte.Error).mensaje,
                    color = ColorError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            Text(
                "Productos más vendidos hoy",
                color = TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, top = 20.dp, bottom = 8.dp)
            )
            if (masVendidos.isEmpty()) {
                Text(
                    "Aún no hay ventas registradas hoy.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    masVendidos.forEachIndexed { index, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FondoTarjeta)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                color = AcentoTerracota,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.nombreProducto, color = TextoCrema, fontSize = 14.sp)
                                Text(
                                    "${p.unidadesVendidas} unidades",
                                    color = TextoCremaApagado,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                "S/ ${"%.2f".format(p.totalVendido)}",
                                color = TextoCrema,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historial de cortes",
                    color = TextoCrema,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (historialCortes.size > 5) {
                    Text(
                        if (mostrarHistorialCompleto) "Ver menos" else "Ver todo",
                        color = AcentoTerracota,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { mostrarHistorialCompleto = !mostrarHistorialCompleto }
                    )
                }
            }

            val cortesAMostrar = if (mostrarHistorialCompleto) historialCortes else historialCortes.take(5)
            if (cortesAMostrar.isEmpty()) {
                Text(
                    "Todavía no se ha hecho ningún corte de caja.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    cortesAMostrar.forEach { corte ->
                        FilaCorte(corte)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (mostrarDialogoCorteZ) {
        DialogoCorteZ(
            resumen = resumenTurno,
            estadoCorte = estadoCorte,
            onCancelar = {
                mostrarDialogoCorteZ = false
                viewModel.limpiarEstadoCorte()
            },
            onConfirmar = { efectivoContado, fondoSiguiente ->
                viewModel.realizarCorteZ(efectivoContado, fondoSiguiente)
            }
        )
    }
}

@Composable
private fun TarjetaResumenTurno(resumen: com.tuempresa.possystem.data.local.dao.ResumenVentasCaja?, desde: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FondoTarjeta)
            .padding(18.dp)
    ) {
        Text(
            "Turno actual",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (desde > 0) {
            Text(
                "Desde ${formatoFechaHora.format(Date(desde))}",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Text(
            "S/ ${"%.2f".format(resumen?.totalVentas ?: 0.0)}",
            color = TextoCrema,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${resumen?.numeroTransacciones ?: 0} ventas",
            color = TextoCremaApagado,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        FilaTotal("Efectivo", resumen?.totalEfectivo ?: 0.0)
        FilaTotal("Tarjeta", resumen?.totalTarjeta ?: 0.0)
        FilaTotal("Transferencia / QR", resumen?.totalTransferencia ?: 0.0)
        FilaTotal("Descuentos aplicados", resumen?.totalDescuentos ?: 0.0)
    }
}

@Composable
private fun FilaTotal(etiqueta: String, valor: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Text(etiqueta, color = TextoCremaApagado, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("S/ ${"%.2f".format(valor)}", color = TextoCrema, fontSize = 13.sp)
    }
}

@Composable
private fun FilaCorte(corte: CorteCajaEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (corte.tipo == TipoCorte.Z) AcentoTerracota else FondoCarbon)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                corte.tipo.name,
                color = if (corte.tipo == TipoCorte.Z) FondoCarbon else TextoCrema,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp)
        ) {
            Text(formatoFechaHora.format(Date(corte.fechaCorte)), color = TextoCrema, fontSize = 13.sp)
            Text("${corte.numeroTransacciones} ventas", color = TextoCremaApagado, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("S/ ${"%.2f".format(corte.totalVentas)}", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (corte.tipo == TipoCorte.Z && corte.diferencia != null) {
                val colorDif = if (kotlin.math.abs(corte.diferencia) < 0.01) ColorExito else ColorError
                val signo = if (corte.diferencia >= 0) "+" else ""
                Text(
                    "Dif: $signo${"%.2f".format(corte.diferencia)}",
                    color = colorDif,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DialogoCorteZ(
    resumen: com.tuempresa.possystem.data.local.dao.ResumenVentasCaja?,
    estadoCorte: EstadoCorte,
    onCancelar: () -> Unit,
    onConfirmar: (efectivoContado: Double, fondoSiguienteTurno: Double) -> Unit
) {
    var efectivoContadoTexto by remember { mutableStateOf("") }
    val totalEfectivoSistema = resumen?.totalEfectivo ?: 0.0

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cerrar caja (Corte Z)") },
        text = {
            Column {
                Text(
                    "Esto cierra el turno actual. El próximo corte empezará a contar desde cero.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    "Efectivo esperado en caja: S/ ${"%.2f".format(totalEfectivoSistema)}",
                    color = TextoCrema,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text("¿Cuánto efectivo hay físicamente en caja?", color = TextoCrema, fontSize = 13.sp)
                TextField(
                    value = efectivoContadoTexto,
                    onValueChange = { nuevo ->
                        if (nuevo.isEmpty() || nuevo.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            efectivoContadoTexto = nuevo
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoCarbon,
                        unfocusedContainerColor = FondoCarbon,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    )
                )
                if (estadoCorte is EstadoCorte.Error) {
                    Text(
                        estadoCorte.mensaje,
                        color = ColorError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val contado = efectivoContadoTexto.toDoubleOrNull() ?: return@TextButton
                    onConfirmar(contado, 0.0)
                },
                enabled = efectivoContadoTexto.toDoubleOrNull() != null && estadoCorte !is EstadoCorte.Procesando
            ) {
                Text(
                    if (estadoCorte is EstadoCorte.Procesando) "Cerrando..." else "Confirmar cierre",
                    color = AcentoTerracota
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
