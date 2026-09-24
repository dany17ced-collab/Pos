package com.tuempresa.possystem.presentation.movimientos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.TipoMovimiento
import com.tuempresa.possystem.presentation.clientes.CabeceraSimple
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun colorParaTipo(tipo: TipoMovimiento): Color = when (tipo) {
    TipoMovimiento.VENTA -> EcoPosColors.VerdeMenta
    TipoMovimiento.ENTRADA -> EcoPosColors.AzulCeleste
    TipoMovimiento.SALIDA -> EcoPosColors.RojoSalmon
    TipoMovimiento.AJUSTE -> EcoPosColors.AcentoAmbar
    TipoMovimiento.DEVOLUCION -> EcoPosColors.AmbarCategoria
    TipoMovimiento.CAMBIO_SALIDA -> EcoPosColors.VerdeMentaOscuro
}

private fun emojiParaTipo(tipo: TipoMovimiento): String = when (tipo) {
    TipoMovimiento.VENTA -> "🧾"
    TipoMovimiento.ENTRADA -> "📥"
    TipoMovimiento.SALIDA -> "📤"
    TipoMovimiento.AJUSTE -> "🛠️"
    TipoMovimiento.DEVOLUCION -> "↩️"
    TipoMovimiento.CAMBIO_SALIDA -> "🔁"
}

private fun etiquetaParaTipo(tipo: TipoMovimiento): String = when (tipo) {
    TipoMovimiento.VENTA -> "Venta"
    TipoMovimiento.ENTRADA -> "Entrada"
    TipoMovimiento.SALIDA -> "Salida"
    TipoMovimiento.AJUSTE -> "Ajuste"
    TipoMovimiento.DEVOLUCION -> "Devolución"
    TipoMovimiento.CAMBIO_SALIDA -> "Cambio"
}

/**
 * Pantalla de movimientos de inventario — descripción gráfica de todo lo que
 * mueve el stock (ventas, entradas, salidas, ajustes, devoluciones y
 * cambios), con un gráfico de barras apiladas por día y el detalle debajo
 * para poder auditar. Solo Admin: cruza cada movimiento con su producto para
 * quedarse únicamente con los de la tienda activa.
 */
@Composable
fun PantallaMovimientos(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: MovimientosViewModel = viewModel(factory = fabricaSimple { MovimientosViewModel(app) })

    val rango by viewModel.rango.collectAsState()
    val filtroTipo by viewModel.filtroTipo.collectAsState()
    val puntosGrafico by viewModel.puntosGrafico.collectAsState()
    val totales by viewModel.totales.collectAsState()
    val movimientos by viewModel.movimientosFiltrados.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoPosColors.FondoNegro)
    ) {
        CabeceraSimple(titulo = "Movimientos", onVolver = onVolver)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RangoMovimientos.values().forEach { opcion ->
                        ChipRango(
                            texto = opcion.etiqueta,
                            seleccionado = rango == opcion,
                            onClick = { viewModel.seleccionarRango(opcion) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TarjetaTotal(valor = totales.ventas, etiqueta = "Vendido", color = EcoPosColors.VerdeMenta, modifier = Modifier.weight(1f))
                    TarjetaTotal(valor = totales.entradas, etiqueta = "Entradas", color = EcoPosColors.AzulCeleste, modifier = Modifier.weight(1f))
                    TarjetaTotal(valor = totales.salidas, etiqueta = "Mermas", color = EcoPosColors.RojoSalmon, modifier = Modifier.weight(1f))
                }
            }

            item {
                if (cargando) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EcoPosColors.AcentoAmbar)
                    }
                } else if (puntosGrafico.all { it.total == 0 }) {
                    Text(
                        "Sin movimientos en este período.",
                        color = EcoPosColors.TextoGrisApagado,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                } else {
                    GraficoBarrasApiladas(
                        puntos = puntosGrafico,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(220.dp)
                    )
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ChipFiltro(
                            emoji = null,
                            texto = "Todos",
                            color = EcoPosColors.TextoBlanco,
                            seleccionado = filtroTipo == null,
                            onClick = { viewModel.seleccionarFiltro(null) }
                        )
                    }
                    items(TipoMovimiento.values().toList()) { tipo ->
                        ChipFiltro(
                            emoji = emojiParaTipo(tipo),
                            texto = etiquetaParaTipo(tipo),
                            color = colorParaTipo(tipo),
                            seleccionado = filtroTipo == tipo,
                            onClick = { viewModel.seleccionarFiltro(tipo) }
                        )
                    }
                }
            }

            if (movimientos.isEmpty() && !cargando) {
                item {
                    Text(
                        "No hay movimientos para este filtro.",
                        color = EcoPosColors.TextoGrisApagado,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            items(movimientos, key = { it.movimiento.id }) { item ->
                FilaMovimiento(item = item)
            }
        }
    }
}

