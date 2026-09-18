package com.tuempresa.possystem.presentation.ajustes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.domain.boleta.DispositivoBluetooth
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.LilaAzulado
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito
/**
 * Ajuste ÚNICO de impresora: se empareja una vez aquí y queda guardada como
 * predeterminada. A partir de ese momento, cada venta la usa automáticamente
 * para imprimir el ticket (ver VentaViewModel.imprimirConUltimaImpresora) sin
 * volver a pedirla — se mantiene emparejada todo el día, y los días
 * siguientes, hasta que alguien la cambie u olvide desde aquí.
 */
@Composable
fun PantallaAjustesImpresora(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: AjustesImpresoraViewModel = viewModel(factory = fabricaSimple { AjustesImpresoraViewModel(app) })

    val impresoraRecordada by viewModel.impresoraRecordada.collectAsState()
    val estadoBusqueda by viewModel.estadoBusqueda.collectAsState()
    val estadoEmparejamiento by viewModel.estadoEmparejamiento.collectAsState()

    val context = LocalContext.current
    var accionPendientePermiso by remember { mutableStateOf<(() -> Unit)?>(null) }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.all { it }) {
            accionPendientePermiso?.invoke()
        }
        accionPendientePermiso = null
    }

    fun ejecutarConPermisoBluetooth(accion: () -> Unit) {
        if (tienePermisosBluetoothAjustes(context)) {
            accion()
        } else {
            accionPendientePermiso = accion
            lanzadorPermisos.launch(permisosBluetoothNecesariosAjustes())
        }
    }

    LaunchedEffect(estadoEmparejamiento) {
        if (estadoEmparejamiento is EstadoEmparejamiento.Exitoso) {
            viewModel.detenerBusqueda()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹ Volver",
                    color = TextoCrema,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(onClick = onVolver)
                )
            }

            Text(
                text = "Impresora",
                color = TextoCrema,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = "Empareja la impresora térmica una sola vez. La app la recordará durante todo el día " +
                    "(y los siguientes) para imprimir cada venta sin pedirla de nuevo.",
                color = TextoCremaApagado,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )

            // --- Impresora actualmente guardada ---
            if (impresoraRecordada != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FondoTarjeta)
                        .padding(16.dp)
                ) {
                    Text("Impresora predeterminada", color = TextoCremaApagado, fontSize = 12.sp)
                    Text(
                        impresoraRecordada!!.nombre,
                        color = TextoCrema,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        impresoraRecordada!!.direccionMac,
                        color = TextoCremaApagado,
                        fontSize = 12.sp
                    )
                    Text(
                        "✓ Conectada automáticamente en cada venta",
                        color = ColorExito,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    OutlinedButton(
                        onClick = { viewModel.olvidarImpresora() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text("Olvidar esta impresora")
                    }
                }
            } else {
                Text(
                    "Todavía no hay ninguna impresora guardada.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // --- Buscar y emparejar otra impresora ---
            Text(
                text = if (impresoraRecordada != null) "Cambiar de impresora" else "Buscar impresora",
                color = TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
            )

            Button(
                onClick = {
                    ejecutarConPermisoBluetooth { viewModel.buscarImpresoras() }
                },
                enabled = estadoBusqueda !is EstadoBusquedaImpresora.Buscando,
                colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    if (estadoBusqueda is EstadoBusquedaImpresora.Buscando) "Buscando…" else "Buscar impresoras Bluetooth",
                    color = FondoCarbon,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (estadoBusqueda is EstadoBusquedaImpresora.Buscando) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp, top = 10.dp)
                ) {
                    CircularProgressIndicator(
                        color = AcentoTerracota,
                        modifier = Modifier.height(16.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Asegúrate de que la impresora esté encendida y cerca…", color = TextoCremaApagado, fontSize = 12.sp)
                }
            }

            if (estadoBusqueda is EstadoBusquedaImpresora.Error) {
                Text(
                    (estadoBusqueda as EstadoBusquedaImpresora.Error).mensaje,
                    color = ColorError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                )
            }

            val dispositivos = (estadoBusqueda as? EstadoBusquedaImpresora.Encontradas)?.dispositivos.orEmpty()
            if (dispositivos.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    dispositivos.forEach { dispositivo ->
                        FilaDispositivoImpresora(
                            dispositivo = dispositivo,
                            emparejando = estadoEmparejamiento is EstadoEmparejamiento.Probando,
                            onEmparejar = {
                                ejecutarConPermisoBluetooth { viewModel.emparejarYGuardar(dispositivo) }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            when (val estado = estadoEmparejamiento) {
                is EstadoEmparejamiento.Exitoso -> {
                    Text(
                        "✓ Impresora emparejada y guardada como predeterminada.",
                        color = ColorExito,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    )
                }
                is EstadoEmparejamiento.Error -> {
                    Text(
                        estado.mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FilaDispositivoImpresora(
    dispositivo: DispositivoBluetooth,
    emparejando: Boolean,
    onEmparejar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dispositivo.nombre, color = TextoCrema, fontSize = 14.sp)
            Text(
                if (dispositivo.yaEmparejado) "Ya emparejado con el teléfono" else dispositivo.direccionMac,
                color = TextoCremaApagado,
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AcentoTerracota)
                .clickable(enabled = !emparejando, onClick = onEmparejar)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                if (emparejando) "…" else "Usar esta",
                color = FondoCarbon,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun permisosBluetoothNecesariosAjustes(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun tienePermisosBluetoothAjustes(context: Context): Boolean {
    return permisosBluetoothNecesariosAjustes().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
