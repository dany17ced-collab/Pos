package com.tuempresa.possystem.presentation.importar

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.domain.importacion.GeneradorPlantillaExcel
import com.tuempresa.possystem.domain.importacion.LectorExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Fila ya parseada del Excel, con su resultado de validación. Se muestra en la
 * pestaña "Producto" del formulario, antes de que el usuario confirme "Importar".
 */
data class FilaProductoImportado(
    val numeroFila: Int,
    val nombre: String,
    val sku: String,
    val codigoBarras: String?,
    val precioVenta: Double?,
    val precioCompra: Double?,
    val stock: Int?,
    val error: String? = null
) {
    val esValida: Boolean get() = error == null
}

sealed class EstadoImportacion {
    object SinArchivo : EstadoImportacion()
    object Leyendo : EstadoImportacion()
    data class Previsualizando(val filas: List<FilaProductoImportado>) : EstadoImportacion()
    object Importando : EstadoImportacion()
    data class Completado(val cantidadImportada: Int) : EstadoImportacion()
    data class Error(val mensaje: String) : EstadoImportacion()
}

/**
 * Columnas esperadas en la plantilla (fila 1 = encabezados, se ignora al leer):
 * A: Nombre (obligatorio) | B: SKU (opcional, se genera uno si falta)
 * C: Código de barras (opcional) | D: Precio de venta (obligatorio, numérico)
 * E: Precio de compra (opcional) | F: Stock inicial (opcional, entero, default 0)
 */
class ImportarProductosViewModel(private val app: POSApplication) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoImportacion>(EstadoImportacion.SinArchivo)
    val estado: StateFlow<EstadoImportacion> = _estado.asStateFlow()

    private val _nombreArchivo = MutableStateFlow<String?>(null)
    val nombreArchivo: StateFlow<String?> = _nombreArchivo.asStateFlow()

    private val _uriPlantillaEjemplo = MutableStateFlow<Uri?>(null)
    val uriPlantillaEjemplo: StateFlow<Uri?> = _uriPlantillaEjemplo.asStateFlow()

    /** Genera y expone (vía FileProvider) la plantilla .xlsx de ejemplo para "Descargar Ejemplo". */
    fun descargarPlantillaEjemplo() {
        viewModelScope.launch {
            try {
                val archivo = withContext(Dispatchers.IO) {
                    val carpeta = File(app.cacheDir, "plantillas").apply { mkdirs() }
                    val destino = File(carpeta, "plantilla_productos.xlsx")
                    GeneradorPlantillaExcel.generar(destino)
                    destino
                }
                _uriPlantillaEjemplo.value = FileProvider.getUriForFile(
                    app, "${app.packageName}.fileprovider", archivo
                )
            } catch (e: Exception) {
                _estado.value = EstadoImportacion.Error("No se pudo generar la plantilla de ejemplo.")
            }
        }
    }

    fun plantillaCompartida() {
        _uriPlantillaEjemplo.value = null
    }

    fun archivoElegido(uri: Uri, nombreMostrado: String) {
        _nombreArchivo.value = nombreMostrado
        _estado.value = EstadoImportacion.Leyendo
        viewModelScope.launch {
            try {
                val filas = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { flujo ->
                        LectorExcel.leerPrimeraHoja(flujo)
                    } ?: emptyList()
                }
                val filasProducto = filas.drop(1) // fila 0 = encabezados
                    .filter { fila -> fila.any { it.isNotBlank() } }
                    .mapIndexed { indice, fila -> mapearFila(indice + 2, fila) } // +2: 1-based y saltando encabezado

                if (filasProducto.isEmpty()) {
                    _estado.value = EstadoImportacion.Error("El archivo no tiene filas de productos para importar.")
                } else {
                    _estado.value = EstadoImportacion.Previsualizando(filasProducto)
                }
            } catch (e: Exception) {
                _estado.value = EstadoImportacion.Error("No se pudo leer el archivo. Verifica que sea un .xlsx válido.")
            }
        }
    }

    private fun mapearFila(numeroFila: Int, celdas: List<String>): FilaProductoImportado {
        val nombre = celdas.getOrNull(0)?.trim().orEmpty()
        val sku = celdas.getOrNull(1)?.trim().orEmpty()
        val codigoBarras = celdas.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        val precioVentaTexto = celdas.getOrNull(3)?.trim().orEmpty()
        val precioCompraTexto = celdas.getOrNull(4)?.trim().orEmpty()
        val stockTexto = celdas.getOrNull(5)?.trim().orEmpty()

        val precioVenta = precioVentaTexto.replace(",", ".").toDoubleOrNull()
        val precioCompra = precioCompraTexto.replace(",", ".").toDoubleOrNull()
        val stock = stockTexto.toDoubleOrNull()?.toInt()

        val error = when {
            nombre.isEmpty() -> "El nombre es obligatorio."
            nombre.length > 40 -> "El nombre supera los 40 caracteres."
            precioVenta == null -> "Precio de venta inválido o vacío."
            else -> null
        }

        return FilaProductoImportado(
            numeroFila = numeroFila,
            nombre = nombre,
            sku = sku,
            codigoBarras = codigoBarras,
            precioVenta = precioVenta,
            precioCompra = precioCompra,
            stock = stock,
            error = error
        )
    }

    fun confirmarImportacion() {
        val estadoActual = _estado.value
        if (estadoActual !is EstadoImportacion.Previsualizando) return

        val validas = estadoActual.filas.filter { it.esValida }
        if (validas.isEmpty()) {
            _estado.value = EstadoImportacion.Error("No hay filas válidas para importar.")
            return
        }

        _estado.value = EstadoImportacion.Importando
        viewModelScope.launch {
            try {
                val dao = app.database.productoDao()
                val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
                val skusUsados = mutableSetOf<String>()
                val entidades = validas.map { fila ->
                    val skuFinal = if (fila.sku.isNotBlank() && dao.buscarPorSku(tiendaId, fila.sku) == null && skusUsados.add(fila.sku)) {
                        fila.sku
                    } else {
                        "IMP-${UUID.randomUUID().toString().take(8).uppercase()}"
                    }
                    ProductoEntity(
                        id = UUID.randomUUID().toString(),
                        tiendaId = tiendaId,
                        sku = skuFinal,
                        codigoBarras = fila.codigoBarras,
                        nombre = fila.nombre,
                        categoriaId = null,
                        precioCompra = fila.precioCompra ?: 0.0,
                        precioVenta = fila.precioVenta ?: 0.0,
                        stockActual = fila.stock ?: 0
                    )
                }
                withContext(Dispatchers.IO) { dao.insertarTodos(entidades) }
                _estado.value = EstadoImportacion.Completado(entidades.size)
            } catch (e: Exception) {
                _estado.value = EstadoImportacion.Error("Ocurrió un error al guardar los productos importados.")
            }
        }
    }

    fun reiniciar() {
        _estado.value = EstadoImportacion.SinArchivo
        _nombreArchivo.value = null
    }
}
