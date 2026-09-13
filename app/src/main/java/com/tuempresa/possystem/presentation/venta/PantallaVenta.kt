@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.tuempresa.possystem.presentation.venta

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
                        is ResultadoBusqueda.VariosResultados -> {
                            ListaCoincidencias(
                                productos = resultado.productos,
                                onSeleccionar = { viewModel.elegirProducto(it) }
                            )
                        }
                        is ResultadoBusqueda.Encontrado -> {
                            SelectorVariante(
                                viewModel = viewModel,
                                variantes = resultado.variantes,
                                onAgregar = { variante, cantidad, precio, etiqueta ->
                                    viewModel.agregarAlCarrito(variante, cantidad, precio, etiqueta)
                                    textoBusqueda = ""
                                },
                                onAgregarVarias = { lineas ->
                                    viewModel.agregarVariasAlCarrito(lineas)
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
private fun ListaCoincidencias(
    productos: List<ProductoEntity>,
    onSeleccionar: (ProductoEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            "¿Cuál de estos?",
            color = TextoCremaApagado,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        productos.forEach { producto ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FondoTarjeta)
                    .clickable { onSeleccionar(producto) }
                    .padding(14.dp)
            ) {
                Column {
                    Text(producto.nombre, color = TextoCrema, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (producto.descripcion != null) {
                        Text(producto.descripcion, color = TextoCremaApagado, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorVariante(
    viewModel: VentaViewModel,
    variantes: List<ProductoEntity>,
    onAgregar: (ProductoEntity, Int, Double, String) -> Unit,
    onAgregarVarias: (List<VentaViewModel.LineaPendiente>) -> Unit,
    onCancelar: () -> Unit
) {
    // Producto simple, sin talla/color: se mantiene el flujo directo de siempre.
    if (variantes.size == 1 && variantes.first().talla == null && variantes.first().color == null) {
        SelectorVarianteUnica(
            viewModel = viewModel,
            variante = variantes.first(),
            onAgregar = onAgregar,
            onCancelar = onCancelar
        )
        return
    }

    var modoMayorista by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FondoTarjeta)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Modo mayorista (varias tallas/colores)",
                color = TextoCrema,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = modoMayorista,
                onCheckedChange = { modoMayorista = it },
                colors = SwitchDefaults.colors(checkedTrackColor = AcentoTerracota)
            )
        }

        if (modoMayorista) {
            SelectorMayorista(
                viewModel = viewModel,
                variantes = variantes,
                onAgregarVarias = onAgregarVarias,
                onCancelar = onCancelar
            )
        } else {
            SelectorVarianteIndividual(
                viewModel = viewModel,
                variantes = variantes,
                onAgregar = onAgregar,
                onCancelar = onCancelar
            )
        }
    }
}

/** Flujo original: un producto simple sin talla ni color, directo a cantidad/precio. */
@Composable
private fun SelectorVarianteUnica(
    viewModel: VentaViewModel,
    variante: ProductoEntity,
    onAgregar: (ProductoEntity, Int, Double, String) -> Unit,
    onCancelar: () -> Unit
) {
    var cantidadTexto by remember { mutableStateOf("1") }
    var precioTexto by remember { mutableStateOf("") }
    var etiquetaEscalon by remember { mutableStateOf("Unidad") }

    LaunchedEffect(cantidadTexto) {
        val cantidad = cantidadTexto.toIntOrNull() ?: 1
        val sugerido = viewModel.calcularPrecioSugerido(variante, cantidad)
        etiquetaEscalon = viewModel.obtenerEtiquetaEscalon(variante, cantidad)
        precioTexto = "%.2f".format(sugerido)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FondoTarjeta)
            .padding(16.dp)
    ) {
        Text(variante.nombre, color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)

        FilaCantidadYPrecio(
            cantidadTexto = cantidadTexto,
            onCantidadChange = { cantidadTexto = it },
            precioTexto = precioTexto,
            onPrecioChange = { precioTexto = it },
            etiquetaEscalon = etiquetaEscalon,
            stock = variante.stockActual
        )

        BotonesCancelarAgregar(
            onCancelar = onCancelar,
            onAgregar = {
                val cantidad = cantidadTexto.toIntOrNull() ?: 1
                val precio = precioTexto.toDoubleOrNull() ?: 0.0
                onAgregar(variante, cantidad, precio, etiquetaEscalon)
            }
        )
    }
}

/** Flujo original de a una combinación por vez: color -> talla -> cantidad/precio -> Agregar. */
@Composable
private fun SelectorVarianteIndividual(
    viewModel: VentaViewModel,
    variantes: List<ProductoEntity>,
    onAgregar: (ProductoEntity, Int, Double, String) -> Unit,
    onCancelar: () -> Unit
) {
    var varianteSeleccionada by remember { mutableStateOf<ProductoEntity?>(null) }
    var cantidadTexto by remember { mutableStateOf("1") }
    var precioTexto by remember { mutableStateOf("") }
    var etiquetaEscalon by remember { mutableStateOf("Unidad") }
    var colorSeleccionado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(varianteSeleccionada, cantidadTexto) {
        val variante = varianteSeleccionada ?: return@LaunchedEffect
        val cantidad = cantidadTexto.toIntOrNull() ?: 1
        val sugerido = viewModel.calcularPrecioSugerido(variante, cantidad)
        etiquetaEscalon = viewModel.obtenerEtiquetaEscalon(variante, cantidad)
        precioTexto = "%.2f".format(sugerido)
    }

    val coloresDisponibles = variantes.mapNotNull { it.color }.distinct()

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text("Elige color", color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            coloresDisponibles.forEach { color ->
                ChipSeleccionable(
                    texto = color,
                    seleccionado = colorSeleccionado == color,
                    onClick = {
                        colorSeleccionado = color
                        varianteSeleccionada = null
                    }
                )
            }
        }

        if (colorSeleccionado != null) {
            Text(
                "Elige talla",
                color = TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp)
            )
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                variantes.filter { it.color == colorSeleccionado }.forEach { variante ->
                    val sinStock = variante.stockActual <= 0
                    ChipSeleccionable(
                        texto = if (sinStock) "Talla ${variante.talla} (agotado)" else "Talla ${variante.talla}",
                        seleccionado = varianteSeleccionada?.id == variante.id,
                        habilitado = !sinStock,
                        onClick = { varianteSeleccionada = variante }
                    )
                }
            }
        }
    }

    varianteSeleccionada?.let { variante ->
        FilaCantidadYPrecio(
            cantidadTexto = cantidadTexto,
            onCantidadChange = { cantidadTexto = it },
            precioTexto = precioTexto,
            onPrecioChange = { precioTexto = it },
            etiquetaEscalon = etiquetaEscalon,
            stock = variante.stockActual
        )

        BotonesCancelarAgregar(
            onCancelar = onCancelar,
            onAgregar = {
                val cantidad = cantidadTexto.toIntOrNull() ?: 1
                val precio = precioTexto.toDoubleOrNull() ?: 0.0
                onAgregar(variante, cantidad, precio, etiquetaEscalon)
            }
        )
    }
}

/**
 * Modo mayorista: el vendedor elige una o varias tallas; por cada talla elegida
 * se despliegan todos los colores disponibles en esa talla, cada uno con su
 * propio campo de cantidad (0 = no se agrega) y precio editable. Al confirmar,
 * todas las líneas con cantidad > 0 se agregan al carrito de una sola vez.
/**
 * Modo mayorista: el vendedor elige una o varias tallas; por cada talla elegida
 * primero elige QUÉ colores participan en esa talla para este pedido (no todos
 * los colores cargados aplican siempre), y recién ahí aparece una fila de
 * cantidad/precio por cada color elegido. Al confirmar, todas las líneas con
 * cantidad > 0 se agregan al carrito de una sola vez.
 */
@Composable
private fun SelectorMayorista(
    viewModel: VentaViewModel,
    variantes: List<ProductoEntity>,
    onAgregarVarias: (List<VentaViewModel.LineaPendiente>) -> Unit,
    onCancelar: () -> Unit
) {
    val tallasDisponibles = variantes.mapNotNull { it.talla }.distinct()
        .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
    var tallasSeleccionadas by remember { mutableStateOf(setOf<String>()) }

    // Colores elegidos para participar, por talla: clave = talla, valor = set de colores.
    var coloresPorTalla by remember { mutableStateOf(mapOf<String, Set<String>>()) }

    // input de cada variante: clave = variante.id
    var cantidades by remember { mutableStateOf(mapOf<String, String>()) }
    var precios by remember { mutableStateOf(mapOf<String, String>()) }
    var etiquetas by remember { mutableStateOf(mapOf<String, String>()) }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text("Elige una o varias tallas", color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tallasDisponibles.forEach { talla ->
                val hayStock = variantes.any { it.talla == talla && it.stockActual > 0 }
                ChipSeleccionable(
                    texto = "Talla $talla",
                    seleccionado = tallasSeleccionadas.contains(talla),
                    habilitado = hayStock,
                    onClick = {
                        tallasSeleccionadas = if (tallasSeleccionadas.contains(talla)) {
                            tallasSeleccionadas - talla
                        } else {
                            tallasSeleccionadas + talla
                        }
                    }
                )
            }
        }

        val lineasParaAgregar = tallasSeleccionadas
            .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
            .flatMap { talla -> variantes.filter { it.talla == talla } }
            .mapNotNull { variante ->
                val cantidad = cantidades[variante.id]?.toIntOrNull() ?: 0
                val precio = precios[variante.id]?.toDoubleOrNull()
                if (cantidad > 0 && precio != null) {
                    VentaViewModel.LineaPendiente(
                        variante = variante,
                        cantidad = cantidad,
                        precioUnitario = precio,
                        etiquetaEscalon = etiquetas[variante.id] ?: "Unidad"
                    )
                } else null
            }

        if (tallasSeleccionadas.isNotEmpty()) {
            // Altura acotada con scroll propio: si hay varias tallas con varios
            // colores cada una, la lista completa no cabría en pantalla junto
            // con el carrito de abajo.
            LazyColumn(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tallasSeleccionadas.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach { talla ->
                    val coloresDeLaTalla = variantes.filter { it.talla == talla }.mapNotNull { it.color }.distinct()
                    val coloresElegidos = coloresPorTalla[talla] ?: emptySet()

                    item(key = "encabezado_$talla") {
                        Text(
                            "Talla $talla",
                            color = TextoCrema,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                        )
                    }

                    item(key = "colores_$talla") {
                        Column {
                            Text(
                                "¿Qué colores lleva en esta talla?",
                                color = TextoCremaApagado,
                                fontSize = 12.sp
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                coloresDeLaTalla.forEach { color ->
                                    val variante = variantes.find { it.talla == talla && it.color == color }
                                    val sinStock = (variante?.stockActual ?: 0) <= 0
                                    ChipSeleccionable(
                                        texto = color,
                                        seleccionado = coloresElegidos.contains(color),
                                        habilitado = !sinStock,
                                        onClick = {
                                            val actuales = coloresPorTalla[talla] ?: emptySet()
                                            coloresPorTalla = coloresPorTalla + (talla to
                                                if (actuales.contains(color)) actuales - color else actuales + color)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    items(
                        variantes.filter { it.talla == talla && coloresElegidos.contains(it.color) },
                        key = { it.id }
                    ) { variante ->
                        FilaVarianteMayorista(
                            variante = variante,
                            cantidadTexto = cantidades[variante.id] ?: "",
                            precioTexto = precios[variante.id] ?: "",
                            etiquetaEscalon = etiquetas[variante.id] ?: "Unidad",
                            onCantidadChange = { nuevo ->
                                cantidades = cantidades + (variante.id to nuevo)
                                val cantidad = nuevo.toIntOrNull()
                                if (cantidad != null && cantidad > 0) {
                                    coroutineScope.launch {
                                        val sugerido = viewModel.calcularPrecioSugerido(variante, cantidad)
                                        val etiqueta = viewModel.obtenerEtiquetaEscalon(variante, cantidad)
                                        precios = precios + (variante.id to "%.2f".format(sugerido))
                                        etiquetas = etiquetas + (variante.id to etiqueta)
                                    }
                                }
                            },
                            onPrecioChange = { precios = precios + (variante.id to it) }
                        )
                    }
                }
            }

            val totalUnidades = lineasParaAgregar.sumOf { it.cantidad }
            val totalMonto = lineasParaAgregar.sumOf { it.cantidad * it.precioUnitario }

            if (lineasParaAgregar.isNotEmpty()) {
                Text(
                    "$totalUnidades unidades · S/ ${"%.2f".format(totalMonto)}",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            Row(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = onCancelar,
                    colors = ButtonDefaults.buttonColors(containerColor = FondoCarbon),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextoCremaApagado)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = { onAgregarVarias(lineasParaAgregar) },
                    enabled = lineasParaAgregar.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Agregar todo (${lineasParaAgregar.size})",
                        color = FondoCarbon,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaVarianteMayorista(
    variante: ProductoEntity,
    cantidadTexto: String,
    precioTexto: String,
    etiquetaEscalon: String,
    onCantidadChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit
) {
    val sinStock = variante.stockActual <= 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FondoCarbon)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = variante.color ?: "Sin color",
            color = if (sinStock) TextoCremaApagado else TextoCrema,
            fontSize = 13.sp
        )
        Text(
            text = if (sinStock) "Agotado" else "Stock: ${variante.stockActual} · $etiquetaEscalon",
            color = TextoCremaApagado,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cant.", color = TextoCremaApagado, fontSize = 12.sp)
            TextField(
                value = cantidadTexto,
                onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) onCantidadChange(nuevo) },
                enabled = !sinStock,
                placeholder = { Text("0", color = TextoCremaApagado) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
                    .height(56.dp),
                colors = camposTextoColores(),
                shape = RoundedCornerShape(8.dp)
            )
            Text(
                "S/",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
            TextField(
                value = precioTexto,
                onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) onPrecioChange(nuevo) },
                enabled = !sinStock,
                placeholder = { Text("0.00", color = TextoCremaApagado) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
                    .height(56.dp),
                colors = camposTextoColores(),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun ChipSeleccionable(
    texto: String,
    seleccionado: Boolean,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionado) AcentoTerracota else FondoCarbon)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = texto,
            color = if (!habilitado) TextoCremaApagado else if (seleccionado) FondoCarbon else TextoCrema,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FilaCantidadYPrecio(
    cantidadTexto: String,
    onCantidadChange: (String) -> Unit,
    precioTexto: String,
    onPrecioChange: (String) -> Unit,
    etiquetaEscalon: String,
    stock: Int
) {
    Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Cantidad:", color = TextoCremaApagado, fontSize = 14.sp)
        TextField(
            value = cantidadTexto,
            onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) onCantidadChange(nuevo) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .padding(start = 12.dp)
                .size(width = 80.dp, height = 52.dp),
            colors = camposTextoColores(),
            shape = RoundedCornerShape(10.dp)
        )
        Text(
            text = "Stock: $stock",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }

    Row(
        modifier = Modifier.padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Precio (${etiquetaEscalon}):", color = TextoCremaApagado, fontSize = 14.sp)
        TextField(
            value = precioTexto,
            onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) onPrecioChange(nuevo) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .padding(start = 12.dp)
                .size(width = 100.dp, height = 52.dp),
            colors = camposTextoColores(),
            shape = RoundedCornerShape(10.dp)
        )
        Text(
            text = "editable",
            color = TextoCremaApagado,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun BotonesCancelarAgregar(onCancelar: () -> Unit, onAgregar: () -> Unit) {
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
            onClick = onAgregar,
            colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
            modifier = Modifier.weight(1f)
        ) {
            Text("Agregar", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
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
                append("${linea.cantidad} × S/ ${"%.2f".format(linea.precioUnitario)} (${linea.etiquetaEscalon})")
            }
            Text(detalle, color = TextoCremaApagado, fontSize = 12.sp)
        }
        Text(
            text = "S/ ${"%.2f".format(linea.subtotal)}",
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
                "S/ ${"%.2f".format(total)}",
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
            text = "S/ ${"%.2f".format(total)}",
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
                            text = "Cambio: S/ ${"%.2f".format(recibido - total)}",
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

@Composable
private fun camposTextoColores() = TextFieldDefaults.colors(
    focusedContainerColor = FondoCarbon,
    unfocusedContainerColor = FondoCarbon,
    focusedTextColor = TextoCrema,
    unfocusedTextColor = TextoCrema,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = AcentoTerracota
)

private fun etiquetaMetodoPago(metodo: MetodoPago): String = when (metodo) {
    MetodoPago.EFECTIVO -> "Efectivo"
    MetodoPago.TARJETA -> "Tarjeta"
    MetodoPago.TRANSFERENCIA -> "Transferencia"
    MetodoPago.QR -> "QR"
    MetodoPago.MIXTO -> "Mixto"
}
