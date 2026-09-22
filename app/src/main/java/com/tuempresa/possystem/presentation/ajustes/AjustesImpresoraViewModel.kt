package com.tuempresa.possystem.presentation.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import com.tuempresa.possystem.domain.boleta.DispositivoBluetooth
import com.tuempresa.possystem.domain.boleta.ImpresoraTermicaBluetooth
import com.tuempresa.possystem.domain.boleta.ResultadoImpresion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Impresora ya guardada como predeterminada, si existe. */
data class ImpresoraRecordada(
    val nombre: String,
    val direccionMac: String
)

sealed class EstadoBusquedaImpresora {
    object Inactivo : EstadoBusquedaImpresora()
    object Buscando : EstadoBusquedaImpresora()
    data class Encontradas(val dispositivos: List<DispositivoBluetooth>) : EstadoBusquedaImpresora()
    data class Error(val mensaje: String) : EstadoBusquedaImpresora()
}

sealed class EstadoEmparejamiento {
    object Inactivo : EstadoEmparejamiento()
    object Probando : EstadoEmparejamiento()
    object Exitoso : EstadoEmparejamiento()
    data class Error(val mensaje: String) : EstadoEmparejamiento()
}

/**
 * Ajuste ÚNICO de impresora: una vez emparejada y guardada aquí, la app la
 * recuerda automáticamente durante todo el día (y en adelante) sin volver a
 * pedirla en cada venta — ver VentaViewModel.imprimirConUltimaImpresora, que
 * lee el mismo dato de ConfiguracionTiendaEntity.macImpresora.
 */
class AjustesImpresoraViewModel(private val app: POSApplication) : ViewModel() {

    private val dao = app.database.configuracionTiendaDao()
    private val impresora = ImpresoraTermicaBluetooth(app)

    private val _impresoraRecordada = MutableStateFlow<ImpresoraRecordada?>(null)
    val impresoraRecordada: StateFlow<ImpresoraRecordada?> = _impresoraRecordada.asStateFlow()

    private val _estadoBusqueda = MutableStateFlow<EstadoBusquedaImpresora>(EstadoBusquedaImpresora.Inactivo)
    val estadoBusqueda: StateFlow<EstadoBusquedaImpresora> = _estadoBusqueda.asStateFlow()

    private val _estadoEmparejamiento = MutableStateFlow<EstadoEmparejamiento>(EstadoEmparejamiento.Inactivo)
    val estadoEmparejamiento: StateFlow<EstadoEmparejamiento> = _estadoEmparejamiento.asStateFlow()

    init {
        cargarImpresoraRecordada()
    }

    private fun cargarImpresoraRecordada() {
        viewModelScope.launch {
            val configuracion = dao.obtenerPorTienda(app.sessionManager.tiendaActivaIdRequerida())
            val mac = configuracion?.macImpresora
            _impresoraRecordada.value = if (mac != null) {
                ImpresoraRecordada(configuracion.nombreImpresora ?: "Impresora", mac)
            } else {
                null
            }
        }
    }

    fun bluetoothDisponible(): Boolean = impresora.bluetoothDisponible()
    fun bluetoothActivado(): Boolean = impresora.bluetoothActivado()

    fun buscarImpresoras() {
        if (!impresora.bluetoothDisponible()) {
            _estadoBusqueda.value = EstadoBusquedaImpresora.Error("Este dispositivo no tiene Bluetooth.")
            return
        }
        if (!impresora.bluetoothActivado()) {
            _estadoBusqueda.value = EstadoBusquedaImpresora.Error("Activa el Bluetooth para buscar la impresora.")
            return
        }
        viewModelScope.launch {
            _estadoBusqueda.value = EstadoBusquedaImpresora.Buscando
            val encontrados = mutableListOf<DispositivoBluetooth>()
            try {
                withContext(Dispatchers.IO) {
                    impresora.buscarDispositivos().collect { dispositivo ->
                        if (encontrados.none { it.direccionMac == dispositivo.direccionMac }) {
                            encontrados.add(dispositivo)
                            _estadoBusqueda.value = EstadoBusquedaImpresora.Encontradas(encontrados.toList())
                        }
                    }
                }
            } catch (e: Exception) {
                if (encontrados.isEmpty()) {
                    _estadoBusqueda.value = EstadoBusquedaImpresora.Error("No se pudo buscar impresoras Bluetooth.")
                }
            }
        }
    }

