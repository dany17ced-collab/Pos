package com.tuempresa.possystem.presentation.venta

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.ProductoEntity

// Misma paleta cálida tipo boutique usada en Login y Home
private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)
private val ColorExito = Color(0xFF8FBF9F)

@Composable
fun PantallaVenta(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: VentaViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return VentaViewModel(app) as T
        }
    })

    val resultadoBusqueda by viewModel.resultadoBusqueda.collectAsState()
    val carrito by viewModel.carrito.collectAsState()
    val estadoCobro by viewModel.estadoCobro.collectAsState()

    var mostrandoEscaner by remember { mutableStateOf(false) }
    var mostrandoCheckout by remember { mutableStateOf(false) }
    var textoBusqueda by remember { mutableStateOf("") }

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) mostrandoEscaner = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        when {
            mostrandoEscaner -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    EscanerCodigoBarras(
                        modifier = Modifier.fillMaxSize(),
                        onCodigoDetectado = { codigo ->
                            mostrandoEscaner = false
                            viewModel.buscarPorCodigoBarras(codigo)
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
            }

            mostrandoCheckout -> {
                PantallaCheckout(
                    total = viewModel.calcularTotal(carrito),
                    estadoCobro = estadoCobro,
                    onConfirmar = { metodo, montoRecibido -> viewModel.confirmarCobro(metodo, montoRecibido) },
                    onVolver = {
                        mostrandoCheckout = false
                        viewModel.reiniciarEstadoCobro()
                    },
                    onFinalizar = {
                        mostrandoCheckout = false
                        viewModel.reiniciarEstadoCobro()
                    }
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    EncabezadoVenta(onVolver = onVolver)

                    BarraBusqueda(
                        texto = textoBusqueda,
                        onTextoCambiado = {
                            textoBusqueda = it
                            viewModel.buscarPorTexto(it)
                        },
                        onEscanear = {
                            if (tienePermisoCamara(context)) {
                                mostrandoEscaner = true
                            } else {
                                lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )

                    when (val resultado = resultadoBusqueda) {
                        is ResultadoBusqueda.Encontrado -> {
                            SelectorVariante(
                                variantes = resultado.variantes,
                                onAgregar = { variante, cantidad ->
                                    viewModel.agregarAlCarrito(variante, cantidad)
                                    textoBusqueda = ""
                                },
                                onCancelar = {
                                    viewModel.limpiarBusqueda()
                                    textoBusqueda = ""
                                }
                            )
                        }
                        is ResultadoBusqueda.NoEncontrado -> {
                            Text(
                                text = "No se encontró ningún producto",
                                color = ColorError,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                        is ResultadoBusqueda.Buscando -> {
                            Text(
                                text = "Buscando…",
                                color = TextoCremaApagado,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                        is ResultadoBusqueda.SinBuscar -> Unit
                    }

                    CarritoLista(
                        lineas = carrito,
                        onQuitar = { viewModel.quitarDelCarrito(it) },
                        modifier = Modifier.weight(1f)
                    )

                    PieCarrito(
                        total = viewModel.calcularTotal(carrito),
                        habilitado = carrito.isNotEmpty(),
                        onCobrar = { mostrandoCheckout = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun EncabezadoVenta(onVolver: () -> Unit) {
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
            text = "Vender",
            color = TextoCrema,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun BarraBusqueda(
    texto: String,
    onTextoCambiado: (String) -> Unit,
    onEscanear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = texto,
            onValueChange = onTextoCambiado,
            placeholder = { Text("Buscar por nombre o SKU", color = TextoCremaApagado) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FondoTarjeta,
                unfocusedContainerColor = FondoTarjeta,
                focusedTextColor = TextoCrema,
                unfocusedTextColor = TextoCrema,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AcentoTerracota
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(AcentoTerracota)
                .clickable(onClick = onEscanear),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📷", fontSize = 22.sp)
        }
    }
}

@Composable
private fun SelectorVariante(
    variantes: List<ProductoEntity>,
    onAgregar: (ProductoEntity, Int) -> Unit,
    onCancelar: () -> Unit
) {
    var varianteSeleccionada by remember { mutableStateOf<ProductoEntity?>(null) }
    var cantidadTexto by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FondoTarjeta)
            .padding(16.dp)
    ) {
        if (variantes.size == 1 && variantes.first().talla == null) {
            // Producto simple, sin variantes: se muestra directo el nombre
            val unico = variantes.first()
            Text(unico.nombre, color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            varianteSeleccionada = unico
        } else {
            Text("Elige talla / color", color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                variantes.forEach { variante ->
                    val etiqueta = listOfNotNull(variante.talla, variante.color).joinToString(" / ")
                    val seleccionada = varianteSeleccionada?.id == variante.id
                    val sinStock = variante.stockActual <= 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (seleccionada) AcentoTerracota else FondoCarbon)
                            .clickable(enabled = !sinStock) { varianteSeleccionada = variante }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (sinStock) "$etiqueta (agotado)" else etiqueta,
                            color = if (sinStock) TextoCremaApagado else TextoCrema,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        varianteSeleccionada?.let { variante ->
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cantidad:", color = TextoCremaApagado, fontSize = 14.sp)
                TextField(
                    value = cantidadTexto,
                    onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) cantidadTexto = nuevo },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(width = 80.dp, height = 52.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoCarbon,
                        unfocusedContainerColor = FondoCarbon,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Text(
                    text = "Stock: ${variante.stockActual}",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(
                    onClick = onCancelar,
                    colors = ButtonDefaults.buttonColors(containerColor = FondoCarbon),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextoCremaApagado)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = {
                        val cantidad = cantidadTexto.toIntOrNull() ?: 1
                        onAgregar(variante, cantidad)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Agregar", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CarritoLista(
    lineas: List<LineaCarrito>,
    onQuitar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lineas.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("El carrito está vacío", color = TextoCremaApagado, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(lineas, key = { it.id }) { linea ->
            LineaCarritoItem(linea = linea, onQuitar = { onQuitar(linea.id) })
        }
    }
}

@Composable
private fun LineaCarritoItem(linea: LineaCarrito, onQuitar: () -> Unit) {
    val etiquetaVariante = listOfNotNull(linea.producto.talla, linea.producto.color).joinToString(" / ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(linea.producto.nombre, color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            val detalle = buildString {
                if (etiquetaVariante.isNotBlank()) append("$etiquetaVariante · ")
                append("${linea.cantidad} × $${"%.2f".format(linea.precioUnitario)} (${linea.etiquetaEscalon})")
            }
            Text(detalle, color = TextoCremaApagado, fontSize = 12.sp)
        }
        Text(
            text = "$${"%.2f".format(linea.subtotal)}",
            color = AcentoTerracota,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "✕",
            color = ColorError,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(start = 14.dp)
                .clickable(onClick = onQuitar)
        )
    }
}

@Composable
private fun PieCarrito(total: Double, habilitado: Boolean, onCobrar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FondoTarjeta)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Total", color = TextoCremaApagado, fontSize = 12.sp)
            Text(
                "$${"%.2f".format(total)}",
                color = TextoCrema,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onCobrar,
            enabled = habilitado,
            colors = ButtonDefaults.buttonColors(
                containerColor = AcentoTerracota,
                disabledContainerColor = FondoCarbon
            )
        ) {
            Text(
                "Cobrar",
                color = if (habilitado) FondoCarbon else TextoCremaApagado,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PantallaCheckout(
    total: Double,
    estadoCobro: EstadoCobro,
    onConfirmar: (MetodoPago, Double?) -> Unit,
    onVolver: () -> Unit,
    onFinalizar: () -> Unit
) {
    var metodoSeleccionado by remember { mutableStateOf(MetodoPago.EFECTIVO) }
    var montoTexto by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "‹ Volver",
            color = TextoCrema,
            fontSize = 16.sp,
            modifier = Modifier.clickable(enabled = estadoCobro !is EstadoCobro.Procesando, onClick = onVolver)
        )

        Text(
            text = "Cobrar",
            color = TextoCrema,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "$${"%.2f".format(total)}",
            color = AcentoTerracota,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        when (estadoCobro) {
            is EstadoCobro.Exitoso -> {
                Column(
                    modifier = Modifier.padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓ Venta registrada", color = ColorExito, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    if (estadoCobro.folio != null) {
                        Text("Folio #${estadoCobro.folio}", color = TextoCremaApagado, fontSize = 14.sp)
                    }
                    Button(
                        onClick = onFinalizar,
                        colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text("Nueva venta", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> {
                Text(
                    text = "Método de pago",
                    color = TextoCremaApagado,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 28.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetodoPago.entries.forEach { metodo ->
                        val seleccionado = metodoSeleccionado == metodo
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (seleccionado) AcentoTerracota else FondoTarjeta)
                                .clickable { metodoSeleccionado = metodo }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = etiquetaMetodoPago(metodo),
                                color = if (seleccionado) FondoCarbon else TextoCrema,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (metodoSeleccionado == MetodoPago.EFECTIVO) {
                    TextField(
                        value = montoTexto,
                        onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) montoTexto = nuevo },
                        placeholder = { Text("Monto recibido", color = TextoCremaApagado) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = FondoTarjeta,
                            unfocusedContainerColor = FondoTarjeta,
                            focusedTextColor = TextoCrema,
                            unfocusedTextColor = TextoCrema,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    val recibido = montoTexto.toDoubleOrNull()
                    if (recibido != null && recibido >= total) {
                        Text(
                            text = "Cambio: $${"%.2f".format(recibido - total)}",
                            color = ColorExito,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (estadoCobro is EstadoCobro.Error) {
                    Text(
                        text = estadoCobro.mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                val montoRecibido = montoTexto.toDoubleOrNull()
                val puedeConfirmar = estadoCobro !is EstadoCobro.Procesando &&
                    (metodoSeleccionado != MetodoPago.EFECTIVO || (montoRecibido != null && montoRecibido >= total))

                Button(
                    onClick = { onConfirmar(metodoSeleccionado, montoRecibido) },
                    enabled = puedeConfirmar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcentoTerracota,
                        disabledContainerColor = FondoTarjeta
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text(
                        if (estadoCobro is EstadoCobro.Procesando) "Procesando…" else "Confirmar cobro",
                        color = if (puedeConfirmar) FondoCarbon else TextoCremaApagado,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun etiquetaMetodoPago(metodo: MetodoPago): String = when (metodo) {
    MetodoPago.EFECTIVO -> "Efectivo"
    MetodoPago.TARJETA -> "Tarjeta"
    MetodoPago.TRANSFERENCIA -> "Transferencia"
    MetodoPago.QR -> "QR"
    MetodoPago.MIXTO -> "Mixto"
}
