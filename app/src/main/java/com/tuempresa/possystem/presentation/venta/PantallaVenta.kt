@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.tuempresa.possystem.presentation.venta

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.EtiquetaPagoEntity
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.domain.boleta.DispositivoBluetooth
import com.tuempresa.possystem.presentation.caja.AperturaCajaViewModel
import com.tuempresa.possystem.presentation.caja.DialogoAperturaCaja
import com.tuempresa.possystem.presentation.caja.EstadoAperturaCaja
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.tuempresa.possystem.presentation.theme.EcoPosColors

// Misma paleta cálida tipo boutique usada en Login y Home
private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.AcentoAmbar
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito
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
    val ultimoAgregadoPorEscaneo by viewModel.ultimoAgregadoPorEscaneo.collectAsState()
    val tiendaActiva by app.sessionManager.tiendaActiva.collectAsState()

    // Antes de dejar vender, se verifica si el turno actual ya tiene un fondo
    // inicial declarado. Si no lo tiene (primer ingreso del día, o justo
    // después de un Corte Z), se exige contarlo y declararlo aquí mismo —
    // sea vendedor o admin quien esté entrando a vender.
    val aperturaCajaViewModel: AperturaCajaViewModel = viewModel(
        factory = fabricaSimple { AperturaCajaViewModel(app) }
    )
    val estadoAperturaCaja by aperturaCajaViewModel.estado.collectAsState()
    val estadoGuardadoApertura by aperturaCajaViewModel.estadoGuardado.collectAsState()

    var mostrandoEscaner by remember { mutableStateOf(false) }
    var mostrandoCheckout by remember { mutableStateOf(false) }
    var textoBusqueda by remember { mutableStateOf("") }

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) mostrandoEscaner = true
    }

    if (estadoAperturaCaja is EstadoAperturaCaja.RequiereApertura) {
        DialogoAperturaCaja(
            estadoGuardado = estadoGuardadoApertura,
            onConfirmar = { fondoInicial -> aperturaCajaViewModel.declararFondoInicial(fondoInicial) }
        )
    }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = FondoCarbon) {
        when {
            estadoAperturaCaja != EstadoAperturaCaja.Lista -> {
                // Mientras se verifica si hay fondo declarado, o mientras el
                // diálogo de apertura está abierto (arriba), no se muestra la
                // pantalla de venta: no se puede cobrar sin haber declarado
                // el efectivo inicial del turno.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (estadoAperturaCaja is EstadoAperturaCaja.RequiereApertura)
                            "Declara el fondo inicial para empezar a vender"
                        else "",
                        color = TextoCremaApagado,
                        fontSize = 14.sp
                    )
                }
            }

            mostrandoEscaner -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    EscanerCodigoBarras(
                        modifier = Modifier.fillMaxSize(),
                        modo = ModoEscaneo.CODIGO_BARRAS,
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
                    viewModel = viewModel,
                    total = viewModel.calcularTotal(carrito),
                    estadoCobro = estadoCobro,
                    onConfirmar = { metodo, montoRecibido, datosCliente ->
                        viewModel.confirmarCobro(metodo, montoRecibido, datosCliente)
                    },
                    onVolver = {
                        mostrandoCheckout = false
                        viewModel.reiniciarEstadoCobro()
                    },
                    onFinalizar = {
                        mostrandoCheckout = false
                        viewModel.reiniciarEstadoCobro()
                        viewModel.reiniciarEstadoBoleta()
                    }
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    EncabezadoVenta(
                        nombreTienda = tiendaActiva?.nombre ?: "",
                        cantidadProductos = carrito.sumOf { it.cantidad },
                        onVolver = onVolver
                    )

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

                    // Confirmación breve al escanear un código de barras
                    // INDEPENDIENTE (identifica talla/color exacta): se agregó
                    // directo al carrito, sin selector — este banner es el único
                    // feedback de que ocurrió, se autolimpia en 1.5s.
                    ultimoAgregadoPorEscaneo?.let { producto ->
                        LaunchedEffect(producto.id) {
                            kotlinx.coroutines.delay(1500)
                            viewModel.limpiarUltimoAgregadoPorEscaneo()
                        }
                        Text(
                            text = "✓ Agregado: ${producto.nombreVariante ?: producto.nombre}",
                            color = ColorExito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }

                    // El área de resultados de búsqueda (coincidencias o selector
                    // de variante) solo ocupa el espacio que su contenido
                    // necesita, con scroll propio si es largo, pero sin reservar
                    // espacio vacío cuando no hay nada que mostrar — así el
                    // carrito de abajo puede usar toda la pantalla disponible.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                    }

                    // El carrito ocupa todo el espacio vertical restante de la
                    // pantalla (antes tenía un máximo fijo de 140dp, que dejaba
                    // un hueco vacío arriba y apretaba la lista con scroll
                    // interno innecesario incluso con pocos productos).
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
private fun EncabezadoVenta(nombreTienda: String, cantidadProductos: Int, onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nueva venta",
                color = TextoCrema,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$nombreTienda · $cantidadProductos producto${if (cantidadProductos == 1) "" else "s"}",
                color = TextoCremaApagado,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FondoTarjeta)
                .clickable(onClick = onVolver),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✕", color = TextoCrema, fontSize = 16.sp)
        }
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
            placeholder = { Text("Agregar producto...", color = TextoCremaApagado) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FondoTarjeta,
                unfocusedContainerColor = FondoTarjeta,
                focusedTextColor = TextoCrema,
                unfocusedTextColor = TextoCrema,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = EcoPosColors.RosaVivo
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
            // Sin altura fija ni scroll propio: todo el bloque de arriba (búsqueda +
            // selector) ya scrollea junto como una sola pantalla (ver el
            // verticalScroll en PantallaVenta), así el teclado nunca tapa contenido.
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tallasSeleccionadas.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach { talla ->
                    val coloresDeLaTalla = variantes.filter { it.talla == talla }.mapNotNull { it.color }.distinct()
                    val coloresElegidos = coloresPorTalla[talla] ?: emptySet()

                    Text(
                        "Talla $talla",
                        color = TextoCrema,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                    )

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

                    variantes.filter { it.talla == talla && coloresElegidos.contains(it.color) }.forEach { variante ->
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
        Box(
            modifier = modifier.fillMaxWidth().heightIn(min = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("El carrito está vacío", color = TextoCremaApagado, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(lineas, key = { _, linea -> linea.id }) { indice, linea ->
            LineaCarritoItem(linea = linea, indice = indice, onQuitar = { onQuitar(linea.id) })
        }
    }
}

@Composable
private fun LineaCarritoItem(linea: LineaCarrito, indice: Int, onQuitar: () -> Unit) {
    val etiquetaVariante = listOfNotNull(linea.producto.talla, linea.producto.color).joinToString(" / ")
    val colorFranja = listOf(EcoPosColors.RosaVivo, EcoPosColors.AzulVivo, EcoPosColors.AmbarVivo, EcoPosColors.VerdeVivo)[indice % 4]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(colorFranja)
        )
        Row(
            modifier = Modifier.weight(1f).padding(14.dp),
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
                color = TextoCrema,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
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
}

@Composable
private fun OpcionTipoComprobante(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionada) AcentoTerracota else FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            color = if (seleccionada) FondoCarbon else TextoCrema,
            fontSize = 12.5.sp,
            fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal
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
                containerColor = EcoPosColors.VerdeVivo,
                disabledContainerColor = FondoCarbon
            )
        ) {
            Text(
                "Cobrar S/ ${"%.2f".format(total)}",
                color = if (habilitado) androidx.compose.ui.graphics.Color(0xFF0C2118) else TextoCremaApagado,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PantallaCheckout(
    viewModel: VentaViewModel,
    total: Double,
    estadoCobro: EstadoCobro,
    onConfirmar: (MetodoPago, Double?, DatosClienteCobro) -> Unit,
    onVolver: () -> Unit,
    onFinalizar: () -> Unit
) {
    var metodoSeleccionado by remember { mutableStateOf(MetodoPago.EFECTIVO) }
    var etiquetaSeleccionada by remember { mutableStateOf<EtiquetaPagoEntity?>(null) }
    var montoTexto by remember { mutableStateOf("") }
    val etiquetasPago by viewModel.etiquetasPago.collectAsState()

    // Preselecciona la primera etiqueta de pago disponible en cuanto cargan
    // (arrancan vacías porque la consulta a Room es asíncrona), para que el
    // botón resaltado en pantalla siempre coincida con metodoSeleccionado.
    LaunchedEffect(etiquetasPago) {
        if (etiquetaSeleccionada == null && etiquetasPago.isNotEmpty()) {
            val primera = etiquetasPago.first()
            etiquetaSeleccionada = primera
            metodoSeleccionado = viewModel.metodoPagoDeEtiqueta(primera)
        }
    }

    // Datos opcionales del cliente y elección de comprobante, capturados antes
    // de confirmar el cobro. DNI/nombre son siempre opcionales; RUC/razón
    // social solo se piden (y validan) si el cliente eligió factura.
    var tipoComprobanteSeleccionado by remember { mutableStateOf(com.tuempresa.possystem.data.local.entity.TipoComprobante.BOLETA) }
    var clienteDocumento by remember { mutableStateOf("") }
    var clienteNombre by remember { mutableStateOf("") }
    var clienteRuc by remember { mutableStateOf("") }
    var clienteRazonSocial by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "‹ Volver",
            color = TextoCrema,
            fontSize = 16.sp,
            modifier = Modifier.clickable(enabled = estadoCobro !is EstadoCobro.Procesando, onClick = onVolver)
        )

        if (estadoCobro !is EstadoCobro.Exitoso) {
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
        }

        when (estadoCobro) {
            is EstadoCobro.Exitoso -> {
                SeccionBoletaEmitida(
                    viewModel = viewModel,
                    ventaId = estadoCobro.ventaId,
                    folio = estadoCobro.folio,
                    onFinalizar = onFinalizar
                )
            }

            else -> {
                // --- Datos del cliente (opcional) y elección de comprobante ---
                Text(
                    text = "Datos del cliente (opcional)",
                    color = TextoCremaApagado,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
                TextField(
                    value = clienteDocumento,
                    onValueChange = { clienteDocumento = it },
                    placeholder = { Text("DNI del cliente", color = TextoCremaApagado) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = camposTextoColores(),
                    shape = RoundedCornerShape(12.dp)
                )
                TextField(
                    value = clienteNombre,
                    onValueChange = { clienteNombre = it },
                    placeholder = { Text("Nombre del cliente", color = TextoCremaApagado) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = camposTextoColores(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Tipo de comprobante",
                    color = TextoCrema,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpcionTipoComprobante(
                        texto = "Boleta",
                        seleccionada = tipoComprobanteSeleccionado == com.tuempresa.possystem.data.local.entity.TipoComprobante.BOLETA,
                        onClick = { tipoComprobanteSeleccionado = com.tuempresa.possystem.data.local.entity.TipoComprobante.BOLETA },
                        modifier = Modifier.weight(1f)
                    )
                    OpcionTipoComprobante(
                        texto = "Factura",
                        seleccionada = tipoComprobanteSeleccionado == com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA,
                        onClick = { tipoComprobanteSeleccionado = com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA },
                        modifier = Modifier.weight(1f)
                    )
                    OpcionTipoComprobante(
                        texto = "Nota de venta",
                        seleccionada = tipoComprobanteSeleccionado == com.tuempresa.possystem.data.local.entity.TipoComprobante.NOTA_VENTA,
                        onClick = { tipoComprobanteSeleccionado = com.tuempresa.possystem.data.local.entity.TipoComprobante.NOTA_VENTA },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (tipoComprobanteSeleccionado == com.tuempresa.possystem.data.local.entity.TipoComprobante.NOTA_VENTA) {
                    Text(
                        "Documento interno, sin validez tributaria — no se reporta a SUNAT.",
                        color = TextoCremaApagado,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (tipoComprobanteSeleccionado == com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA) {
                    TextField(
                        value = clienteRuc,
                        onValueChange = { clienteRuc = it },
                        placeholder = { Text("RUC del cliente", color = TextoCremaApagado) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = camposTextoColores(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    TextField(
                        value = clienteRazonSocial,
                        onValueChange = { clienteRazonSocial = it },
                        placeholder = { Text("Razón social", color = TextoCremaApagado) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = camposTextoColores(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text(
                    text = "Método de pago",
                    color = TextoCremaApagado,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (etiquetasPago.isNotEmpty()) {
                        etiquetasPago.forEach { etiqueta ->
                            val seleccionada = etiquetaSeleccionada?.id == etiqueta.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (seleccionada) AcentoTerracota else FondoTarjeta)
                                    .clickable {
                                        etiquetaSeleccionada = etiqueta
                                        metodoSeleccionado = viewModel.metodoPagoDeEtiqueta(etiqueta)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = etiqueta.nombre,
                                    color = if (seleccionada) FondoCarbon else TextoCrema,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        // Fallback defensivo: si por algún motivo aún no hay etiquetas
                        // cargadas (ej. base de datos recién migrada), se sigue mostrando
                        // el enum fijo para que el cobro nunca quede bloqueado.
                        MetodoPago.entries.forEach { metodo ->
                            val seleccionado = metodoSeleccionado == metodo && etiquetaSeleccionada == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (seleccionado) AcentoTerracota else FondoTarjeta)
                                    .clickable { metodoSeleccionado = metodo; etiquetaSeleccionada = null }
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
                val esFacturaSeleccionada = tipoComprobanteSeleccionado == com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA
                val facturaValida = !esFacturaSeleccionada || clienteRuc.isNotBlank()
                val puedeConfirmar = estadoCobro !is EstadoCobro.Procesando &&
                    (metodoSeleccionado != MetodoPago.EFECTIVO || (montoRecibido != null && montoRecibido >= total)) &&
                    facturaValida

                if (esFacturaSeleccionada && clienteRuc.isBlank()) {
                    Text(
                        text = "Ingresa el RUC del cliente para emitir factura.",
                        color = ColorError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Button(
                    onClick = {
                        val datosCliente = DatosClienteCobro(
                            tipoComprobante = tipoComprobanteSeleccionado,
                            documento = clienteDocumento,
                            nombre = clienteNombre,
                            ruc = clienteRuc,
                            razonSocial = clienteRazonSocial
                        )
                        onConfirmar(metodoSeleccionado, montoRecibido, datosCliente)
                    },
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

/**
 * Se muestra tras confirmar el cobro: la venta ya quedó registrada, y aquí
 * el vendedor puede compartir la boleta como PDF (A4) o imprimirla en la
 * impresora térmica de la tienda (58mm). El logo/eslogan/dirección que
 * aparecen en la boleta vienen de un ajuste único (Ajustes de boleta), no
 * se piden aquí.
 */
@Composable
private fun SeccionBoletaEmitida(
    viewModel: VentaViewModel,
    ventaId: String,
    folio: Long?,
    onFinalizar: () -> Unit
) {
    val estadoBoleta by viewModel.estadoBoleta.collectAsState()
    val context = LocalContext.current
    var mostrandoSelectorImpresora by remember { mutableStateOf(false) }
    var accionPendientePermiso by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Se guarda aparte porque, mientras se genera el PDF o se busca impresora,
    // estadoBoleta cambia a otros valores y la vista previa no debe desaparecer de pantalla.
    var datosVistaPrevia by remember { mutableStateOf<DatosBoleta?>(null) }

    // Al entrar a esta pantalla se carga automáticamente la vista previa de la
    // boleta, para que el vendedor la revise ANTES de compartirla o imprimirla.
    LaunchedEffect(ventaId) {
        viewModel.cargarVistaPrevia(ventaId)
    }

    val lanzadorPermisosBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.all { it }) {
            accionPendientePermiso?.invoke()
        }
        accionPendientePermiso = null
    }

    fun ejecutarConPermisoBluetooth(accion: () -> Unit) {
        if (tienePermisosBluetooth(context)) {
            accion()
        } else {
            accionPendientePermiso = accion
            lanzadorPermisosBluetooth.launch(permisosBluetoothNecesarios())
        }
    }

    // Guarda la vista previa apenas llega, y no la borra aunque el estado luego
    // pase a Generando/Buscando/Imprimiendo — sólo la reemplaza si llega una nueva.
    LaunchedEffect(estadoBoleta) {
        val estado = estadoBoleta
        if (estado is EstadoBoleta.VistaPreviaLista) {
            datosVistaPrevia = estado.datos
        }
    }

    // Cuando el PDF queda listo, se dispara el selector nativo de "Compartir" de Android.
    LaunchedEffect(estadoBoleta) {
        val estado = estadoBoleta
        if (estado is EstadoBoleta.PdfListo) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, estado.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir boleta"))
            viewModel.reiniciarEstadoBoleta()
        }
    }

    // Si llegan resultados de búsqueda de impresoras, se abre el selector automáticamente.
    LaunchedEffect(estadoBoleta) {
        if (estadoBoleta is EstadoBoleta.ImpresorasEncontradas || estadoBoleta is EstadoBoleta.BuscandoImpresoras) {
            mostrandoSelectorImpresora = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓ Venta registrada", color = ColorExito, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        if (folio != null) {
            Text("Folio #$folio", color = TextoCremaApagado, fontSize = 14.sp)
        }

        Text(
            "Vista previa de la boleta",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
        )

        when {
            datosVistaPrevia != null -> {
                VistaPreviaBoleta(datos = datosVistaPrevia!!)
            }
            estadoBoleta is EstadoBoleta.CargandoVistaPrevia -> {
                Text("Cargando vista previa…", color = TextoCremaApagado, fontSize = 13.sp)
            }
            estadoBoleta is EstadoBoleta.Error && datosVistaPrevia == null -> {
                Text((estadoBoleta as EstadoBoleta.Error).mensaje, color = ColorError, fontSize = 13.sp)
            }
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FondoTarjeta)
                    .clickable(enabled = estadoBoleta !is EstadoBoleta.GenerandoPdf) {
                        viewModel.generarPdfParaCompartir(ventaId)
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(
                    if (estadoBoleta is EstadoBoleta.GenerandoPdf) "Generando…" else "📄 Compartir",
                    color = TextoCrema,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FondoTarjeta)
                    .clickable(
                        enabled = estadoBoleta !is EstadoBoleta.Imprimiendo && estadoBoleta !is EstadoBoleta.BuscandoImpresoras
                    ) {
                        ejecutarConPermisoBluetooth {
                            viewModel.hayImpresoraRecordada { yaHay ->
                                if (yaHay) {
                                    viewModel.imprimirConUltimaImpresora(ventaId)
                                } else {
                                    viewModel.buscarImpresoras()
                                }
                            }
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                val textoBoton = when (estadoBoleta) {
                    is EstadoBoleta.Imprimiendo -> "Imprimiendo…"
                    is EstadoBoleta.BuscandoImpresoras -> "Buscando…"
                    else -> "🖨️ Imprimir"
                }
                Text(textoBoton, color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        when (val estado = estadoBoleta) {
            is EstadoBoleta.ImpresionExitosa -> {
                Text(
                    "✓ Boleta impresa",
                    color = ColorExito,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            is EstadoBoleta.Error -> {
                // Si no hay vista previa cargada, este mismo error ya se mostró arriba.
                if (datosVistaPrevia != null) {
                    Text(
                        estado.mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            else -> {}
        }

        Button(
            onClick = onFinalizar,
            colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Nueva venta", color = FondoCarbon, fontWeight = FontWeight.SemiBold)
        }
    }

    if (mostrandoSelectorImpresora) {
        DialogoSeleccionImpresora(
            estadoBoleta = estadoBoleta,
            onSeleccionar = { dispositivo ->
                mostrandoSelectorImpresora = false
                viewModel.detenerBusquedaImpresoras()
                viewModel.imprimirEn(ventaId, dispositivo)
            },
            onCerrar = {
                mostrandoSelectorImpresora = false
                viewModel.detenerBusquedaImpresoras()
                viewModel.reiniciarEstadoBoleta()
            }
        )
    }
}

/**
 * Dibuja la boleta como se verá al compartirla/imprimirla, para que el
 * vendedor la revise antes de emitirla de verdad. Imita el ancho angosto
 * de un ticket térmico, ya que ese es el caso más restrictivo (58mm); si
 * cabe bien aquí, cabe bien también en el PDF A4.
 */
@Composable
private fun VistaPreviaBoleta(datos: DatosBoleta) {
    val venta = datos.venta
    val configuracion = datos.configuracion
    val folioTexto = venta.folio?.toString()?.padStart(6, '0') ?: "000000"

    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFDF8F3))
            .padding(vertical = 16.dp, horizontal = 14.dp)
    ) {
        val rutaLogo = configuracion?.rutaLogo
        if (rutaLogo != null && File(rutaLogo).exists()) {
            AsyncImage(
                model = rutaLogo,
                contentDescription = "Logo de la tienda",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(48.dp)
                    .padding(bottom = 6.dp)
            )
        }

        Text(
            configuracion?.nombreTienda?.takeIf { it.isNotBlank() } ?: "Mi Tienda",
            color = Color(0xFF221B1D),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        )
        configuracion?.eslogan?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Color(0xFF5A5250),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
            )
        }
        // Descripción opcional de la tienda (qué ofrece). Solo ocupa espacio
        // si el dueño la configuró; si está vacía, no se infla nada aquí.
        configuracion?.descripcion?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Color(0xFF5A5250),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = 8.dp, end = 8.dp)
            )
        }
        configuracion?.direccion?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        configuracion?.telefono?.takeIf { it.isNotBlank() }?.let {
            Text("Tel: $it", color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        configuracion?.ruc?.takeIf { it.isNotBlank() }?.let {
            Text("RUC: $it", color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        LineaPunteadaPreview()

        val esFactura = venta.tipoComprobante == com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA
        val etiquetaComprobante = when (venta.tipoComprobante) {
            com.tuempresa.possystem.data.local.entity.TipoComprobante.FACTURA -> "Factura"
            com.tuempresa.possystem.data.local.entity.TipoComprobante.BOLETA -> "Boleta"
            com.tuempresa.possystem.data.local.entity.TipoComprobante.NOTA_VENTA -> "Nota de venta"
        }
        Text(
            "$etiquetaComprobante N° $folioTexto",
            color = Color(0xFF221B1D),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
        Text(
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(venta.fecha)),
            color = Color(0xFF7A7270),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (esFactura) {
            venta.clienteRazonSocial?.takeIf { it.isNotBlank() }?.let {
                Text("Cliente: $it", color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            venta.clienteRuc?.takeIf { it.isNotBlank() }?.let {
                Text("RUC: $it", color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        } else {
            venta.clienteNombre?.takeIf { it.isNotBlank() }?.let {
                Text("Cliente: $it", color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            venta.clienteDocumento?.takeIf { it.isNotBlank() }?.let {
                Text("DNI: $it", color = Color(0xFF7A7270), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        LineaPunteadaPreview()

        datos.detalles.forEach { linea ->
            Text(linea.nombreProducto, color = Color(0xFF221B1D), fontSize = 10.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${linea.cantidad} x S/ ${"%.2f".format(linea.precioUnitario)}",
                    color = Color(0xFF7A7270),
                    fontSize = 9.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "S/ ${"%.2f".format(linea.subtotal)}",
                    color = Color(0xFF221B1D),
                    fontSize = 10.sp
                )
            }
        }

        LineaPunteadaPreview()

        FilaTotalPreview("Subtotal", "S/ ${"%.2f".format(venta.subtotal)}")
        if (venta.descuento > 0) FilaTotalPreview("Descuento", "-S/ ${"%.2f".format(venta.descuento)}")
        if (venta.impuestos > 0) FilaTotalPreview("Impuestos", "S/ ${"%.2f".format(venta.impuestos)}")
        FilaTotalPreview("TOTAL", "S/ ${"%.2f".format(venta.total)}", resaltar = true)
        FilaTotalPreview("Pago (${etiquetaMetodoPago(venta.metodoPago)})", "S/ ${"%.2f".format(venta.montoRecibido ?: venta.total)}")
        if (venta.metodoPago == MetodoPago.EFECTIVO && venta.cambio != null && venta.cambio > 0) {
            FilaTotalPreview("Cambio", "S/ ${"%.2f".format(venta.cambio)}")
        }

        LineaPunteadaPreview()

        Text(
            configuracion?.piePagina?.takeIf { it.isNotBlank() } ?: "¡Gracias por su compra!",
            color = Color(0xFF5A5250),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        val linkRedes = configuracion?.linkRedesSociales?.takeIf { it.isNotBlank() }
        if (linkRedes != null) {
            val bitmapQrRedes = remember(linkRedes) {
                com.tuempresa.possystem.domain.boleta.GeneradorQr.generar(linkRedes, tamanoPx = 200)?.asImageBitmap()
            }
            bitmapQrRedes?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Código QR de redes sociales",
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    "Síguenos en nuestras redes",
                    color = Color(0xFF7A7270),
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
        }

        // Advertencia legal de plazo/condiciones de cambios y devoluciones.
        // Editable en Ajustes; no ocupa espacio si el dueño la deja vacía.
        // Va debajo del QR de redes, como texto normal centrado.
        configuracion?.politicaCambios?.takeIf { it.isNotBlank() }?.let {
            Text(
                "⚠️ $it",
                color = Color(0xFF8A4B3A),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 6.dp, end = 6.dp)
            )
        }

        val bitmapQrCambio = remember(venta.id) {
            com.tuempresa.possystem.domain.boleta.GeneradorQr.generar(
                "${com.tuempresa.possystem.domain.boleta.PREFIJO_QR_VENTA}${venta.id}",
                tamanoPx = 200
            )?.asImageBitmap()
        }
        bitmapQrCambio?.let { bitmap ->
            // Pequeño y en la esquina inferior izquierda: es un QR de uso
            // interno (vendedor escaneando una devolución), no algo que el
            // cliente necesite ver destacado como el de redes sociales.
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            ) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Código QR para cambios",
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    "Escanea para cambios/devoluciones",
                    color = Color(0xFF7A7270),
                    fontSize = 6.5.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun LineaPunteadaPreview() {
    Text(
        "- - - - - - - - - - - - - - - -",
        color = Color(0xFFCBBFB8),
        fontSize = 9.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun FilaTotalPreview(etiqueta: String, valor: String, resaltar: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Text(
            etiqueta,
            color = Color(0xFF221B1D),
            fontSize = if (resaltar) 12.sp else 10.sp,
            fontWeight = if (resaltar) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            valor,
            color = Color(0xFF221B1D),
            fontSize = if (resaltar) 12.sp else 10.sp,
            fontWeight = if (resaltar) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DialogoSeleccionImpresora(
    estadoBoleta: EstadoBoleta,
    onSeleccionar: (DispositivoBluetooth) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Elegir impresora") },
        text = {
            Column {
                when (estadoBoleta) {
                    is EstadoBoleta.BuscandoImpresoras -> {
                        Text(
                            "Buscando impresoras Bluetooth cercanas…",
                            color = TextoCremaApagado,
                            fontSize = 13.sp
                        )
                    }
                    is EstadoBoleta.ImpresorasEncontradas -> {
                        if (estadoBoleta.dispositivos.isEmpty()) {
                            Text(
                                "No se encontró ninguna impresora. Verifica que esté encendida y visible.",
                                color = TextoCremaApagado,
                                fontSize = 13.sp
                            )
                        } else {
                            estadoBoleta.dispositivos.forEach { dispositivo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSeleccionar(dispositivo) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    Column {
                                        Text(dispositivo.nombre, color = TextoCrema, fontSize = 14.sp)
                                        Text(
                                            if (dispositivo.yaEmparejado) "Emparejada · ${dispositivo.direccionMac}" else dispositivo.direccionMac,
                                            color = TextoCremaApagado,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is EstadoBoleta.Error -> {
                        Text(estadoBoleta.mensaje, color = ColorError, fontSize = 13.sp)
                    }
                    else -> {
                        Text("Preparando búsqueda…", color = TextoCremaApagado, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar")
            }
        }
    )
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

/** Permisos necesarios para buscar/conectar impresoras Bluetooth, según versión de Android. */
private fun permisosBluetoothNecesarios(): Array<String> {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun tienePermisosBluetooth(context: android.content.Context): Boolean {
    return permisosBluetoothNecesarios().all {
        androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