    fun detenerBusqueda() {
        impresora.detenerBusqueda()
    }

    /**
     * Empareja y guarda la impresora elegida como predeterminada. Se envía un
     * ticket de prueba corto para confirmar que realmente conecta antes de
     * darla por buena — así no se guarda "a ciegas" una impresora apagada o
     * fuera de rango.
     */
    fun emparejarYGuardar(dispositivo: DispositivoBluetooth) {
        viewModelScope.launch {
            _estadoEmparejamiento.value = EstadoEmparejamiento.Probando
            try {
                val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
                val ticketPrueba = com.tuempresa.possystem.domain.boleta.GeneradorTicketEscPos.generar(
                    configuracion = dao.obtenerPorTienda(tiendaId),
                    venta = ticketDePrueba(tiendaId),
                    detalles = emptyList()
                )
                val resultado = withContext(Dispatchers.IO) {
                    impresora.imprimir(dispositivo.direccionMac, ticketPrueba)
                }

                when (resultado) {
                    is ResultadoImpresion.Exitoso -> {
                        guardarComoPredeterminada(dispositivo)
                        _estadoEmparejamiento.value = EstadoEmparejamiento.Exitoso
                    }
                    is ResultadoImpresion.Error -> {
                        _estadoEmparejamiento.value = EstadoEmparejamiento.Error(resultado.mensaje)
                    }
                }
            } catch (e: Exception) {
                _estadoEmparejamiento.value = EstadoEmparejamiento.Error("No se pudo emparejar la impresora.")
            }
        }
    }

    /** Guarda la impresora sin imprimir ticket de prueba (por si el papel se acabó, etc.). */
    fun guardarSinProbar(dispositivo: DispositivoBluetooth) {
        viewModelScope.launch {
            guardarComoPredeterminada(dispositivo)
            _estadoEmparejamiento.value = EstadoEmparejamiento.Exitoso
        }
    }

    private suspend fun guardarComoPredeterminada(dispositivo: DispositivoBluetooth) {
        val tiendaId = app.sessionManager.tiendaActivaIdRequerida()
        val base = dao.obtenerPorTienda(tiendaId)
            ?: ConfiguracionTiendaEntity(id = java.util.UUID.randomUUID().toString(), tiendaId = tiendaId)
        dao.guardar(
            base.copy(
                macImpresora = dispositivo.direccionMac,
                nombreImpresora = dispositivo.nombre,
                actualizadoEn = System.currentTimeMillis()
            )
        )
        _impresoraRecordada.value = ImpresoraRecordada(dispositivo.nombre, dispositivo.direccionMac)
    }

    fun olvidarImpresora() {
        viewModelScope.launch {
            val existente = dao.obtenerPorTienda(app.sessionManager.tiendaActivaIdRequerida()) ?: return@launch
            dao.guardar(
                existente.copy(
                    macImpresora = null,
                    nombreImpresora = null,
                    actualizadoEn = System.currentTimeMillis()
                )
            )
            _impresoraRecordada.value = null
        }
    }

    fun reiniciarEstadoEmparejamiento() {
        _estadoEmparejamiento.value = EstadoEmparejamiento.Inactivo
    }

    private fun ticketDePrueba(tiendaId: String) = com.tuempresa.possystem.data.local.entity.VentaEntity(
        id = "prueba",
        tiendaId = tiendaId,
        fecha = System.currentTimeMillis(),
        subtotal = 0.0,
        impuestos = 0.0,
        total = 0.0,
        metodoPago = com.tuempresa.possystem.data.local.entity.MetodoPago.EFECTIVO,
        cajaId = "caja-principal"
    )
}
