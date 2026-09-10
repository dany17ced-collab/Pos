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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.tuempresa.possystem.presentation.venta.EscanerCodigoBarras
import com.tuempresa.possystem.presentation.venta.tienePermisoCamara

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)

@Composable
fun PantallaNuevoProducto(app: POSApplication, onVolver: () -> Unit, onGuardado: () -> Unit) {
    val viewModel: NuevoProductoViewModel = viewModel(factory = fabricaSimple { NuevoProductoViewModel(app) })

    val nombre by viewModel.nombre.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val codigoBarras by viewModel.codigoBarras.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaId by viewModel.categoriaId.collectAsState()
    val precioCompraTexto by viewModel.precioCompraTexto.collectAsState()
    val escalones by viewModel.escalones.collectAsState()
    val colores by viewModel.colores.collectAsState()
    val estadoGuardado by viewModel.estadoGuardado.collectAsState()

    var mostrandoEscaner by remember { mutableStateOf(false) }
    var mostrandoAgregarColor by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) mostrandoEscaner = true }

    if (estadoGuardado is EstadoGuardado.Exitoso) {
        onGuardado()
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        if (mostrandoEscaner) {
            Box(modifier = Modifier.fillMaxSize()) {
                EscanerCodigoBarras(
                    modifier = Modifier.fillMaxSize(),
                    onCodigoDetectado = { codigo ->
                        mostrandoEscaner = false
                        viewModel.actualizarCodigoBarras(codigo)
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
        } else if (mostrandoAgregarColor) {
            PantallaAgregarColor(
                onGuardar = { color, stockPorTalla ->
                    viewModel.agregarColor(color, stockPorTalla)
                    mostrandoAgregarColor = false
                },
                onCancelar = { mostrandoAgregarColor = false }
            )
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
                        text = "Nuevo producto",
                        color = TextoCrema,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                SeccionDatosGenerales(
                    nombre = nombre,
                    onNombreCambiado = viewModel::actualizarNombre,
                    descripcion = descripcion,
                    onDescripcionCambiada = viewModel::actualizarDescripcion,
                    codigoBarras = codigoBarras,
                    onCodigoBarrasCambiado = viewModel::actualizarCodigoBarras,
                    onEscanear = {
                        if (tienePermisoCamara(context)) mostrandoEscaner = true
                        else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                    },
                    categorias = categorias,
                    categoriaId = categoriaId,
                    onCategoriaSeleccionada = viewModel::seleccionarCategoria,
                    precioCompraTexto = precioCompraTexto,
                    onPrecioCompraCambiado = viewModel::actualizarPrecioCompra
                )

                SeccionEscalonesPrecio(
                    escalones = escalones,
                    onPrecioCambiado = viewModel::actualizarPrecioEscalon
                )

                SeccionColores(
                    colores = colores,
                    onAgregarColor = { mostrandoAgregarColor = true },
                    onQuitarColor = viewModel::quitarColor
                )

                if (estadoGuardado is EstadoGuardado.Error) {
                    Text(
                        text = (estadoGuardado as EstadoGuardado.Error).mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Button(
                    onClick = { viewModel.guardarProducto() },
                    enabled = estadoGuardado !is EstadoGuardado.Guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        if (estadoGuardado is EstadoGuardado.Guardando) "Guardando…" else "Guardar producto",
                        color = FondoCarbon,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionDatosGenerales(
    nombre: String,
    onNombreCambiado: (String) -> Unit,
    descripcion: String,
    onDescripcionCambiada: (String) -> Unit,
    codigoBarras: String,
    onCodigoBarrasCambiado: (String) -> Unit,
    onEscanear: () -> Unit,
    categorias: List<CategoriaEntity>,
    categoriaId: String?,
    onCategoriaSeleccionada: (String?) -> Unit,
    precioCompraTexto: String,
    onPrecioCompraCambiado: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        CampoTexto(valor = nombre, onCambio = onNombreCambiado, placeholder = "Nombre del producto")
        Spacer(modifier = Modifier.height(10.dp))
        CampoTexto(valor = descripcion, onCambio = onDescripcionCambiada, placeholder = "Descripción (opcional)")
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CampoTexto(valor = codigoBarras, onCambio = onCodigoBarrasCambiado, placeholder = "Código de barras")
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
        Spacer(modifier = Modifier.height(10.dp))

        CampoTexto(
            valor = precioCompraTexto,
            onCambio = onPrecioCompraCambiado,
            placeholder = "Precio de compra",
            tipoTeclado = KeyboardType.Decimal
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (categorias.isNotEmpty()) {
            Text("Categoría", color = TextoCremaApagado, fontSize = 13.sp)
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorias.forEach { categoria ->
                    val seleccionada = categoriaId == categoria.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (seleccionada) AcentoTerracota else FondoTarjeta)
                            .clickable { onCategoriaSeleccionada(if (seleccionada) null else categoria.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                        Text(
                            categoria.nombre,
                            color = if (seleccionada) FondoCarbon else TextoCrema,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionEscalonesPrecio(
    escalones: List<EscalonEnCaptura>,
    onPrecioCambiado: (String, String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Precios por cantidad", color = TextoCrema, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(
            "Aplican a todas las tallas y colores de este producto",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )
        escalones.forEach { escalon ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = escalon.etiqueta,
                    color = TextoCrema,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "≥${escalon.cantidadMinima}",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                TextField(
                    value = escalon.precioTexto,
                    onValueChange = { onPrecioCambiado(escalon.etiqueta, it) },
                    placeholder = { Text("S/", color = TextoCremaApagado) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.size(width = 100.dp, height = 52.dp),
                    colors = camposTextoColores(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun SeccionColores(
    colores: List<ColorEnCaptura>,
    onAgregarColor: () -> Unit,
    onQuitarColor: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Colores y tallas",
                color = TextoCrema,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AcentoTerracota)
                    .clickable(onClick = onAgregarColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("+ Agregar color", color = FondoCarbon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (colores.isEmpty()) {
            Text(
                "Aún no agregas ningún color",
                color = TextoCremaApagado,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        } else {
            colores.forEach { colorCaptura ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FondoTarjeta)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(colorCaptura.color, color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        val resumen = colorCaptura.stockPorTalla.entries.joinToString(", ") { (talla, stock) ->
                            "T$talla: $stock"
                        }
                        Text(resumen, color = TextoCremaApagado, fontSize = 12.sp)
                    }
                    Text(
                        "✕",
                        color = ColorError,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { onQuitarColor(colorCaptura.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PantallaAgregarColor(
    onGuardar: (String, Map<String, Int>) -> Unit,
    onCancelar: () -> Unit
) {
    var color by remember { mutableStateOf("") }
    var tallasSeleccionadas by remember { mutableStateOf(setOf<String>()) }
    var stockPorTalla by remember { mutableStateOf(mapOf<String, String>()) }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "‹ Cancelar",
                color = TextoCrema,
                fontSize = 16.sp,
                modifier = Modifier.clickable(onClick = onCancelar)
            )
            Text(
                text = "Agregar color",
                color = TextoCrema,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            CampoTexto(valor = color, onCambio = { color = it }, placeholder = "Nombre del color (ej. Vino, Azul)")

            Text(
                "Tallas disponibles",
                color = TextoCremaApagado,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )

            TALLAS_DISPONIBLES.forEach { talla ->
                val marcada = talla in tallasSeleccionadas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = marcada,
                        onCheckedChange = { activo ->
                            tallasSeleccionadas = if (activo) tallasSeleccionadas + talla else tallasSeleccionadas - talla
                        },
                        colors = CheckboxDefaults.colors(checkedColor = AcentoTerracota, uncheckedColor = TextoCremaApagado)
                    )
                    Text("Talla $talla", color = TextoCrema, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (marcada) {
                        TextField(
                            value = stockPorTalla[talla] ?: "",
                            onValueChange = { nuevo ->
                                if (nuevo.all { it.isDigit() }) {
                                    stockPorTalla = stockPorTalla + (talla to nuevo)
                                }
                            },
                            placeholder = { Text("Stock", color = TextoCremaApagado) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.size(width = 90.dp, height = 52.dp),
                            colors = camposTextoColores(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            val puedeGuardar = color.isNotBlank() &&
                tallasSeleccionadas.isNotEmpty() &&
                tallasSeleccionadas.all { (stockPorTalla[it]?.toIntOrNull() ?: -1) >= 0 }

            Button(
                onClick = {
                    val mapa = tallasSeleccionadas.associateWith { stockPorTalla[it]?.toIntOrNull() ?: 0 }
                    onGuardar(color, mapa)
                },
                enabled = puedeGuardar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcentoTerracota,
                    disabledContainerColor = FondoTarjeta
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    "Agregar",
                    color = if (puedeGuardar) FondoCarbon else TextoCremaApagado,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
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
