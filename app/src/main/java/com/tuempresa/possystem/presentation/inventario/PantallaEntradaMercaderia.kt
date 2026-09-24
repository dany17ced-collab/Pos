package com.tuempresa.possystem.presentation.inventario

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.presentation.venta.EscanerCodigoBarras
import com.tuempresa.possystem.presentation.venta.tienePermisoCamara
import com.tuempresa.possystem.presentation.theme.EcoPosColors

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.AcentoAmbar
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito
/**
 * Pantalla para registrar entradas de mercadería (restock) sobre productos que
 * YA EXISTEN en el inventario: busca el producto padre (por texto o escaneando
 * su código de barras) y permite sumar cantidad a cada talla/color existente.
 *
 * Si llega una talla o color NUEVO que aún no está dado de alta, esta pantalla
 * no lo crea — hay que ir a "Nuevo producto" para definirlo con su precio.
 */
@Composable
fun PantallaEntradaMercaderia(
    app: POSApplication,
    onVolver: () -> Unit,
    onIrANuevoProducto: () -> Unit
) {
    val viewModel: EntradaMercaderiaViewModel = viewModel(factory = fabricaSimple { EntradaMercaderiaViewModel(app) })

    val resultadosBusqueda by viewModel.resultadosBusqueda.collectAsState()
    val estadoBusqueda by viewModel.estadoBusqueda.collectAsState()
    val cantidadesPorVariante by viewModel.cantidadesPorVariante.collectAsState()
    val estadoRegistro by viewModel.estadoRegistro.collectAsState()
    val mostrandoAgregarColor by viewModel.mostrandoAgregarColor.collectAsState()
    val estadoAgregarColor by viewModel.estadoAgregarColor.collectAsState()

    var textoBusqueda by remember { mutableStateOf("") }
    var mostrandoEscaner by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }
    // Cuando el escáner se abre desde un campo de código por talla (dentro de
    // PantallaAgregarColorEnEntrada, para un producto en modo INDEPENDIENTE),
    // este callback recibe el código leído en vez de ir a buscarPorCodigoBarras.
    var callbackEscaneoTalla by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) mostrandoEscaner = true }

    if (estadoRegistro is EstadoRegistroEntrada.Exitoso) {
        onVolver()
        return
    }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = FondoCarbon) {
        val estadoActual = estadoBusqueda
        if (mostrandoAgregarColor && estadoActual is EstadoBusquedaProducto.Encontrado) {
            PantallaAgregarColorEnEntrada(
                nombreProducto = estadoActual.padre.nombre,
                tallasDisponibles = viewModel.tallasExistentesDelProducto(),
                modoCodigoBarras = viewModel.modoCodigoBarrasDelProducto(),
                onEscanearParaTalla = { callback ->
                    callbackEscaneoTalla = callback
                    if (tienePermisoCamara(context)) mostrandoEscaner = true
                    else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                },
                estadoGuardado = estadoAgregarColor,
                onGuardar = { color, stockPorTalla -> viewModel.agregarColorNuevo(color, stockPorTalla) },
                onTerminar = { viewModel.cerrarAgregarColor(); viewModel.reiniciarEstadoAgregarColor() },
                onLimpiarEstado = { viewModel.reiniciarEstadoAgregarColor() }
            )
        } else if (mostrandoEscaner) {
            Box(modifier = Modifier.fillMaxSize()) {
                EscanerCodigoBarras(
                    modifier = Modifier.fillMaxSize(),
                    modo = com.tuempresa.possystem.presentation.venta.ModoEscaneo.CODIGO_BARRAS,
                    onCodigoDetectado = { codigo ->
                        mostrandoEscaner = false
                        val callback = callbackEscaneoTalla
                        if (callback != null) {
                            callback(codigo)
                            callbackEscaneoTalla = null
                        } else {
                            viewModel.buscarPorCodigoBarras(codigo)
                        }
                    }
                )
                Text(
                    text = "✕ Cerrar",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .clickable {
                            mostrandoEscaner = false
                            callbackEscaneoTalla = null
                        }
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
                        text = "Entrada de mercadería",
                        color = TextoCrema,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                val estado = estadoBusqueda
                when (estado) {
                    is EstadoBusquedaProducto.Encontrado -> {
                        SeccionVariantesEncontradas(
                            padre = estado.padre,
                            variantes = estado.variantes,
                            cantidades = cantidadesPorVariante,
                            onCantidadCambiada = viewModel::actualizarCantidad,
                            onCambiarProducto = { viewModel.limpiarBusqueda(); textoBusqueda = "" },
                            onAgregarColorNuevo = { viewModel.abrirAgregarColor() },
                            onIrANuevoProducto = onIrANuevoProducto
                        )

                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            CampoTextoEntrada(
                                valor = motivo,
                                onCambio = { motivo = it },
                                placeholder = "Motivo (opcional, ej. \"Compra proveedor Lima\")"
                            )

                            if (estadoRegistro is EstadoRegistroEntrada.Error) {
                                Text(
                                    text = (estadoRegistro as EstadoRegistroEntrada.Error).mensaje,
                                    color = ColorError,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            val puedeGuardar = viewModel.hayCantidadesValidas() &&
                                estadoRegistro !is EstadoRegistroEntrada.Guardando

                            Button(
                                onClick = { viewModel.registrarEntrada(motivo) },
                                enabled = puedeGuardar,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcentoTerracota,
                                    disabledContainerColor = FondoTarjeta
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Text(
                                    if (estadoRegistro is EstadoRegistroEntrada.Guardando) "Guardando…" else "Registrar entrada",
                                    color = if (puedeGuardar) FondoCarbon else TextoCremaApagado,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    is EstadoBusquedaProducto.NoEncontrado -> {
                        SeccionBusqueda(
                            textoBusqueda = textoBusqueda,
                            onTextoBusquedaCambiado = {
                                textoBusqueda = it
                                viewModel.buscarPorTexto(it)
                            },
                            onEscanear = {
                                if (tienePermisoCamara(context)) mostrandoEscaner = true
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            },
                            resultados = resultadosBusqueda,
                            onSeleccionarProducto = viewModel::seleccionarProducto
                        )
                        Text(
                            "No se encontró ningún producto con ese código de barras.",
                            color = ColorError,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FondoTarjeta)
                                .clickable(onClick = onIrANuevoProducto)
                                .padding(14.dp)
                        ) {
                            Text("Crear como producto nuevo →", color = AcentoTerracota, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    else -> {
                        SeccionBusqueda(
                            textoBusqueda = textoBusqueda,
                            onTextoBusquedaCambiado = {
                                textoBusqueda = it
                                viewModel.buscarPorTexto(it)
                            },
                            onEscanear = {
                                if (tienePermisoCamara(context)) mostrandoEscaner = true
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            },
                            resultados = resultadosBusqueda,
                            onSeleccionarProducto = viewModel::seleccionarProducto
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionBusqueda(
    textoBusqueda: String,
    onTextoBusquedaCambiado: (String) -> Unit,
    onEscanear: () -> Unit,
    resultados: List<ProductoEntity>,
    onSeleccionarProducto: (ProductoEntity) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Busca el producto al que le llegó mercadería",
            color = TextoCremaApagado,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CampoTextoEntrada(
                    valor = textoBusqueda,
                    onCambio = onTextoBusquedaCambiado,
                    placeholder = "Nombre, SKU o código de barras"
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AcentoTerracota)
                    .clickable(onClick = onEscanear),
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 22.sp)
            }
        }

        if (resultados.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                resultados.forEach { producto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FondoTarjeta)
                            .clickable { onSeleccionarProducto(producto) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(producto.nombre, color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("SKU: ${producto.sku}", color = TextoCremaApagado, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else if (textoBusqueda.isNotBlank()) {
            Text(
                "Sin resultados para \"$textoBusqueda\"",
                color = TextoCremaApagado,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun SeccionVariantesEncontradas(
    padre: ProductoEntity,
    variantes: List<ProductoEntity>,
    cantidades: Map<String, String>,
    onCantidadCambiada: (String, String) -> Unit,
    onCambiarProducto: () -> Unit,
    onAgregarColorNuevo: () -> Unit,
    onIrANuevoProducto: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(padre.nombre, color = TextoCrema, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("SKU: ${padre.sku}", color = TextoCremaApagado, fontSize = 12.sp)
            }
            Text(
                "Cambiar",
                color = AcentoTerracota,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onCambiarProducto)
            )
        }

        if (variantes.isEmpty()) {
            Text(
                "Este producto aún no tiene tallas ni colores registrados.",
                color = TextoCremaApagado,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            Text(
                "Ingresa la cantidad que llegó de cada talla/color",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            variantes.forEach { variante ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FondoTarjeta)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            variante.nombreVariante ?: "Talla ${variante.talla} / ${variante.color}",
                            color = TextoCrema,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text("Stock actual: ${variante.stockActual}", color = TextoCremaApagado, fontSize = 12.sp)
                    }
                    TextField(
                        value = cantidades[variante.id] ?: "",
                        onValueChange = { onCantidadCambiada(variante.id, it) },
                        placeholder = { Text("0", color = TextoCremaApagado) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(80.dp)
                            .height(56.dp),
                        colors = camposTextoColores(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(FondoTarjeta)
                .clickable(onClick = onAgregarColorNuevo)
                .padding(14.dp)
        ) {
            Text(
                "+ Agregar color nuevo (mismo precio)",
                color = AcentoTerracota,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onIrANuevoProducto)
                .padding(vertical = 8.dp)
        ) {
            Text(
                "¿Llegó una talla totalmente nueva o precio distinto? Créalo en Nuevo producto →",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Agrega un color nuevo a un producto ya existente: solo pide el nombre del
 * color y el stock de cada talla ya usada por ese producto. El precio de cada
 * talla se copia automáticamente de una variante existente de esa misma talla
 * — por eso aquí no se pide precio en ningún momento.
 */
@Composable
private fun PantallaAgregarColorEnEntrada(
    nombreProducto: String,
    tallasDisponibles: List<String>,
    modoCodigoBarras: ModoCodigoBarras,
    onEscanearParaTalla: ((String) -> Unit) -> Unit,
    estadoGuardado: EstadoRegistroEntrada,
    onGuardar: (String, Map<String, StockTallaEnCaptura>) -> Unit,
    onTerminar: () -> Unit,
    onLimpiarEstado: () -> Unit
) {
    var color by remember { mutableStateOf("") }
    var tallasMarcadas by remember { mutableStateOf(setOf<String>()) }
    var stockPorTalla by remember { mutableStateOf(mapOf<String, String>()) }
    var codigosPorTalla by remember { mutableStateOf(mapOf<String, String>()) }
    var coloresAgregados by remember { mutableStateOf(0) }
    var ultimoColorAgregado by remember { mutableStateOf<String?>(null) }

    // Cuando la entrada se guarda con éxito, el panel se queda abierto: se
    // limpian los campos para el siguiente color, se suma al contador, y se
    // avisa al ViewModel (onLimpiarEstado) para que vuelva a Inactivo y este
    // efecto no se repita en la siguiente recomposición.
    LaunchedEffect(estadoGuardado) {
        if (estadoGuardado is EstadoRegistroEntrada.Exitoso) {
            ultimoColorAgregado = color.trim()
            coloresAgregados += 1
            color = ""
            tallasMarcadas = emptySet()
            stockPorTalla = emptyMap()
            codigosPorTalla = emptyMap()
            onLimpiarEstado()
        }
    }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
     ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹ Cancelar",
                    color = TextoCrema,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f).clickable(onClick = onTerminar)
                )
                if (coloresAgregados > 0) {
                    Text(
                        text = "Terminar ✓",
                        color = AcentoTerracota,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onTerminar)
                    )
                }
            }
            Text(
                text = "Color nuevo · $nombreProducto",
                color = TextoCrema,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            if (coloresAgregados > 0) {
                Text(
                    "✓ $coloresAgregados color${if (coloresAgregados == 1) "" else "es"} agregado${if (coloresAgregados == 1) "" else "s"}" +
                        (ultimoColorAgregado?.let { " · último: $it" } ?: ""),
                    color = ColorExito,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                "El precio de cada talla se copia del que ya tiene este producto",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            CampoTextoEntrada(valor = color, onCambio = { color = it }, placeholder = "Nombre del color (ej. Verde)")

            if (tallasDisponibles.isEmpty()) {
                Text(
                    "Este producto aún no tiene ninguna talla con precio definido. Créalo primero en \"Nuevo producto\".",
                    color = ColorError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                Text(
                    "Tallas — marca las que llegaron en este color",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                tallasDisponibles.forEach { talla ->
                    val marcada = talla in tallasMarcadas
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (marcada) FondoTarjeta else Color.Transparent)
                            .padding(if (marcada) 12.dp else 0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = marcada,
                                onCheckedChange = { activo ->
                                    tallasMarcadas = if (activo) tallasMarcadas + talla else tallasMarcadas - talla
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AcentoTerracota, uncheckedColor = TextoCremaApagado)
                            )
                            Text("Talla $talla", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        if (marcada) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp, start = 40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stock:", color = TextoCremaApagado, fontSize = 13.sp)
                                TextField(
                                    value = stockPorTalla[talla] ?: "",
                                    onValueChange = { nuevo ->
                                        if (nuevo.all { it.isDigit() }) {
                                            stockPorTalla = stockPorTalla + (talla to nuevo)
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .width(90.dp)
                                        .height(56.dp),
                                    colors = camposTextoColores(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            if (modoCodigoBarras == ModoCodigoBarras.INDEPENDIENTE) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 40.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CampoTextoEntrada(
                                            valor = codigosPorTalla[talla] ?: "",
                                            onCambio = { nuevo -> codigosPorTalla = codigosPorTalla + (talla to nuevo) },
                                            placeholder = "Código de barras de esta talla"
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AcentoTerracota)
                                            .clickable {
                                                onEscanearParaTalla { codigo ->
                                                    codigosPorTalla = codigosPorTalla + (talla to codigo)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📷", fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (estadoGuardado is EstadoRegistroEntrada.Error) {
                Text(
                    text = estadoGuardado.mensaje,
                    color = ColorError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            val puedeGuardar = color.isNotBlank() &&
                tallasMarcadas.isNotEmpty() &&
                estadoGuardado !is EstadoRegistroEntrada.Guardando &&
                (modoCodigoBarras == ModoCodigoBarras.COMPARTIDO ||
                    tallasMarcadas.all { !codigosPorTalla[it].isNullOrBlank() })

            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val mapaFinal = tallasMarcadas.associateWith { talla ->
                            StockTallaEnCaptura(
                                talla = talla,
                                stockTexto = stockPorTalla[talla] ?: "",
                                codigoBarrasTexto = codigosPorTalla[talla] ?: ""
                            )
                        }
                        onGuardar(color, mapaFinal)
                    },
                    enabled = puedeGuardar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcentoTerracota,
                        disabledContainerColor = FondoTarjeta
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (estadoGuardado is EstadoRegistroEntrada.Guardando) "Guardando…" else "Agregar y seguir",
                        color = if (puedeGuardar) FondoCarbon else TextoCremaApagado,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                if (coloresAgregados > 0) {
                    Button(
                        onClick = onTerminar,
                        colors = ButtonDefaults.buttonColors(containerColor = FondoTarjeta),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Terminar",
                            color = TextoCrema,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoTextoEntrada(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String
) {
    TextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text(placeholder, color = TextoCremaApagado) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = camposTextoColores(),
        shape = RoundedCornerShape(12.dp)
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
