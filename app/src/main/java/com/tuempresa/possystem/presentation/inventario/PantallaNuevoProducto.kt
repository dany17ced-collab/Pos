package com.tuempresa.possystem.presentation.inventario

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import com.tuempresa.possystem.presentation.venta.EscanerCodigoBarras
import com.tuempresa.possystem.presentation.venta.tienePermisoCamara

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoRosa = EcoPosColors.RojoSalmon
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito

/**
 * Alta de producto nuevo — UNA sola pantalla de scroll continuo, sin
 * pestañas: datos generales, modo de código de barras, tallas con su precio
 * (checkbox que despliega el campo de precio in-line), y colores como filas
 * expandibles con su stock y —si el modo es independiente— su propio código
 * de barras por talla. Todo visible y editable sin saltar de vista.
 */
@Composable
fun PantallaNuevoProducto(app: POSApplication, onVolver: () -> Unit, onGuardado: () -> Unit) {
    val viewModel: NuevoProductoViewModel = viewModel(factory = fabricaSimple { NuevoProductoViewModel(app) })

    val nombre by viewModel.nombre.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val codigoBarras by viewModel.codigoBarras.collectAsState()
    val modoCodigoBarras by viewModel.modoCodigoBarras.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaId by viewModel.categoriaId.collectAsState()
    val precioCompraTexto by viewModel.precioCompraTexto.collectAsState()
    val tallas by viewModel.tallas.collectAsState()
    val colores by viewModel.colores.collectAsState()
    val estadoGuardado by viewModel.estadoGuardado.collectAsState()

    var mostrandoEscaner by remember { mutableStateOf(false) }
    // Cuando el escáner se abre desde un campo de código por talla (modo
    // INDEPENDIENTE, dentro de una fila de color), este callback recibe el
    // código leído en vez de ir a viewModel.actualizarCodigoBarras.
    var callbackEscaneoTalla by remember { mutableStateOf<((String) -> Unit)?>(null) }
    // Índice del color cuya fila está expandida para editar (null = ninguna,
    // o se está creando uno nuevo con colorNuevoAbierto = true).
    var colorNuevoAbierto by remember { mutableStateOf(false) }
    var colorNombreNuevo by remember { mutableStateOf("") }
    var stockPorTallaNuevo by remember { mutableStateOf<Map<String, StockTallaEnCaptura>>(emptyMap()) }
    var coloresAgregadosSesion by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) mostrandoEscaner = true }

    val tallasMarcadasKey = remember(tallas.keys.toList().sorted()) { tallas.keys.toList().sorted() }
    androidx.compose.runtime.LaunchedEffect(tallasMarcadasKey) {
        val tallasOrdenadas = viewModel.tallasMarcadasOrdenadas().map { it.talla }
        stockPorTallaNuevo = tallasOrdenadas.associateWith { t ->
            stockPorTallaNuevo[t] ?: StockTallaEnCaptura(talla = t)
        }
    }

    if (estadoGuardado is EstadoGuardado.Exitoso) {
        onGuardado()
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        if (mostrandoEscaner) {
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
                            viewModel.actualizarCodigoBarras(codigo)
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
            return@Surface
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹ Cancelar",
                        color = TextoCrema,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable(onClick = onVolver)
                    )
                }
                Text(
                    "Nuevo producto",
                    color = TextoCrema,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
                )
            }

            // ---- Datos generales ----
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CampoTexto(valor = nombre, onCambio = viewModel::actualizarNombre, placeholder = "Nombre del producto")
                    Spacer(modifier = Modifier.height(10.dp))
                    CampoTexto(valor = descripcion, onCambio = viewModel::actualizarDescripcion, placeholder = "Descripción / tipo de tela")
                    Spacer(modifier = Modifier.height(10.dp))
                    CampoTexto(
                        valor = precioCompraTexto,
                        onCambio = viewModel::actualizarPrecioCompra,
                        placeholder = "Precio de compra",
                        tipoTeclado = KeyboardType.Decimal
                    )

                    if (categorias.isNotEmpty()) {
                        Text("Categoría", color = TextoCremaApagado, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categorias.forEach { categoria ->
                                val seleccionada = categoriaId == categoria.id
                                ChipSeleccionable(
                                    texto = categoria.nombre,
                                    seleccionada = seleccionada,
                                    onClick = { viewModel.seleccionarCategoria(if (seleccionada) null else categoria.id) }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Código de barras: modo compartido vs independiente ----
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Código de barras", color = TextoCremaApagado, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OpcionModoCodigoBarras(
                            texto = "Uno solo para todo",
                            seleccionada = modoCodigoBarras == ModoCodigoBarras.COMPARTIDO,
                            onClick = { viewModel.seleccionarModoCodigoBarras(ModoCodigoBarras.COMPARTIDO) },
                            modifier = Modifier.weight(1f)
                        )
                        OpcionModoCodigoBarras(
                            texto = "Uno por cada talla/color",
                            seleccionada = modoCodigoBarras == ModoCodigoBarras.INDEPENDIENTE,
                            onClick = { viewModel.seleccionarModoCodigoBarras(ModoCodigoBarras.INDEPENDIENTE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (modoCodigoBarras == ModoCodigoBarras.COMPARTIDO) {
                        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                CampoTexto(valor = codigoBarras, onCambio = viewModel::actualizarCodigoBarras, placeholder = "Código de barras")
                            }
                            BotonEscanear(modifier = Modifier.padding(start = 10.dp)) {
                                if (tienePermisoCamara(context)) mostrandoEscaner = true
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            }
                        }
                    } else {
                        Text(
                            "Cada talla de cada color pedirá su propio código más abajo.",
                            color = TextoCremaApagado,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // ---- Tallas y precios: checkbox que despliega el precio in-line ----
            item {
                Text(
                    "Tallas y precio",
                    color = TextoCrema,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
            }
            items(TALLAS_DISPONIBLES, key = { it }) { talla ->
                val tallaCaptura = tallas[talla]
                val marcada = tallaCaptura != null
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(EcoPosShapes.TarjetaChica)
                        .background(if (marcada) FondoTarjeta else Color.Transparent)
                        .padding(if (marcada) 12.dp else 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = marcada,
                            onCheckedChange = { activo -> viewModel.alternarTalla(talla, activo) },
                            colors = CheckboxDefaults.colors(checkedColor = AcentoRosa, uncheckedColor = TextoCremaApagado)
                        )
                        Text("Talla $talla", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    if (marcada && tallaCaptura != null) {
                        Column(modifier = Modifier.padding(start = 40.dp, top = 4.dp)) {
                            tallaCaptura.escalones.forEach { escalon ->
                                key(talla, escalon.etiqueta) {
                                    Row(
                                        modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${escalon.etiqueta} (${escalon.cantidadMinima}+):",
                                            color = TextoCremaApagado,
                                            fontSize = 12.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextField(
                                            value = escalon.precioTexto,
                                            onValueChange = { nuevo -> viewModel.actualizarPrecioEscalon(talla, escalon.etiqueta, nuevo) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            placeholder = { Text("0.00") },
                                            modifier = Modifier.padding(start = 8.dp).width(100.dp),
                                            colors = camposTextoColores(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Colores: lista + fila expandible para agregar uno nuevo ----
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Colores", color = TextoCrema, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (tallas.isNotEmpty() && !colorNuevoAbierto) {
                        Text(
                            "+ Agregar color",
                            color = AcentoRosa,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { colorNuevoAbierto = true }
                        )
                    }
                }
                if (tallas.isEmpty()) {
                    Text(
                        "Marca al menos una talla con su precio para poder agregar colores.",
                        color = TextoCremaApagado,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            items(colores, key = { it.id }) { colorCaptura ->
                FilaColorGuardado(colorCaptura = colorCaptura, onQuitar = { viewModel.quitarColor(colorCaptura.id) })
            }

            if (colorNuevoAbierto && tallas.isNotEmpty()) {
                item {
                    FilaAgregarColor(
                        tallasMarcadas = viewModel.tallasMarcadasOrdenadas(),
                        modoCodigoBarras = modoCodigoBarras,
                        color = colorNombreNuevo,
                        onColorCambio = { colorNombreNuevo = it },
                        stockPorTalla = stockPorTallaNuevo,
                        onStockPorTallaCambio = { stockPorTallaNuevo = it },
                        coloresAgregados = coloresAgregadosSesion,
                        onEscanearParaTalla = { callback ->
                            callbackEscaneoTalla = callback
                            if (tienePermisoCamara(context)) mostrandoEscaner = true
                            else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                        },
                        onGuardar = { color, stockPorTalla ->
                            viewModel.agregarColor(color, stockPorTalla)
                            coloresAgregadosSesion += 1
                            colorNombreNuevo = ""
                            stockPorTallaNuevo = viewModel.tallasMarcadasOrdenadas()
                                .associate { it.talla to StockTallaEnCaptura(talla = it.talla) }
                            // Se queda abierta y limpia para seguir agregando el
                            // siguiente color sin volver a tocar "+ Agregar color".
                        },
                        onCerrar = { colorNuevoAbierto = false }
                    )
                }
            }

            // ---- Estado y botón guardar ----
            item {
                if (estadoGuardado is EstadoGuardado.Error) {
                    Text(
                        (estadoGuardado as EstadoGuardado.Error).mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                Button(
                    onClick = { viewModel.guardarProducto() },
                    enabled = estadoGuardado !is EstadoGuardado.Guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoRosa),
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp)
                ) {
                    Text(
                        if (estadoGuardado is EstadoGuardado.Guardando) "Guardando..." else "Guardar producto",
                        color = TextoCrema,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaColorGuardado(colorCaptura: ColorEnCaptura, onQuitar: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(FondoTarjeta)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(colorCaptura.color, color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val totalStock = colorCaptura.stockPorTalla.values.sumOf { it.stockTexto.toIntOrNull() ?: 0 }
            Text(
                "${colorCaptura.stockPorTalla.size} tallas · $totalStock unidades",
                color = TextoCremaApagado,
                fontSize = 12.sp
            )
        }
        Text("✕", color = ColorError, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onQuitar))
    }
}

@Composable
private fun FilaAgregarColor(
    tallasMarcadas: List<TallaEnCaptura>,
    modoCodigoBarras: ModoCodigoBarras,
    color: String,
    onColorCambio: (String) -> Unit,
    stockPorTalla: Map<String, StockTallaEnCaptura>,
    onStockPorTallaCambio: (Map<String, StockTallaEnCaptura>) -> Unit,
    coloresAgregados: Int,
    onEscanearParaTalla: ((String) -> Unit) -> Unit,
    onGuardar: (String, Map<String, StockTallaEnCaptura>) -> Unit,
    onCerrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(EcoPosShapes.Tarjeta)
            .background(FondoTarjeta)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CampoTexto(valor = color, onCambio = onColorCambio, placeholder = "Nombre del color (ej. Vino, Azul)")
            }
            Text(
                "✕",
                color = TextoCremaApagado,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 10.dp).clickable(onClick = onCerrar)
            )
        }

        if (coloresAgregados > 0) {
            Text(
                "✓ $coloresAgregados color${if (coloresAgregados == 1) "" else "es"} agregado${if (coloresAgregados == 1) "" else "s"} en esta sesión",
                color = ColorExito,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        tallasMarcadas.forEach { talla ->
            val stockCaptura = stockPorTalla[talla.talla] ?: StockTallaEnCaptura(talla.talla)
            key(talla.talla) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Talla ${talla.talla} — stock:", color = TextoCremaApagado, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextField(
                        value = stockCaptura.stockTexto,
                        onValueChange = { nuevo ->
                            val limpio = nuevo.filter { it.isDigit() }
                            onStockPorTallaCambio(stockPorTalla + (talla.talla to stockCaptura.copy(stockTexto = limpio)))
                        },
                        placeholder = { Text("0", color = TextoCremaApagado) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                        colors = camposTextoColores(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                if (modoCodigoBarras == ModoCodigoBarras.INDEPENDIENTE) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            CampoTexto(
                                valor = stockCaptura.codigoBarrasTexto,
                                onCambio = { nuevo -> onStockPorTallaCambio(stockPorTalla + (talla.talla to stockCaptura.copy(codigoBarrasTexto = nuevo))) },
                                placeholder = "Código de barras de esta talla"
                            )
                        }
                        BotonEscanear(modifier = Modifier.padding(start = 8.dp), tamano = 40.dp) {
                            onEscanearParaTalla { codigo ->
                                onStockPorTallaCambio(stockPorTalla + (talla.talla to (stockPorTalla[talla.talla] ?: stockCaptura).copy(codigoBarrasTexto = codigo)))
                            }
                        }
                    }
                }
            }
            }
        }

        val puedeGuardar = color.isNotBlank() &&
            (modoCodigoBarras == ModoCodigoBarras.COMPARTIDO || stockPorTalla.values.all { it.codigoBarrasTexto.isNotBlank() })

        Button(
            onClick = { onGuardar(color, stockPorTalla) },
            enabled = puedeGuardar,
            colors = ButtonDefaults.buttonColors(containerColor = AcentoRosa, disabledContainerColor = FondoCarbon),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text(
                "Agregar y seguir con otro color",
                color = if (puedeGuardar) TextoCrema else TextoCremaApagado,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BotonEscanear(modifier: Modifier = Modifier, tamano: androidx.compose.ui.unit.Dp = 48.dp, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(AcentoRosa)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("📷", fontSize = if (tamano > 44.dp) 20.sp else 16.sp)
    }
}

@Composable
private fun OpcionModoCodigoBarras(texto: String, seleccionada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionada) AcentoRosa else FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            texto,
            color = if (seleccionada) TextoCrema else TextoCremaApagado,
            fontSize = 12.5.sp,
            fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ChipSeleccionable(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionada) AcentoRosa else FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(texto, color = if (seleccionada) TextoCrema else TextoCremaApagado, fontSize = 13.sp)
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    tipoTeclado: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text(placeholder, color = TextoCremaApagado) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        modifier = Modifier.fillMaxWidth(),
        colors = camposTextoColores(),
        shape = EcoPosShapes.Campo
    )
}

@Composable
private fun camposTextoColores() = TextFieldDefaults.colors(
    focusedContainerColor = EcoPosColors.FondoInput,
    unfocusedContainerColor = EcoPosColors.FondoInput,
    focusedTextColor = TextoCrema,
    unfocusedTextColor = TextoCrema,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedPlaceholderColor = TextoCremaApagado,
    unfocusedPlaceholderColor = TextoCremaApagado,
    cursorColor = AcentoRosa
)
