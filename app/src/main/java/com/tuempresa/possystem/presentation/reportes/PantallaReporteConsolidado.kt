package com.tuempresa.possystem.presentation.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.theme.EcoPosColors

/**
 * Vista de un vistazo de cómo va cada tienda hoy: ventas del día y alertas de
 * stock bajo, una fila por sucursal. Exclusivo ADMIN — el vendedor solo ve el
 * detalle de su propia tienda activa en el resto de la app.
 */
@Composable
fun PantallaReporteConsolidado(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: ReporteConsolidadoViewModel =
        viewModel(factory = fabricaViewModel(app) { ReporteConsolidadoViewModel(app) })
    val resumenes by viewModel.resumenes.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    Scaffold(containerColor = EcoPosColors.FondoNegro) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabeceraSimple(titulo = "Reporte consolidado", subtitulo = "Ventas y stock de todas tus tiendas", onVolver = onVolver)

            Text(
                "Ventas de hoy y stock bajo de todas tus tiendas activas.",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
            )

            when {
                cargando -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EcoPosColors.AcentoAmbar)
                    }
                }
                resumenes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay tiendas activas.", color = EcoPosColors.TextoGrisApagado, fontSize = 14.sp)
                    }
                }
                else -> {
                    val totalHoy = resumenes.sumOf { it.totalVentasHoy }
                    val totalStockBajo = resumenes.sumOf { it.productosStockBajo }

                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                        item {
                            TarjetaTotalGeneral(totalHoy = totalHoy, totalStockBajo = totalStockBajo)
                            Text(
                                "Por tienda",
                                color = EcoPosColors.TextoBlanco,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                            )
                        }
                        items(resumenes, key = { it.tienda.id }) { resumen ->
                            FilaResumenTienda(resumen)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaTotalGeneral(totalHoy: Double, totalStockBajo: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoPosColors.FondoTarjeta)
            .padding(18.dp)
    ) {
        Text("Ventas de hoy (todas las tiendas)", color = EcoPosColors.TextoGrisApagado, fontSize = 12.sp)
        Text(
            "S/ ${"%.2f".format(totalHoy)}",
            color = EcoPosColors.TextoBlanco,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        if (totalStockBajo > 0) {
            Text(
                "$totalStockBajo productos con stock bajo en total",
                color = EcoPosColors.ColorError,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun FilaResumenTienda(resumen: ResumenTienda) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EcoPosColors.FondoTarjeta)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(resumen.tienda.nombre, color = EcoPosColors.TextoBlanco, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            Text("${resumen.numeroVentasHoy} ventas hoy", color = EcoPosColors.TextoGrisApagado, fontSize = 12.sp)
            if (resumen.productosStockBajo > 0) {
                Text(
                    "${resumen.productosStockBajo} con stock bajo",
                    color = EcoPosColors.ColorError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Text(
            "S/ ${"%.2f".format(resumen.totalVentasHoy)}",
            color = EcoPosColors.VerdeMenta,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