@Composable
private fun ChipRango(texto: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(EcoPosShapes.Chip)
            .background(if (seleccionado) EcoPosColors.AcentoAmbar else EcoPosColors.FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            color = if (seleccionado) EcoPosColors.TextoBlanco else EcoPosColors.TextoGrisApagado,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChipFiltro(emoji: String?, texto: String, color: Color, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(EcoPosShapes.Chip)
            .background(if (seleccionado) color.copy(alpha = 0.18f) else EcoPosColors.FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
        } else {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color).padding(end = 6.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            texto,
            color = if (seleccionado) color else EcoPosColors.TextoGrisApagado,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TarjetaTotal(valor: Int, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(EcoPosShapes.Tarjeta)
            .background(EcoPosColors.FondoTarjeta)
            .padding(14.dp)
    ) {
        Text("$valor", color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(etiqueta, color = EcoPosColors.TextoGrisApagado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun FilaMovimiento(item: MovimientoConProducto) {
    val formatoHora = androidx.compose.runtime.remember { SimpleDateFormat("dd/MM HH:mm", Locale("es", "PE")) }
    val tipo = item.movimiento.tipo
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorParaTipo(tipo).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emojiParaTipo(tipo), fontSize = 16.sp)
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(item.nombreProducto, color = EcoPosColors.TextoBlanco, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(
                "${etiquetaParaTipo(tipo)} · ${formatoHora.format(Date(item.movimiento.fecha))}",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 11.5.sp
            )
        }
        val signo = if (tipo == TipoMovimiento.VENTA || tipo == TipoMovimiento.SALIDA || tipo == TipoMovimiento.CAMBIO_SALIDA) "-" else "+"
        Text(
            "$signo${item.movimiento.cantidad}",
            color = colorParaTipo(tipo),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun GraficoBarrasApiladas(puntos: List<PuntoGrafico>, modifier: Modifier = Modifier) {
    val maximo = (puntos.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
    val ordenTipos = TipoMovimiento.values().toList()

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (puntos.isEmpty()) return@Canvas
            val anchoDisponible = size.width
            val altoDisponible = size.height
            val espacioEntreBarras = anchoDisponible * 0.02f
            val anchoBarra = (anchoDisponible - espacioEntreBarras * (puntos.size + 1)) / puntos.size

            // Líneas guía horizontales suaves
            val lineas = 4
            repeat(lineas + 1) { i ->
                val y = altoDisponible * i / lineas
                drawLine(
                    color = EcoPosColors.FondoTarjetaClara,
                    start = Offset(0f, y),
                    end = Offset(anchoDisponible, y),
                    strokeWidth = 1f
                )
            }

            puntos.forEachIndexed { indice, punto ->
                val x = espacioEntreBarras + indice * (anchoBarra + espacioEntreBarras)
                var yAcumulada = altoDisponible

                if (punto.total == 0) {
                    // barra vacía: solo una línea base tenue para marcar el día
                    drawRoundRect(
                        color = EcoPosColors.FondoTarjetaClara,
                        topLeft = Offset(x, altoDisponible - 3f),
                        size = Size(anchoBarra, 3f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                } else {
                    ordenTipos.forEach { tipo ->
                        val cantidad = punto.porTipo[tipo] ?: 0
                        if (cantidad > 0) {
                            val alturaSegmento = (cantidad.toFloat() / maximo) * altoDisponible
                            yAcumulada -= alturaSegmento
                            drawRoundRect(
                                color = colorParaTipo(tipo),
                                topLeft = Offset(x, yAcumulada),
                                size = Size(anchoBarra, alturaSegmento),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            val anchoEtiqueta = 1f / puntos.size.coerceAtLeast(1)
            puntos.forEach { punto ->
                Text(
                    punto.etiquetaDia,
                    color = EcoPosColors.TextoGrisApagado,
                    fontSize = 9.5.sp,
                    modifier = Modifier.weight(anchoEtiqueta),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ordenTipos.forEach { tipo ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorParaTipo(tipo)))
                    Text(
                        etiquetaParaTipo(tipo),
                        color = EcoPosColors.TextoGrisApagado,
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
