package com.tuempresa.possystem.presentation.caja

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.ResumenVentasCaja
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.TipoCorte
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.AcentoAmbar
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito
private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

/**
 * Cierre de caja del turno vigente en la tienda activa: ver el resumen del
 * turno, hacer un corte X (parcial, informativo) o un corte Z (cierra el
 * turno y pide contar el efectivo físico). Accesible para VENDEDOR y ADMIN
 * por igual — es la operación diaria de mostrador, no una función administrativa.
 */
@Composable
fun PantallaCierreCaja(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: CierreCajaViewModel = viewModel(factory = fabricaViewModel(app) { CierreCajaViewModel(app) })

    val resumenTurno by viewModel.resumenTurno.collectAsState()
    val fechaInicioTurno by viewModel.fechaInicioTurno.collectAsState()
    val fondoInicialTurno by viewModel.fondoInicialTurno.collectAsState()
    val historialCortes by viewModel.historialCortes.collectAsState()
    val estadoCorte by viewModel.estadoCorte.collectAsState()

    var mostrarDialogoCorteZ by remember { mutableStateOf(false) }
    var mostrarHistorialCompleto by remember { mutableStateOf(false) }

    LaunchedEffect(estadoCorte) {
        val estado = estadoCorte
        if (estado is EstadoCorte.Exitoso && estado.tipo == TipoCorte.Z) {
            mostrarDialogoCorteZ = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            com.tuempresa.possystem.presentation.clientes.CabeceraSimple(
                titulo = "Cierre de caja",
                subtitulo = "Resumen del turno y cortes X/Z",
                onVolver = onVolver
            )

            TarjetaResumenTurno(resumen = resumenTurno, desde = fechaInicioTurno, fondoInicial = fondoInicialTurno)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.realizarCorteX() },
                    enabled = estadoCorte !is EstadoCorte.Procesando && fondoInicialTurno != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Corte X", color = TextoCrema)
                }
                Button(
                    onClick = { mostrarDialogoCorteZ = true },
                    enabled = estadoCorte !is EstadoCorte.Procesando && fondoInicialTurno != null,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Corte Z (cerrar)", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
                }
            }

            when (estadoCorte) {
                is EstadoCorte.Exitoso -> {
                    val tipo = (estadoCorte as EstadoCorte.Exitoso).tipo
                    val mensaje = if (tipo == TipoCorte.X) "Corte X generado." else "Caja cerrada correctamente."
                    Text(mensaje, color = ColorExito, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
                is EstadoCorte.Error -> {
                    Text(
                        (estadoCorte as EstadoCorte.Error).mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                else -> {}
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
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

            val cortesAMostrar = (if (mostrarHistorialCompleto) historialCortes else historialCortes.take(5))
                .filter { it.tipo != TipoCorte.APERTURA }
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
            onConfirmar = { efectivoContado, fondoSiguiente -> viewModel.realizarCorteZ(efectivoContado, fondoSiguiente) }
        )
    }

    // Diálogo de apertura de turno: obligatorio (sin opción de cancelar) cuando
    // no hay un fondo inicial declarado — primer ingreso del día en esta
    // tienda, o justo después de cerrar el turno anterior con Corte Z.
    if (fondoInicialTurno == null) {
        DialogoAperturaTurno(
            estadoCorte = estadoCorte,
            onConfirmar = { fondoInicial -> viewModel.abrirTurno(fondoInicial) }
        )
    }
}

@Composable
private fun TarjetaResumenTurno(resumen: ResumenVentasCaja?, desde: Long, fondoInicial: Double?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FondoTarjeta)
            .padding(18.dp)
    ) {
        Text("Turno actual", color = TextoCremaApagado, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (desde > 0) {
            Text(
                "Desde ${formatoFechaHora.format(Date(desde))}",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Text("S/ ${"%.2f".format(resumen?.totalVentas ?: 0.0)}", color = TextoCrema, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "${resumen?.numeroTransacciones ?: 0} ventas",
            color = TextoCremaApagado,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        if (fondoInicial != null) {
            FilaTotal("Fondo inicial de caja", fondoInicial)
        }
        FilaTotal("Efectivo", resumen?.totalEfectivo ?: 0.0)
        FilaTotal("Tarjeta", resumen?.totalTarjeta ?: 0.0)
        FilaTotal("Transferencia / QR", resumen?.totalTransferencia ?: 0.0)
        FilaTotal("Descuentos aplicados", resumen?.totalDescuentos ?: 0.0)
    }
}

@Composable
private fun FilaTotal(etiqueta: String, valor: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
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
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(formatoFechaHora.format(Date(corte.fechaCorte)), color = TextoCrema, fontSize = 13.sp)
            Text("${corte.numeroTransacciones} ventas", color = TextoCremaApagado, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("S/ ${"%.2f".format(corte.totalVentas)}", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (corte.tipo == TipoCorte.Z && corte.diferencia != null) {
                val colorDif = if (kotlin.math.abs(corte.diferencia) < 0.01) ColorExito else ColorError
                val signo = if (corte.diferencia >= 0) "+" else ""
                Text("Dif: $signo${"%.2f".format(corte.diferencia)}", color = colorDif, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DialogoAperturaTurno(estadoCorte: EstadoCorte, onConfirmar: (fondoInicial: Double) -> Unit) {
    var fondoTexto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Obligatorio: no se cierra tocando fuera, hay que declarar el fondo. */ },
        title = { Text("Abrir caja") },
        text = {
            Column {
                Text(
                    "Antes de vender, cuenta el efectivo con el que empiezas el turno.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text("¿Cuánto efectivo hay en caja para empezar?", color = TextoCrema, fontSize = 13.sp)
                TextField(
                    value = fondoTexto,
                    onValueChange = { nuevo ->
                        if (nuevo.isEmpty() || nuevo.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            fondoTexto = nuevo
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoCarbon,
                        unfocusedContainerColor = FondoCarbon,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    )
                )
                if (estadoCorte is EstadoCorte.Error) {
                    Text(estadoCorte.mensaje, color = ColorError, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fondo = fondoTexto.toDoubleOrNull() ?: return@TextButton
                    onConfirmar(fondo)
                },
                enabled = fondoTexto.toDoubleOrNull() != null && estadoCorte !is EstadoCorte.Procesando
            ) {
                Text(if (estadoCorte is EstadoCorte.Procesando) "Guardando..." else "Empezar turno", color = AcentoTerracota)
            }
        }
    )
}

@Composable
private fun DialogoCorteZ(
    resumen: ResumenVentasCaja?,
    estadoCorte: EstadoCorte,
    onCancelar: () -> Unit,
    onConfirmar: (efectivoContado: Double, fondoSiguienteTurno: Double) -> Unit
) {
    var efectivoContadoTexto by remember { mutableStateOf("") }
    var fondoSiguienteTexto by remember { mutableStateOf("") }
    val totalEfectivoSistema = resumen?.totalEfectivo ?: 0.0

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cerrar caja (Corte Z)") },
        text = {
            Column {
                Text(
                    "Esto cierra el turno actual. El próximo turno deberá declarar su propio fondo inicial.",
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoCarbon,
                        unfocusedContainerColor = FondoCarbon,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    )
                )
                Text("¿Cuánto dejas de fondo para el próximo turno? (opcional)", color = TextoCrema, fontSize = 13.sp)
                Text(
                    "Si lo dejas vacío, el próximo turno tendrá que contar y declarar su propio fondo antes de vender.",
                    color = TextoCremaApagado,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                TextField(
                    value = fondoSiguienteTexto,
                    onValueChange = { nuevo ->
                        if (nuevo.isEmpty() || nuevo.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            fondoSiguienteTexto = nuevo
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoCarbon,
                        unfocusedContainerColor = FondoCarbon,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    )
                )
                if (estadoCorte is EstadoCorte.Error) {
                    Text(estadoCorte.mensaje, color = ColorError, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val contado = efectivoContadoTexto.toDoubleOrNull() ?: return@TextButton
                    val fondoSiguiente = fondoSiguienteTexto.toDoubleOrNull() ?: 0.0
                    onConfirmar(contado, fondoSiguiente)
                },
                enabled = efectivoContadoTexto.toDoubleOrNull() != null && estadoCorte !is EstadoCorte.Procesando
            ) {
                Text(if (estadoCorte is EstadoCorte.Procesando) "Cerrando..." else "Confirmar cierre", color = AcentoTerracota)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
