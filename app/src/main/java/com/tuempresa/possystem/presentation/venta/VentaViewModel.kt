package com.tuempresa.possystem.presentation.venta

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.StockInsuficienteException
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.data.local.entity.VentaEntity
import com.tuempresa.possystem.domain.boleta.DispositivoBluetooth
import com.tuempresa.possystem.domain.boleta.GeneradorBoletaPdf
import com.tuempresa.possystem.domain.boleta.GeneradorTicketEscPos
import com.tuempresa.possystem.domain.boleta.ImpresoraTermicaBluetooth
import com.tuempresa.possystem.domain.boleta.ResultadoImpresion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Resultado de buscar productos por código de barras o texto. */
sealed class ResultadoBusqueda {
    object SinBuscar : ResultadoBusqueda()
    object Buscando : ResultadoBusqueda()
    object NoEncontrado : ResultadoBusqueda()
    /** Varios productos coinciden con el texto (ej. "buso" -> franela, algodón, french terry). */
    data class VariosResultados(val productos: List<ProductoEntity>) : ResultadoBusqueda()
    /** Un producto específico ya elegido: se muestran sus variantes (talla/color). */
    data class Encontrado(val padre: ProductoEntity?, val variantes: List<ProductoEntity>) : ResultadoBusqueda()
}

sealed class EstadoCobro {
    object Inactivo : EstadoCobro()
    object Procesando : EstadoCobro()
    data class Exitoso(val ventaId: String, val folio: Long?) : EstadoCobro()
    data class Error(val mensaje: String) : EstadoCobro()
}

/** Estado de las acciones sobre la boleta ya emitida: compartir PDF o imprimir en la térmica. */
sealed class EstadoBoleta {
    object Inactivo : EstadoBoleta()
    object GenerandoPdf : EstadoBoleta()
    data class PdfListo(val uri: Uri) : EstadoBoleta()
    object BuscandoImpresoras : EstadoBoleta()
    data class ImpresorasEncontradas(val dispositivos: List<DispositivoBluetooth>) : EstadoBoleta()
    object Imprimiendo : EstadoBoleta()
    object ImpresionExitosa : EstadoBoleta()
    data class Error(val mensaje: String) : EstadoBoleta()
}

class VentaViewModel(private val app: POSApplication) : ViewModel() {

    private val productoDao = app.database.productoDao()
    private val ventaDao = app.database.ventaDao()
    private val configuracionTiendaDao = app.database.configuracionTiendaDao()
    private val precioCalculator = app.precioCalculator
    private val impresora = ImpresoraTermicaBluetooth(app)

    private val _resultadoBusqueda = MutableStateFlow<ResultadoBusqueda>(ResultadoBusqueda.SinBuscar)
    val resultadoBusqueda: StateFlow<ResultadoBusqueda> = _resultadoBusqueda.asStateFlow()

    private val _carrito = MutableStateFlow<List<LineaCarrito>>(emptyList())
    val carrito: StateFlow<List<LineaCarrito>> = _carrito.asStateFlow()

    private val _estadoCobro = MutableStateFlow<EstadoCobro>(EstadoCobro.Inactivo)
    val estadoCobro: StateFlow<EstadoCobro> = _estadoCobro.asStateFlow()

    private val _estadoBoleta = MutableStateFlow<EstadoBoleta>(EstadoBoleta.Inactivo)
    val estadoBoleta: StateFlow<EstadoBoleta> = _estadoBoleta.asStateFlow()

    fun calcularTotal(lineas: List<LineaCarrito>): Double = lineas.sumOf { it.subtotal }

