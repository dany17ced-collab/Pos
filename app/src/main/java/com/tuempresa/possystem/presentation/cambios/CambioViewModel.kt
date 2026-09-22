package com.tuempresa.possystem.presentation.cambios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.StockInsuficienteException
import com.tuempresa.possystem.data.local.entity.CambioEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MotivoCambio
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.data.local.entity.VentaEntity
import com.tuempresa.possystem.domain.boleta.PREFIJO_QR_VENTA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Resultado de buscar la venta original, por QR o por folio manual. */
sealed class ResultadoBusquedaVenta {
    object SinBuscar : ResultadoBusquedaVenta()
    object Buscando : ResultadoBusquedaVenta()
    object NoEncontrada : ResultadoBusquedaVenta()
    data class Encontrada(val venta: VentaEntity, val detalles: List<DetalleVentaEntity>) : ResultadoBusquedaVenta()
}

/**
 * Si el plazo correspondiente ya venció, se pide confirmación explícita al
 * vendedor antes de continuar — en cualquier otro caso el flujo sigue directo.
 */
sealed class EstadoValidacionPlazo {
    object DentroDePlazo : EstadoValidacionPlazo()
    data class PlazoVencido(val motivo: MotivoCambio, val diasTranscurridos: Int, val diasPermitidos: Int) : EstadoValidacionPlazo()
}

sealed class EstadoRegistroCambio {
    object Inactivo : EstadoRegistroCambio()
    object Guardando : EstadoRegistroCambio()
    data class Exitoso(val cambioId: String) : EstadoRegistroCambio()
    data class Error(val mensaje: String) : EstadoRegistroCambio()
}

sealed class EstadoAnulacionCambio {
    object Inactivo : EstadoAnulacionCambio()
    object Anulando : EstadoAnulacionCambio()
    object Exitosa : EstadoAnulacionCambio()
    data class Error(val mensaje: String) : EstadoAnulacionCambio()
}

class CambioViewModel(private val app: POSApplication) : ViewModel() {

    private val ventaDao = app.database.ventaDao()
    private val productoDao = app.database.productoDao()
    private val cambioDao = app.database.cambioDao()
    private val configuracionTiendaDao = app.database.configuracionTiendaDao()

    private val _resultadoBusqueda = MutableStateFlow<ResultadoBusquedaVenta>(ResultadoBusquedaVenta.SinBuscar)
    val resultadoBusqueda: StateFlow<ResultadoBusquedaVenta> = _resultadoBusqueda.asStateFlow()

    /** Línea de la venta original elegida para devolver, o null si aún no se elige. */
    private val _lineaADevolver = MutableStateFlow<DetalleVentaEntity?>(null)
    val lineaADevolver: StateFlow<DetalleVentaEntity?> = _lineaADevolver.asStateFlow()

    /** Variantes disponibles del mismo producto padre, para elegir la nueva talla/color. */
    private val _variantesDisponibles = MutableStateFlow<List<ProductoEntity>>(emptyList())
    val variantesDisponibles: StateFlow<List<ProductoEntity>> = _variantesDisponibles.asStateFlow()

    private val _varianteElegida = MutableStateFlow<ProductoEntity?>(null)
    val varianteElegida: StateFlow<ProductoEntity?> = _varianteElegida.asStateFlow()

    private val _motivoElegido = MutableStateFlow<MotivoCambio?>(null)
    val motivoElegido: StateFlow<MotivoCambio?> = _motivoElegido.asStateFlow()

    private val _estadoValidacionPlazo = MutableStateFlow<EstadoValidacionPlazo?>(null)
    val estadoValidacionPlazo: StateFlow<EstadoValidacionPlazo?> = _estadoValidacionPlazo.asStateFlow()

    private val _estadoRegistro = MutableStateFlow<EstadoRegistroCambio>(EstadoRegistroCambio.Inactivo)
    val estadoRegistro: StateFlow<EstadoRegistroCambio> = _estadoRegistro.asStateFlow()

    private val _estadoAnulacion = MutableStateFlow<EstadoAnulacionCambio>(EstadoAnulacionCambio.Inactivo)
    val estadoAnulacion: StateFlow<EstadoAnulacionCambio> = _estadoAnulacion.asStateFlow()

