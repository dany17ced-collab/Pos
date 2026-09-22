package com.tuempresa.possystem.presentation.importar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

@Composable
fun PantallaImportarProductos(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: ImportarProductosViewModel = viewModel(
        factory = fabricaViewModel(app) { ImportarProductosViewModel(app) }
    )
    val estado by viewModel.estado.collectAsState()
    val nombreArchivo by viewModel.nombreArchivo.collectAsState()
    val uriPlantilla by viewModel.uriPlantillaEjemplo.collectAsState()
    val contexto = LocalContext.current

    var tabSeleccionada by remember { mutableStateOf(0) }

    val selectorArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val nombre = obtenerNombreArchivo(contexto, uri)
            viewModel.archivoElegido(uri, nombre)
            tabSeleccionada = 1
        }
    }

    LaunchedEffect(uriPlantilla) {
        uriPlantilla?.let { uri ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            contexto.startActivity(android.content.Intent.createChooser(intent, "Compartir plantilla"))
            viewModel.plantillaCompartida()
        }
    }

    Scaffold(containerColor = EcoPosColors.FondoNegro) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Importar Productos", subtitulo = "Carga masiva desde un archivo Excel", onVolver = onVolver)

            TabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor = EcoPosColors.FondoNegro,
                contentColor = EcoPosColors.TextoBlanco
            ) {
                Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 }, text = { Text("Datos") })
                Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 }, text = { Text("Vista previa") })
            }

            Box(modifier = Modifier.weight(1f)) {
                if (tabSeleccionada == 0) {
                    PestanaDatos(
                        nombreArchivo = nombreArchivo,
                        onDescargarEjemplo = { viewModel.descargarPlantillaEjemplo() },
                        onBuscarArchivo = { selectorArchivo.launch(arrayOf("application/*", "*/*")) }
                    )
                } else {
                    PestanaProducto(estado = estado)
                }
            }

            BotonImportar(estado = estado, onImportar = { viewModel.confirmarImportacion() })
        }
    }
}

@Composable
private fun PestanaDatos(
    nombreArchivo: String?,
    onDescargarEjemplo: () -> Unit,
    onBuscarArchivo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(EcoPosShapes.Tarjeta)
                .background(EcoPosColors.AcentoAmbar.copy(alpha = 0.85f))
                .padding(20.dp)
        ) {
            Text(
                "Su archivo de Excel (XLSX) debe usar el mismo formato que la plantilla designada.",
                color = EcoPosColors.TextoBlanco,
                fontSize = 14.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(EcoPosShapes.Boton)
                    .background(EcoPosColors.AcentoAmbar)
                    .clickable(onClick = onDescargarEjemplo)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("DESCARGAR EJEMPLO", color = EcoPosColors.FondoNegro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "Archivo",
            color = EcoPosColors.TextoBlanco,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(EcoPosShapes.TarjetaChica)
                .padding(1.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(nombreArchivo ?: "Ninguno", color = EcoPosColors.TextoBlanco, fontSize = 15.sp)
            Row(
                modifier = Modifier
                    .clip(EcoPosShapes.Chip)
                    .background(EcoPosColors.VerdeMenta)
                    .clickable(onClick = onBuscarArchivo)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = EcoPosColors.FondoNegro, modifier = Modifier.height(16.dp))
                Text("Buscar", color = EcoPosColors.FondoNegro, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 20.dp)
                .clip(EcoPosShapes.Tarjeta)
                .background(EcoPosColors.AmbarCategoria.copy(alpha = 0.25f))
                .padding(20.dp)
        ) {
            Text("Información", color = EcoPosColors.AmbarCategoria, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            BloqueInfo("Nombre", "El nombre es obligatorio y tiene una longitud máxima de 40 caracteres.")
            BloqueInfo("SKU", "Si proporciona SKU para su producto, debe ser único. Si se repite o está vacío, se genera uno automáticamente.")
            BloqueInfo("Precio de venta", "Es obligatorio y debe ser un número (usa punto o coma decimal).")
            BloqueInfo("Código de barras, precio de compra y stock", "Son opcionales; si se dejan vacíos se guardan como 0 o sin código.")
        }
    }
}

@Composable
private fun BloqueInfo(titulo: String, texto: String) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(titulo, color = EcoPosColors.AmbarCategoria, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(texto, color = EcoPosColors.TextoBlanco, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PestanaProducto(estado: EstadoImportacion) {
    when (estado) {
        is EstadoImportacion.SinArchivo -> MensajeCentrado("Sin Producto")
        is EstadoImportacion.Leyendo -> Cargando("Leyendo archivo...")
        is EstadoImportacion.Importando -> Cargando("Importando productos...")
        is EstadoImportacion.Error -> MensajeCentrado(estado.mensaje, esError = true)
        is EstadoImportacion.Completado -> MensajeCentrado(
            "${estado.cantidadImportada} producto(s) importado(s) correctamente.",
            esExito = true
        )
        is EstadoImportacion.Previsualizando -> {
            val validas = estado.filas.count { it.esValida }
            LazyColumn(contentPadding = PaddingValues(20.dp)) {
                item {
                    Text(
                        "$validas de ${estado.filas.size} filas listas para importar",
                        color = EcoPosColors.TextoBlanco,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(estado.filas) { fila -> TarjetaFilaImportada(fila) }
            }
        }
    }
}

@Composable
private fun TarjetaFilaImportada(fila: FilaProductoImportado) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                fila.nombre.ifBlank { "Fila ${fila.numeroFila}" },
                color = EcoPosColors.TextoBlanco,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (fila.error != null) {
                Text(fila.error, color = EcoPosColors.ColorError, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            } else {
                Text(
                    "S/.${"%.2f".format(fila.precioVenta ?: 0.0)} · stock ${fila.stock ?: 0}",
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            if (fila.esValida) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = null,
            tint = if (fila.esValida) EcoPosColors.VerdeMenta else EcoPosColors.ColorError
        )
    }
}

@Composable
private fun MensajeCentrado(texto: String, esError: Boolean = false, esExito: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            texto,
            color = when {
                esError -> EcoPosColors.ColorError
                esExito -> EcoPosColors.VerdeMenta
                else -> EcoPosColors.TextoGrisApagado
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Cargando(texto: String) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = EcoPosColors.VerdeMenta)
        Text(texto, color = EcoPosColors.TextoGrisApagado, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun BotonImportar(estado: EstadoImportacion, onImportar: () -> Unit) {
    val habilitado = estado is EstadoImportacion.Previsualizando && estado.filas.any { it.esValida }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .height(52.dp)
            .clip(EcoPosShapes.Boton)
            .background(if (habilitado) EcoPosColors.VerdeMenta else EcoPosColors.FondoTarjetaClara)
            .clickable(enabled = habilitado, onClick = onImportar),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Importar productos",
            color = if (habilitado) EcoPosColors.FondoNegro else EcoPosColors.TextoGrisApagado,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun obtenerNombreArchivo(contexto: android.content.Context, uri: Uri): String {
    var nombre = "archivo.xlsx"
    contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val indice = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (indice >= 0 && cursor.moveToFirst()) nombre = cursor.getString(indice)
    }
    return nombre
}