    /** Búsqueda por código de barras exacto (viene del escáner): va directo a variantes. */
    fun buscarPorCodigoBarras(codigo: String) {
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusqueda.Buscando
            val producto = productoDao.buscarPorCodigoBarras(codigo)
            if (producto == null) {
                _resultadoBusqueda.value = ResultadoBusqueda.NoEncontrado
            } else {
                mostrarVariantesDe(producto)
            }
        }
    }

    /**
     * Búsqueda manual por nombre o SKU. Si hay varios productos padre que coinciden
     * (ej. "buso" -> franela, algodón, french terry), se muestran todos para elegir.
     * Si solo hay uno, se pasa directo a mostrar sus variantes.
     */
    fun buscarPorTexto(texto: String) {
        if (texto.isBlank()) {
            _resultadoBusqueda.value = ResultadoBusqueda.SinBuscar
            return
        }
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusqueda.Buscando

            val porSku = productoDao.buscarPorSku(texto)
            if (porSku != null) {
                mostrarVariantesDe(porSku)
                return@launch
            }

            val coincidencias = productoDao.buscar(texto).first()
                .filter { it.productoBaseId == null }

            when {
                coincidencias.isEmpty() -> _resultadoBusqueda.value = ResultadoBusqueda.NoEncontrado
                coincidencias.size == 1 -> mostrarVariantesDe(coincidencias.first())
                else -> _resultadoBusqueda.value = ResultadoBusqueda.VariosResultados(coincidencias)
            }
        }
    }

    /** Se llama cuando el usuario elige un producto específico de la lista de VariosResultados. */
    fun elegirProducto(producto: ProductoEntity) {
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusqueda.Buscando
            mostrarVariantesDe(producto)
        }
    }

    private suspend fun mostrarVariantesDe(producto: ProductoEntity) {
        val esVariante = producto.productoBaseId != null
        val idPadre = if (esVariante) producto.productoBaseId!! else producto.id
        val padre = if (esVariante) productoDao.obtenerPorId(idPadre) else producto

        val variantes = productoDao.observarVariantes(idPadre).first()

        _resultadoBusqueda.value = if (variantes.isEmpty()) {
            // Producto simple, sin variantes de talla/color: se vende directo con su propio stock
            ResultadoBusqueda.Encontrado(padre = null, variantes = listOf(producto))
        } else {
            ResultadoBusqueda.Encontrado(padre = padre, variantes = variantes)
        }
    }

    fun limpiarBusqueda() {
        _resultadoBusqueda.value = ResultadoBusqueda.SinBuscar
    }

    /** Calcula el precio sugerido por escalón para mostrarlo editable en la UI antes de agregar. */
    suspend fun calcularPrecioSugerido(variante: ProductoEntity, cantidad: Int): Double {
        return precioCalculator.calcularPrecioUnitario(variante, cantidad)
    }

    suspend fun obtenerEtiquetaEscalon(variante: ProductoEntity, cantidad: Int): String {
        return precioCalculator.obtenerEtiquetaEscalon(variante, cantidad)
    }

    /**
     * Agrega una variante al carrito con la cantidad y el PRECIO indicados explícitamente.
     * El precio ya viene resuelto por la UI (que parte del escalón sugerido pero permite
     * al vendedor sobrescribirlo manualmente antes de confirmar, ej. para un descuento puntual).
     */
    fun agregarAlCarrito(variante: ProductoEntity, cantidad: Int, precioUnitario: Double, etiquetaEscalon: String) {
        if (cantidad <= 0 || precioUnitario < 0) return

        val lineaExistente = _carrito.value.find { it.producto.id == variante.id }
        if (lineaExistente != null) {
            val nuevaCantidad = lineaExistente.cantidad + cantidad
            _carrito.value = _carrito.value.map {
                if (it.id == lineaExistente.id) {
                    it.copy(cantidad = nuevaCantidad, precioUnitario = precioUnitario, etiquetaEscalon = etiquetaEscalon)
                } else it
            }
        } else {
            _carrito.value = _carrito.value + LineaCarrito(
                id = UUID.randomUUID().toString(),
                producto = variante,
                cantidad = cantidad,
                precioUnitario = precioUnitario,
                etiquetaEscalon = etiquetaEscalon
            )
        }
        limpiarBusqueda()
    }

    fun quitarDelCarrito(lineaId: String) {
        _carrito.value = _carrito.value.filterNot { it.id == lineaId }
    }

    /**
     * Agrega varias variantes al carrito de una sola vez (pedido mayorista:
     * varias tallas/colores en una sola pasada). Cada línea trae su propia
     * cantidad, precio y etiqueta de escalón, ya resueltos por la UI.
     */
    fun agregarVariasAlCarrito(lineas: List<LineaPendiente>) {
        lineas.forEach { linea ->
            agregarAlCarrito(linea.variante, linea.cantidad, linea.precioUnitario, linea.etiquetaEscalon)
        }
    }

    data class LineaPendiente(
        val variante: ProductoEntity,
        val cantidad: Int,
        val precioUnitario: Double,
        val etiquetaEscalon: String
    )

    fun vaciarCarrito() {
        _carrito.value = emptyList()
    }

    /** Confirma el cobro: arma la venta y sus detalles, y llama a la transacción atómica del DAO. */
    fun confirmarCobro(metodoPago: MetodoPago, montoRecibido: Double?) {
        val lineas = _carrito.value
        if (lineas.isEmpty()) return

        viewModelScope.launch {
            _estadoCobro.value = EstadoCobro.Procesando
            try {
                val ventaId = UUID.randomUUID().toString()
                val ahora = System.currentTimeMillis()
                val subtotalGeneral = lineas.sumOf { it.subtotal }
                val impuestosGeneral = lineas.sumOf {
                    it.subtotal * (it.producto.impuestoPorcentaje / 100.0)
                }
                val totalGeneral = subtotalGeneral + impuestosGeneral
                val cambio = if (metodoPago == MetodoPago.EFECTIVO && montoRecibido != null) {
                    (montoRecibido - totalGeneral).coerceAtLeast(0.0)
                } else null

                val usuarioActual = app.sessionManager.usuarioActual.value

                val venta = VentaEntity(
                    id = ventaId,
                    fecha = ahora,
                    subtotal = subtotalGeneral,
                    impuestos = impuestosGeneral,
                    total = totalGeneral,
                    metodoPago = metodoPago,
                    montoRecibido = montoRecibido,
                    cambio = cambio,
                    cajaId = "caja-principal", // TODO: soporte multi-caja en un paso futuro
                    usuarioId = usuarioActual?.id
                )

                val detalles = lineas.map { linea ->
                    DetalleVentaEntity(
                        id = UUID.randomUUID().toString(),
                        ventaId = ventaId,
                        productoId = linea.producto.id,
                        nombreProducto = nombreCompletoVariante(linea.producto),
                        cantidad = linea.cantidad,
                        precioUnitario = linea.precioUnitario,
                        precioCompraUnitario = linea.producto.precioCompra,
                        impuestoPorcentaje = linea.producto.impuestoPorcentaje,
                        subtotal = linea.subtotal
                    )
                }

                ventaDao.registrarVentaCompleta(venta, detalles)

                _estadoCobro.value = EstadoCobro.Exitoso(ventaId, venta.folio)
                vaciarCarrito()
            } catch (e: StockInsuficienteException) {
                _estadoCobro.value = EstadoCobro.Error(
                    "Stock insuficiente para \"${e.nombreProducto}\": disponible ${e.stockDisponible}, solicitado ${e.cantidadSolicitada}"
                )
            } catch (e: Exception) {
                _estadoCobro.value = EstadoCobro.Error("No se pudo completar la venta. Intenta de nuevo.")
            }
        }
    }

    fun reiniciarEstadoCobro() {
        _estadoCobro.value = EstadoCobro.Inactivo
    }

    // ---- Boleta: compartir PDF (A4) e imprimir en térmica (58mm) ----

    /** Recupera la venta + detalles ya guardados y la configuración de tienda vigente. */
    private suspend fun obtenerDatosBoleta(ventaId: String): Triple<VentaEntity, List<DetalleVentaEntity>, ConfiguracionTiendaEntity?>? {
        val venta = ventaDao.obtenerPorId(ventaId) ?: return null
        val detalles = ventaDao.obtenerDetallesDeVenta(ventaId)
        val configuracion = configuracionTiendaDao.obtener()
        return Triple(venta, detalles, configuracion)
    }

    /** Genera el PDF en tamaño A4 en caché y devuelve su Uri (vía FileProvider) lista para compartir. */
    fun generarPdfParaCompartir(ventaId: String) {
        viewModelScope.launch {
            _estadoBoleta.value = EstadoBoleta.GenerandoPdf
            try {
                val datos = obtenerDatosBoleta(ventaId)
                if (datos == null) {
                    _estadoBoleta.value = EstadoBoleta.Error("No se encontró la venta.")
                    return@launch
                }
                val (venta, detalles, configuracion) = datos

                val uri = withContext(Dispatchers.IO) {
                    val carpeta = File(app.cacheDir, "boletas").apply { mkdirs() }
                    val folioTexto = venta.folio?.toString()?.padStart(6, '0') ?: venta.id.take(8)
                    val archivo = File(carpeta, "boleta_$folioTexto.pdf")
                    GeneradorBoletaPdf.generar(archivo, configuracion, venta, detalles)
                    FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", archivo)
                }
                _estadoBoleta.value = EstadoBoleta.PdfListo(uri)
            } catch (e: Exception) {
                _estadoBoleta.value = EstadoBoleta.Error("No se pudo generar el PDF de la boleta.")
            }
        }
    }

    /** Busca impresoras Bluetooth cercanas (emparejadas y nuevas) para que el usuario elija una. */
    fun buscarImpresoras() {
        if (!impresora.bluetoothDisponible()) {
            _estadoBoleta.value = EstadoBoleta.Error("Este dispositivo no tiene Bluetooth.")
            return
        }
        if (!impresora.bluetoothActivado()) {
            _estadoBoleta.value = EstadoBoleta.Error("Activa el Bluetooth para buscar la impresora.")
            return
        }
        viewModelScope.launch {
            _estadoBoleta.value = EstadoBoleta.BuscandoImpresoras
            val encontrados = mutableListOf<DispositivoBluetooth>()
            try {
                withContext(Dispatchers.IO) {
                    impresora.buscarDispositivos().collect { dispositivo ->
                        if (encontrados.none { it.direccionMac == dispositivo.direccionMac }) {
                            encontrados.add(dispositivo)
                            _estadoBoleta.value = EstadoBoleta.ImpresorasEncontradas(encontrados.toList())
                        }
                    }
                }
            } catch (e: Exception) {
                if (encontrados.isEmpty()) {
                    _estadoBoleta.value = EstadoBoleta.Error("No se pudo buscar impresoras Bluetooth.")
                }
            }
        }
    }

    fun detenerBusquedaImpresoras() {
        impresora.detenerBusqueda()
    }

    /**
     * Imprime la boleta en la impresora térmica elegida y, si se imprime con éxito,
     * la recuerda como predeterminada en la configuración de tienda para la próxima vez.
     */
    fun imprimirEn(ventaId: String, dispositivo: DispositivoBluetooth) {
        viewModelScope.launch {
            _estadoBoleta.value = EstadoBoleta.Imprimiendo
            try {
                val datos = obtenerDatosBoleta(ventaId)
                if (datos == null) {
                    _estadoBoleta.value = EstadoBoleta.Error("No se encontró la venta.")
                    return@launch
                }
                val (venta, detalles, configuracion) = datos
                val ticket = GeneradorTicketEscPos.generar(configuracion, venta, detalles)

                val resultado = withContext(Dispatchers.IO) {
                    impresora.imprimir(dispositivo.direccionMac, ticket)
                }

                when (resultado) {
                    is ResultadoImpresion.Exitoso -> {
                        recordarImpresora(configuracion, dispositivo)
                        _estadoBoleta.value = EstadoBoleta.ImpresionExitosa
                    }
                    is ResultadoImpresion.Error -> {
                        _estadoBoleta.value = EstadoBoleta.Error(resultado.mensaje)
                    }
                }
            } catch (e: Exception) {
                _estadoBoleta.value = EstadoBoleta.Error("No se pudo imprimir la boleta.")
            }
        }
    }

    /** Reimprime directo con la última impresora recordada, sin pedir que la elija de nuevo. */
    fun imprimirConUltimaImpresora(ventaId: String) {
        viewModelScope.launch {
            val configuracion = configuracionTiendaDao.obtener()
            val mac = configuracion?.macImpresora
            val nombre = configuracion?.nombreImpresora
            if (mac == null) {
                buscarImpresoras()
                return@launch
            }
            imprimirEn(ventaId, DispositivoBluetooth(nombre ?: "Impresora", mac, yaEmparejado = true))
        }
    }

    fun hayImpresoraRecordada(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(configuracionTiendaDao.obtener()?.macImpresora != null)
        }
    }

    private suspend fun recordarImpresora(configuracionActual: ConfiguracionTiendaEntity?, dispositivo: DispositivoBluetooth) {
        val base = configuracionActual ?: ConfiguracionTiendaEntity()
        configuracionTiendaDao.guardar(
            base.copy(
                macImpresora = dispositivo.direccionMac,
                nombreImpresora = dispositivo.nombre,
                actualizadoEn = System.currentTimeMillis()
            )
        )
    }

    fun reiniciarEstadoBoleta() {
        _estadoBoleta.value = EstadoBoleta.Inactivo
    }

    private fun nombreCompletoVariante(producto: ProductoEntity): String {
        val partes = listOfNotNull(
            producto.talla?.let { "Talla $it" },
            producto.color
        )
        return if (partes.isEmpty()) producto.nombre else "${producto.nombre} (${partes.joinToString(" / ")})"
    }
}