    /** Procesa el contenido leído por la cámara: solo acepta QRs generados por esta misma app. */
    fun procesarCodigoEscaneado(contenido: String) {
        if (!contenido.startsWith(PREFIJO_QR_VENTA)) {
            _resultadoBusqueda.value = ResultadoBusquedaVenta.NoEncontrada
            return
        }
        val ventaId = contenido.removePrefix(PREFIJO_QR_VENTA)
        buscarVentaPorId(ventaId)
    }

    /** Búsqueda manual por folio, para cuando el cliente no trae la boleta física. */
    fun buscarPorFolio(folioTexto: String) {
        val folio = folioTexto.trim().toLongOrNull()
        if (folio == null) {
            _resultadoBusqueda.value = ResultadoBusquedaVenta.NoEncontrada
            return
        }
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusquedaVenta.Buscando
            val venta = ventaDao.obtenerPorFolio(folio)
            if (venta == null) {
                _resultadoBusqueda.value = ResultadoBusquedaVenta.NoEncontrada
            } else {
                cargarVenta(venta)
            }
        }
    }

    private fun buscarVentaPorId(ventaId: String) {
        viewModelScope.launch {
            _resultadoBusqueda.value = ResultadoBusquedaVenta.Buscando
            val venta = ventaDao.obtenerPorId(ventaId)
            if (venta == null) {
                _resultadoBusqueda.value = ResultadoBusquedaVenta.NoEncontrada
            } else {
                cargarVenta(venta)
            }
        }
    }

    private suspend fun cargarVenta(venta: VentaEntity) {
        val detalles = ventaDao.obtenerDetallesDeVenta(venta.id)
        _resultadoBusqueda.value = ResultadoBusquedaVenta.Encontrada(venta, detalles)
    }

    fun limpiarBusqueda() {
        _resultadoBusqueda.value = ResultadoBusquedaVenta.SinBuscar
        _lineaADevolver.value = null
        _variantesDisponibles.value = emptyList()
        _varianteElegida.value = null
        _motivoElegido.value = null
        _estadoValidacionPlazo.value = null
        _estadoRegistro.value = EstadoRegistroCambio.Inactivo
    }

    /** El vendedor elige qué línea de la venta original se está devolviendo. */
    fun elegirLineaADevolver(linea: DetalleVentaEntity) {
        _lineaADevolver.value = linea
        _varianteElegida.value = null
        viewModelScope.launch {
            val productoDevuelto = productoDao.obtenerPorId(linea.productoId)
            val idPadre = productoDevuelto?.productoBaseId ?: productoDevuelto?.id
            if (idPadre != null) {
                _variantesDisponibles.value = productoDao.observarVariantes(idPadre).first()
                    .filter { it.id != linea.productoId } // no ofrecer la misma variante devuelta
            }
        }
    }

    fun elegirNuevaVariante(variante: ProductoEntity) {
        _varianteElegida.value = variante
    }

    /** Calcula la diferencia a pagar (positiva) o devolver (negativa) al cliente. */
    fun calcularDiferencia(): Double {
        val linea = _lineaADevolver.value ?: return 0.0
        val variante = _varianteElegida.value ?: return 0.0
        return variante.precioVenta - linea.precioUnitario
    }

    /**
     * El vendedor elige el motivo (siempre se pregunta, sin excepción, para que
     * las fallas de fábrica queden registradas). Si el plazo correspondiente ya
     * venció, se detiene aquí y se pide confirmación antes de continuar.
     */
    fun elegirMotivo(motivo: MotivoCambio) {
        _motivoElegido.value = motivo
        viewModelScope.launch {
            val venta = (resultadoBusqueda.value as? ResultadoBusquedaVenta.Encontrada)?.venta ?: return@launch
            val configuracion = configuracionTiendaDao.obtenerPorTienda(venta.tiendaId)
            val diasPermitidos = when (motivo) {
                MotivoCambio.TALLA -> configuracion?.diasPlazoCambioTalla ?: 7
                MotivoCambio.FALLA_FABRICA -> configuracion?.diasPlazoCambioFabrica ?: 30
            }
            val diasTranscurridos = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - venta.fecha).toInt()

            _estadoValidacionPlazo.value = if (diasTranscurridos > diasPermitidos) {
                EstadoValidacionPlazo.PlazoVencido(motivo, diasTranscurridos, diasPermitidos)
            } else {
                EstadoValidacionPlazo.DentroDePlazo
            }
        }
    }

    fun cancelarPorPlazoVencido() {
        _motivoElegido.value = null
        _estadoValidacionPlazo.value = null
    }

    /** El vendedor decide autorizar el cambio pese a estar fuera de plazo. */
    fun autorizarExcepcionDePlazo() {
        _estadoValidacionPlazo.value = EstadoValidacionPlazo.DentroDePlazo
    }

    /** Registra el cambio de forma atómica: ajusta stock de ambas variantes y guarda el registro. */
    fun confirmarCambio() {
        val venta = (resultadoBusqueda.value as? ResultadoBusquedaVenta.Encontrada)?.venta ?: return
        val linea = _lineaADevolver.value ?: return
        val variante = _varianteElegida.value ?: return
        val motivo = _motivoElegido.value ?: return
        val fueraDePlazo = _estadoValidacionPlazo.value is EstadoValidacionPlazo.PlazoVencido

        viewModelScope.launch {
            _estadoRegistro.value = EstadoRegistroCambio.Guardando
            try {
                val usuarioActual = app.sessionManager.usuarioActual.value
                val cambio = CambioEntity(
                    id = UUID.randomUUID().toString(),
                    ventaOriginalId = venta.id,
                    folioVentaOriginal = venta.folio,
                    productoDevueltoId = linea.productoId,
                    nombreProductoDevuelto = linea.nombreProducto,
                    precioUnitarioDevuelto = linea.precioUnitario,
                    productoEntregadoId = variante.id,
                    nombreProductoEntregado = nombreCompletoVariante(variante),
                    precioUnitarioEntregado = variante.precioVenta,
                    cantidad = 1,
                    motivo = motivo,
                    diferencia = calcularDiferencia(),
                    fueraDePlazo = fueraDePlazo,
                    usuarioId = usuarioActual?.id
                )
                cambioDao.registrarCambio(cambio)
                _estadoRegistro.value = EstadoRegistroCambio.Exitoso(cambio.id)
            } catch (e: StockInsuficienteException) {
                _estadoRegistro.value = EstadoRegistroCambio.Error(
                    "No hay stock de \"${e.nombreProducto}\": disponible ${e.stockDisponible}"
                )
            } catch (e: Exception) {
                _estadoRegistro.value = EstadoRegistroCambio.Error("No se pudo registrar el cambio. Intenta de nuevo.")
            }
        }
    }

    fun reiniciarEstadoRegistro() {
        _estadoRegistro.value = EstadoRegistroCambio.Inactivo
    }

    /**
     * Anula por completo la venta encontrada (por QR o folio), sin importar
     * qué tan antigua sea ni quién la haya hecho: revierte el stock y la marca
     * como ANULADA (ver VentaDao.anularVenta). Es la misma operación que
     * "Anular esta venta" en Mis ventas de hoy, pero accesible aquí porque
     * este flujo ya localiza cualquier venta por QR o folio, sin filtrar por
     * fecha ni por usuario.
     */
    fun anularVentaEncontrada(motivo: String) {
        val venta = (resultadoBusqueda.value as? ResultadoBusquedaVenta.Encontrada)?.venta ?: return
        viewModelScope.launch {
            _estadoAnulacion.value = EstadoAnulacionCambio.Anulando
            try {
                ventaDao.anularVenta(venta.id, motivo)
                _estadoAnulacion.value = EstadoAnulacionCambio.Exitosa
            } catch (e: Exception) {
                _estadoAnulacion.value = EstadoAnulacionCambio.Error(
                    e.message ?: "No se pudo anular la venta. Intenta de nuevo."
                )
            }
        }
    }

    fun reiniciarEstadoAnulacion() {
        _estadoAnulacion.value = EstadoAnulacionCambio.Inactivo
    }

    private fun nombreCompletoVariante(producto: ProductoEntity): String {
        val partes = listOfNotNull(
            producto.talla?.let { "Talla $it" },
            producto.color
        )
        return if (partes.isEmpty()) producto.nombre else "${producto.nombre} (${partes.joinToString(" / ")})"
    }
}
