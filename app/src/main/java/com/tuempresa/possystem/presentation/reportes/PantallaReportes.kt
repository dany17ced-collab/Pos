package com.tuempresa.possystem.presentation.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.DatePickerDialog
import android.content.Intent
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.domain.reportes.PeriodoReporte
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.AcentoAmbar
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError

/**
 * Reportes de la tienda activa (exclusivo ADMIN): exportar Excel/PDF y ver
 * el ranking de más vendidos. El cierre de caja del día a día vive en
 * PantallaCierreCaja (accesible también para VENDEDOR); si el negocio tiene
 * más de una tienda, "Reporte consolidado" arriba lleva al resumen de todas.
 */
@Composable
fun PantallaReportes(
    app: POSApplication,
    onVolver: () -> Unit,
    onReporteConsolidado: () -> Unit,
    onCierreCaja: () -> Unit
) {
    val viewModel: ReportesViewModel = viewModel(factory = fabricaSimple { ReportesViewModel(app) })

    val masVendidos by viewModel.masVendidosHoy.collectAsState()
    val estadoExportacion by viewModel.estadoExportacion.collectAsState()
    val contexto = LocalContext.current

    var fechaDesdePersonalizada by remember { mutableStateOf<Long?>(null) }
    var fechaHastaPersonalizada by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(estadoExportacion) {
        val estado = estadoExportacion
        if (estado is EstadoExportacion.Exitoso) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (estado.nombreArchivo.endsWith(".pdf")) "application/pdf"
                else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, estado.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            contexto.startActivity(Intent.createChooser(intent, "Compartir reporte"))
            viewModel.limpiarEstadoExportacion()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            com.tuempresa.possystem.presentation.clientes.CabeceraSimple(
                titulo = "Reportes",
                subtitulo = "Exporta tus ventas e inventario de esta tienda",
                onVolver = onVolver
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FondoTarjeta)
                    .clickable(onClick = onCierreCaja)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cierre de caja", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Corte X/Z del turno de esta tienda", color = TextoCremaApagado, fontSize = 12.sp)
                }
                Text("›", color = AcentoTerracota, fontSize = 20.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FondoTarjeta)
                    .clickable(onClick = onReporteConsolidado)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reporte consolidado", color = TextoCrema, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Ventas y stock bajo de todas tus tiendas", color = TextoCremaApagado, fontSize = 12.sp)
                }
                Text("›", color = AcentoTerracota, fontSize = 20.sp)
            }

            Text(
                "Exportar reporte detallado",
                color = TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 4.dp)
            )
            Text(
                "Incluye ventas por día, productos más vendidos, cortes de caja e inventario valorizado de esta tienda.",
                color = TextoCremaApagado,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            )

            val exportando = estadoExportacion is EstadoExportacion.Generando
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Últimos 7 días", color = TextoCremaApagado, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonExportar(
                        texto = "Excel",
                        habilitado = !exportando,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.exportarReporte(PeriodoReporte.SEMANAL, FormatoExportacion.EXCEL) }
                    BotonExportar(
                        texto = "PDF",
                        habilitado = !exportando,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.exportarReporte(PeriodoReporte.SEMANAL, FormatoExportacion.PDF) }
                }

                Text(
                    "Últimos 30 días",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonExportar(
                        texto = "Excel",
                        habilitado = !exportando,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.exportarReporte(PeriodoReporte.MENSUAL, FormatoExportacion.EXCEL) }
                    BotonExportar(
                        texto = "PDF",
                        habilitado = !exportando,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.exportarReporte(PeriodoReporte.MENSUAL, FormatoExportacion.PDF) }
                }

                Text(
                    "Rango personalizado",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                SelectorRangoFechas(
                    fechaDesde = fechaDesdePersonalizada,
                    fechaHasta = fechaHastaPersonalizada,
                    onFechaDesdeSeleccionada = { fechaDesdePersonalizada = it },
                    onFechaHastaSeleccionada = { fechaHastaPersonalizada = it }
                )

                val desdeElegida = fechaDesdePersonalizada
                val hastaElegida = fechaHastaPersonalizada
                val rangoInvertido = desdeElegida != null && hastaElegida != null && desdeElegida > hastaElegida
                val rangoListo = desdeElegida != null && hastaElegida != null && !rangoInvertido

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonExportar(
                        texto = "Excel",
                        habilitado = !exportando && rangoListo,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.exportarReporte(
                            PeriodoReporte.PERSONALIZADO,
                            FormatoExportacion.EXCEL,
                            desde = desdeElegida,
                            hasta = hastaElegida
                        )
                    }
                    BotonExportar(
                        texto = "PDF",
                        habilitado = !exportando && rangoListo,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.exportarReporte(
                            PeriodoReporte.PERSONALIZADO,
                            FormatoExportacion.PDF,
                            desde = desdeElegida,
                            hasta = hastaElegida
                        )
                    }
                }
                if (rangoInvertido) {
                    Text(
                        "La fecha \"desde\" no puede ser posterior a \"hasta\".",
                        color = ColorError,
                        fontSize = 11.5.sp
                    )
                }

                if (exportando) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AcentoTerracota,
                            modifier = Modifier.width(16.dp).height(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Generando reporte…",
                            color = TextoCremaApagado,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (estadoExportacion is EstadoExportacion.Error) {
                    Text(
                        (estadoExportacion as EstadoExportacion.Error).mensaje,
                        color = ColorError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Text(
                "Productos más vendidos hoy",
                color = TextoCrema,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
            )

            if (masVendidos.isEmpty()) {
                Text(
                    "Aún no hay ventas registradas hoy.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    masVendidos.forEachIndexed { index, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FondoTarjeta)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                color = AcentoTerracota,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.nombreProducto, color = TextoCrema, fontSize = 14.sp)
                                Text(
                                    "${p.unidadesVendidas} unidades",
                                    color = TextoCremaApagado,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                "S/ ${"%.2f".format(p.totalVendido)}",
                                color = TextoCrema,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonExportar(
    texto: String,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
    ) {
        Text(texto, color = if (habilitado) TextoCrema else TextoCremaApagado)
    }
}

/**
 * Dos campos, "Desde" y "Hasta", que abren el DatePickerDialog nativo de
 * Android al tocarlos. Cada valor es epoch millis a medianoche del día
 * elegido (el fin de "Hasta" se ajusta a las 23:59:59.999 en
 * GeneradorDatosReporte, para incluir todo ese último día).
 */
@Composable
private fun SelectorRangoFechas(
    fechaDesde: Long?,
    fechaHasta: Long?,
    onFechaDesdeSeleccionada: (Long) -> Unit,
    onFechaHastaSeleccionada: (Long) -> Unit
) {
    val contexto = LocalContext.current
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")) }

    fun abrirSelector(fechaActual: Long?, onSeleccionada: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (fechaActual != null) cal.timeInMillis = fechaActual
        DatePickerDialog(
            contexto,
            { _, anio, mes, dia ->
                val seleccion = Calendar.getInstance().apply {
                    set(anio, mes, dia, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onSeleccionada(seleccion.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CampoFecha(
            etiqueta = "Desde",
            valor = fechaDesde?.let { formato.format(java.util.Date(it)) } ?: "Elegir",
            modifier = Modifier.weight(1f)
        ) { abrirSelector(fechaDesde) { onFechaDesdeSeleccionada(it) } }

        CampoFecha(
            etiqueta = "Hasta",
            valor = fechaHasta?.let { formato.format(java.util.Date(it)) } ?: "Elegir",
            modifier = Modifier.weight(1f)
        ) { abrirSelector(fechaHasta) { onFechaHastaSeleccionada(it) } }
    }
}

@Composable
private fun CampoFecha(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(etiqueta, color = TextoCremaApagado, fontSize = 11.sp)
        Text(valor, color = TextoCrema, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}
