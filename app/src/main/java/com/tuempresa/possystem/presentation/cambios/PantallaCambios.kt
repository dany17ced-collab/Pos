package com.tuempresa.possystem.presentation.cambios

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.MotivoCambio
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import com.tuempresa.possystem.presentation.venta.EscanerCodigoBarras
import com.tuempresa.possystem.presentation.venta.ModoEscaneo
import com.tuempresa.possystem.presentation.venta.tienePermisoCamara
import com.tuempresa.possystem.presentation.theme.EcoPosColors

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.LilaAzulado
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito
/**
 * Flujo de cambios y devoluciones: escanear el QR de la boleta original (o
 * buscar por folio si el cliente no la trae), elegir qué prenda se devuelve
 * y por cuál variante se cambia, indicar el motivo (siempre, para que las
 * fallas de fábrica queden registradas), y confirmar. La diferencia de precio
 * se resuelve siempre en efectivo, sin categorías de pago — es el camino más
 * simple y rápido en el mostrador.
 */
@Composable
fun PantallaCambios(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: CambioViewModel = viewModel(factory = fabricaSimple { CambioViewModel(app) })

    val resultadoBusqueda by viewModel.resultadoBusqueda.collectAsState()
    val lineaADevolver by viewModel.lineaADevolver.collectAsState()
    val variantesDisponibles by viewModel.variantesDisponibles.collectAsState()
    val varianteElegida by viewModel.varianteElegida.collectAsState()
    val motivoElegido by viewModel.motivoElegido.collectAsState()
    val estadoValidacionPlazo by viewModel.estadoValidacionPlazo.collectAsState()
    val estadoRegistro by viewModel.estadoRegistro.collectAsState()
    val estadoAnulacion by viewModel.estadoAnulacion.collectAsState()

    var mostrandoEscaner by remember { mutableStateOf(false) }
    var textoFolio by remember { mutableStateOf("") }
    var mostrandoDialogoAnular by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) mostrandoEscaner = true }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = FondoCarbon) {
        if (mostrandoEscaner) {
            Box(modifier = Modifier.fillMaxSize()) {
                EscanerCodigoBarras(
                    modifier = Modifier.fillMaxSize(),
                    modo = ModoEscaneo.QR,
                    onCodigoDetectado = { codigo ->
                        mostrandoEscaner = false
                        viewModel.procesarCodigoEscaneado(codigo)
                    }
                )
                Text(
                    text = "✕ Cerrar",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .clickable { mostrandoEscaner = false }
                )
            }
        } else {
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
                        text = "Cambios y devoluciones",
                        color = TextoCrema,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                when (val estado = resultadoBusqueda) {
                    is ResultadoBusquedaVenta.Encontrada -> {
                        SeccionVentaEncontrada(
                            venta = estado.venta,
                            detalles = estado.detalles,
                            folio = estado.venta.folio,
                            lineaADevolver = lineaADevolver,
                            variantesDisponibles = variantesDisponibles,
                            varianteElegida = varianteElegida,
                            motivoElegido = motivoElegido,
                            diferencia = viewModel.calcularDiferencia(),
                            estadoRegistro = estadoRegistro,
                            onElegirLinea = viewModel::elegirLineaADevolver,
                            onElegirVariante = viewModel::elegirNuevaVariante,
                            onElegirMotivo = viewModel::elegirMotivo,
                            onConfirmar = viewModel::confirmarCambio,
                            onCambiarVenta = {
                                viewModel.limpiarBusqueda()
                                textoFolio = ""
                            },
                            onAnularVenta = { mostrandoDialogoAnular = true }
                        )
                    }

                    is ResultadoBusquedaVenta.NoEncontrada -> {
                        SeccionBusquedaVenta(
                            textoFolio = textoFolio,
                            onTextoFolioCambiado = { textoFolio = it },
                            onBuscarFolio = { viewModel.buscarPorFolio(textoFolio) },
                            onEscanear = {
                                if (tienePermisoCamara(context)) mostrandoEscaner = true
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            }
                        )
                        Text(
                            "No se encontró ninguna venta con ese folio o código.",
                            color = ColorError,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    else -> {
                        SeccionBusquedaVenta(
                            textoFolio = textoFolio,
                            onTextoFolioCambiado = { textoFolio = it },
                            onBuscarFolio = { viewModel.buscarPorFolio(textoFolio) },
                            onEscanear = {
                                if (tienePermisoCamara(context)) mostrandoEscaner = true
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }
        }
    }

    // Si el plazo del motivo elegido ya venció, se detiene el flujo con un aviso claro.
    val validacion = estadoValidacionPlazo
    if (validacion is EstadoValidacionPlazo.PlazoVencido) {
        DialogoPlazoVencido(
            validacion = validacion,
            onCancelar = viewModel::cancelarPorPlazoVencido,
            onAutorizar = viewModel::autorizarExcepcionDePlazo
        )
    }

    if (mostrandoDialogoAnular) {
        DialogoAnularVentaEncontrada(
            estadoAnulacion = estadoAnulacion,
            onCancelar = {
                mostrandoDialogoAnular = false
                viewModel.reiniciarEstadoAnulacion()
            },
            onConfirmar = { motivo -> viewModel.anularVentaEncontrada(motivo) }
        )
    }

    // Al anular con éxito, se cierra el diálogo y se limpia la búsqueda para
    // poder atender al siguiente cliente sin arrastrar la venta ya anulada.
    LaunchedEffect(estadoAnulacion) {
        if (estadoAnulacion is EstadoAnulacionCambio.Exitosa) {
            mostrandoDialogoAnular = false
            viewModel.reiniciarEstadoAnulacion()
            viewModel.limpiarBusqueda()
            textoFolio = ""
        }
    }

    // Al registrar el cambio con éxito, se limpia todo para poder atender al siguiente cliente.
    LaunchedEffect(estadoRegistro) {
        if (estadoRegistro is EstadoRegistroCambio.Exitoso) {
            // Se deja el mensaje de éxito visible un instante antes de limpiar
            // (la UI ya muestra el check dentro de SeccionVentaEncontrada).
        }
    }
}

@Composable
private fun SeccionBusquedaVenta(
    textoFolio: String,
    onTextoFolioCambiado: (String) -> Unit,
    onBuscarFolio: () -> Unit,
    onEscanear: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Escanea el QR de la boleta del cliente",
            color = TextoCrema,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "Para hacer un cambio o anular la venta. Está impreso al final de cada boleta",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AcentoTerracota)
                .clickable(onClick = onEscanear)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("📷  Escanear boleta", color = FondoCarbon, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "¿El cliente no trae la boleta? Busca por folio:",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = textoFolio,
                    onValueChange = onTextoFolioCambiado,
                    placeholder = { Text("N° de folio", color = TextoCremaApagado) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = camposTextoColores(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FondoTarjeta)
                    .clickable(enabled = textoFolio.isNotBlank(), onClick = onBuscarFolio)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text("Buscar", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SeccionVentaEncontrada(
    venta: com.tuempresa.possystem.data.local.entity.VentaEntity,
    detalles: List<DetalleVentaEntity>,
    folio: Long?,
    lineaADevolver: DetalleVentaEntity?,
    variantesDisponibles: List<ProductoEntity>,
    varianteElegida: ProductoEntity?,
    motivoElegido: MotivoCambio?,
    diferencia: Double,
    estadoRegistro: EstadoRegistroCambio,
    onElegirLinea: (DetalleVentaEntity) -> Unit,
    onElegirVariante: (ProductoEntity) -> Unit,
    onElegirMotivo: (MotivoCambio) -> Unit,
    onConfirmar: () -> Unit,
    onCambiarVenta: () -> Unit,
    onAnularVenta: () -> Unit
) {
    if (estadoRegistro is EstadoRegistroCambio.Exitoso) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✓ Cambio registrado", color = ColorExito, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "El stock ya quedó actualizado",
                color = TextoCremaApagado,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = onCambiarVenta,
                colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Hacer otro cambio", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (folio != null) "Boleta N° ${folio.toString().padStart(6, '0')}" else "Venta encontrada",
                color = TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Cambiar",
                color = AcentoTerracota,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onCambiarVenta)
            )
        }

        if (venta.estado == EstadoVenta.ANULADA) {
            Text(
                "Esta venta ya fue anulada" + (venta.notaAnulacion?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "."),
                color = ColorError,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Text(
                "Anular esta venta",
                color = ColorError,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(onClick = onAnularVenta)
            )
        }

        Text(
            "¿Qué prenda devuelve el cliente?",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        detalles.forEach { linea ->
            val seleccionada = lineaADevolver?.id == linea.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (seleccionada) AcentoTerracota else FondoTarjeta)
                    .clickable { onElegirLinea(linea) }
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        linea.nombreProducto,
                        color = if (seleccionada) FondoCarbon else TextoCrema,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${linea.cantidad} x S/ ${"%.2f".format(linea.precioUnitario)}",
                        color = if (seleccionada) FondoCarbon.copy(alpha = 0.7f) else TextoCremaApagado,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (lineaADevolver != null) {
            Text(
                "¿Por cuál variante se cambia?",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            if (variantesDisponibles.isEmpty()) {
                Text(
                    "Este producto no tiene otras variantes disponibles.",
                    color = ColorError,
                    fontSize = 13.sp
                )
            } else {
                variantesDisponibles.forEach { variante ->
                    val seleccionada = varianteElegida?.id == variante.id
                    val nombreMostrado = listOfNotNull(
                        variante.talla?.let { "Talla $it" },
                        variante.color
                    ).joinToString(" / ").ifBlank { variante.nombre }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (seleccionada) AcentoTerracota else FondoTarjeta)
                            .clickable { onElegirVariante(variante) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                nombreMostrado,
                                color = if (seleccionada) FondoCarbon else TextoCrema,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Stock: ${variante.stockActual}",
                                color = if (seleccionada) FondoCarbon.copy(alpha = 0.7f) else TextoCremaApagado,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            "S/ ${"%.2f".format(variante.precioVenta)}",
                            color = if (seleccionada) FondoCarbon else TextoCrema,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (lineaADevolver != null && varianteElegida != null) {
            Text(
                "¿Cuál es el motivo del cambio?",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonMotivo(
                    texto = "Cambio de talla",
                    seleccionado = motivoElegido == MotivoCambio.TALLA,
                    modifier = Modifier.weight(1f),
                    onClick = { onElegirMotivo(MotivoCambio.TALLA) }
                )
                BotonMotivo(
                    texto = "Falla de fábrica",
                    seleccionado = motivoElegido == MotivoCambio.FALLA_FABRICA,
                    modifier = Modifier.weight(1f),
                    onClick = { onElegirMotivo(MotivoCambio.FALLA_FABRICA) }
                )
            }
        }

        if (lineaADevolver != null && varianteElegida != null && motivoElegido != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FondoTarjeta)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    diferencia > 0.005 -> {
                        Text("Cliente paga", color = TextoCremaApagado, fontSize = 13.sp)
                        Text(
                            "S/ ${"%.2f".format(diferencia)}",
                            color = AcentoTerracota,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    diferencia < -0.005 -> {
                        Text("Devolver al cliente", color = TextoCremaApagado, fontSize = 13.sp)
                        Text(
                            "S/ ${"%.2f".format(-diferencia)}",
                            color = ColorExito,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Text("Sin diferencia", color = TextoCrema, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (estadoRegistro is EstadoRegistroCambio.Error) {
                Text(
                    estadoRegistro.mensaje,
                    color = ColorError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Button(
                onClick = onConfirmar,
                enabled = estadoRegistro !is EstadoRegistroCambio.Guardando,
                colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    if (estadoRegistro is EstadoRegistroCambio.Guardando) "Guardando…" else "Confirmar cambio",
                    color = FondoCarbon,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BotonMotivo(
    texto: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (seleccionado) AcentoTerracota else FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            color = if (seleccionado) FondoCarbon else TextoCrema,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DialogoPlazoVencido(
    validacion: EstadoValidacionPlazo.PlazoVencido,
    onCancelar: () -> Unit,
    onAutorizar: () -> Unit
) {
    val tipoTexto = if (validacion.motivo == MotivoCambio.TALLA) "cambio de talla" else "falla de fábrica"
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Plazo vencido") },
        text = {
            Text(
                "Han pasado ${validacion.diasTranscurridos} días desde la venta. " +
                    "El plazo para $tipoTexto es de ${validacion.diasPermitidos} días. " +
                    "¿Deseas autorizar el cambio de todas formas?",
                color = TextoCremaApagado,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAutorizar) {
                Text("Autorizar de todas formas", color = AcentoTerracota, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DialogoAnularVentaEncontrada(
    estadoAnulacion: EstadoAnulacionCambio,
    onCancelar: () -> Unit,
    onConfirmar: (motivo: String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Anular esta venta?") },
        text = {
            Column {
                Text(
                    "Esto revierte el stock de todos los productos de la venta y la marca como anulada. No se puede deshacer.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    placeholder = { Text("Motivo de la anulación", color = TextoCremaApagado) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = camposTextoColores(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (estadoAnulacion is EstadoAnulacionCambio.Error) {
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
                onClick = { onConfirmar(motivo.trim().ifBlank { "Sin motivo especificado" }) },
                enabled = estadoAnulacion !is EstadoAnulacionCambio.Anulando
            ) {
                Text(
                    if (estadoAnulacion is EstadoAnulacionCambio.Anulando) "Anulando..." else "Anular venta",
                    color = ColorError,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = estadoAnulacion !is EstadoAnulacionCambio.Anulando) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun camposTextoColores() = TextFieldDefaults.colors(
    focusedContainerColor = FondoTarjeta,
    unfocusedContainerColor = FondoTarjeta,
    focusedTextColor = TextoCrema,
    unfocusedTextColor = TextoCrema,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = AcentoTerracota
)
