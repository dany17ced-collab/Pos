package com.tuempresa.possystem.domain.boleta

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID

data class DispositivoBluetooth(
    val nombre: String,
    val direccionMac: String,
    val yaEmparejado: Boolean
)

sealed class ResultadoImpresion {
    object Exitoso : ResultadoImpresion()
    data class Error(val mensaje: String) : ResultadoImpresion()
}

/**
 * Maneja el descubrimiento y la conexión Bluetooth SPP (Serial Port Profile)
 * con impresoras térmicas ESC/POS. La mayoría de impresoras baratas de 58mm
 * usan el UUID estándar de SPP para emular un puerto serie por Bluetooth.
 */
class ImpresoraTermicaBluetooth(private val contexto: Context) {

    companion object {
        private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val adaptador: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    fun bluetoothDisponible(): Boolean = adaptador != null

    fun bluetoothActivado(): Boolean = adaptador?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun dispositivosEmparejados(): List<DispositivoBluetooth> {
        val adaptadorLocal = adaptador ?: return emptyList()
        return try {
            adaptadorLocal.bondedDevices.map {
                DispositivoBluetooth(
                    nombre = it.name ?: "Dispositivo sin nombre",
                    direccionMac = it.address,
                    yaEmparejado = true
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /**
     * Busca dispositivos Bluetooth cercanos (emparejados y nuevos) durante el
     * escaneo. Requiere BLUETOOTH_SCAN/BLUETOOTH_CONNECT (Android 12+) o
     * ACCESS_FINE_LOCATION (versiones anteriores), ya solicitados en el manifest.
     */
    @SuppressLint("MissingPermission")
    fun buscarDispositivos(): Flow<DispositivoBluetooth> = callbackFlow {
        val adaptadorLocal = adaptador
        if (adaptadorLocal == null) {
            close()
            return@callbackFlow
        }

        // Primero se emiten los ya emparejados, para que aparezcan de inmediato.
        dispositivosEmparejados().forEach { trySend(it) }

        val receptor = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_FOUND) {
                    val dispositivo = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    dispositivo?.let {
                        try {
                            trySend(
                                DispositivoBluetooth(
                                    nombre = it.name ?: "Dispositivo sin nombre",
                                    direccionMac = it.address,
                                    yaEmparejado = it.bondState == BluetoothDevice.BOND_BONDED
                                )
                            )
                        } catch (e: SecurityException) {
                            // Sin permiso para leer el nombre: se ignora este dispositivo.
                        }
                    }
                }
            }
        }

        val filtro = IntentFilter(BluetoothDevice.ACTION_FOUND)
        contexto.registerReceiver(receptor, filtro)

        try {
            if (adaptadorLocal.isDiscovering) adaptadorLocal.cancelDiscovery()
            adaptadorLocal.startDiscovery()
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            try {
                contexto.unregisterReceiver(receptor)
                if (adaptadorLocal.isDiscovering) adaptadorLocal.cancelDiscovery()
            } catch (e: Exception) {
                // El receiver puede ya estar desregistrado si el flow se canceló temprano.
            }
        }
    }

    fun detenerBusqueda() {
        try {
            if (adaptador?.isDiscovering == true) adaptador.cancelDiscovery()
        } catch (e: SecurityException) {
            // Sin permiso: no hay nada que hacer, no es un error fatal.
        }
    }

    /**
     * Conecta por SPP con la impresora y le envía los bytes del ticket ya
     * armados por GeneradorTicketEscPos. Es una operación bloqueante — debe
     * llamarse desde un hilo de fondo (Dispatchers.IO).
     */
    @SuppressLint("MissingPermission")
    fun imprimir(direccionMac: String, datos: ByteArray): ResultadoImpresion {
        val adaptadorLocal = adaptador
            ?: return ResultadoImpresion.Error("Este dispositivo no tiene Bluetooth.")

        if (!adaptadorLocal.isEnabled) {
            return ResultadoImpresion.Error("Activa el Bluetooth para poder imprimir.")
        }

        var socket: BluetoothSocket? = null
        return try {
            adaptadorLocal.cancelDiscovery()
            val dispositivo = adaptadorLocal.getRemoteDevice(direccionMac)
            socket = dispositivo.createRfcommSocketToServiceRecord(UUID_SPP)
            socket.connect()
            socket.outputStream.write(datos)
            socket.outputStream.flush()
            ResultadoImpresion.Exitoso
        } catch (e: IOException) {
            ResultadoImpresion.Error("No se pudo conectar con la impresora. Verifica que esté encendida y cerca.")
        } catch (e: SecurityException) {
            ResultadoImpresion.Error("Falta permiso de Bluetooth para imprimir.")
        } finally {
            try {
                socket?.close()
            } catch (e: IOException) {
                // Ya se envió el ticket; un error al cerrar el socket no debe reportarse como fallo de impresión.
            }
        }
    }
}
