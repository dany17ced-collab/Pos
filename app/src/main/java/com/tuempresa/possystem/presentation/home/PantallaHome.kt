package com.tuempresa.possystem.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)

data class OpcionMenu(
    val titulo: String,
    val descripcion: String,
    val ruta: String
)

@Composable
fun PantallaHomeVendedor(
    nombreUsuario: String,
    onNavegar: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val opciones = listOf(
        OpcionMenu("Vender", "Escanear y cobrar productos", "venta"),
        OpcionMenu("Inventario", "Consultar existencias por talla", "inventario_consulta"),
        OpcionMenu("Mis ventas de hoy", "Revisar y gestionar devoluciones", "mis_ventas")
    )

    PantallaHomeBase(
        saludo = "Hola, $nombreUsuario",
        etiquetaRol = "Vendedor",
        opciones = opciones,
        onNavegar = onNavegar,
        onCerrarSesion = onCerrarSesion
    )
}

@Composable
fun PantallaHomeAdmin(
    nombreUsuario: String,
    onNavegar: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val opciones = listOf(
        OpcionMenu("Vender", "Escanear y cobrar productos", "venta"),
        OpcionMenu("Inventario", "Productos, tallas, colores y precios", "inventario"),
        OpcionMenu("Reportes y caja", "Corte X/Z, historial, más vendidos", "reportes"),
        OpcionMenu("Usuarios", "Vendedores y permisos de acceso", "usuarios")
    )

    PantallaHomeBase(
        saludo = "Hola, $nombreUsuario",
        etiquetaRol = "Administrador",
        opciones = opciones,
        onNavegar = onNavegar,
        onCerrarSesion = onCerrarSesion
    )
}

@Composable
private fun PantallaHomeBase(
    saludo: String,
    etiquetaRol: String,
    opciones: List<OpcionMenu>,
    onNavegar: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = etiquetaRol.uppercase(),
                    color = AcentoTerracota,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = saludo,
                    color = TextoCrema,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(opciones) { opcion ->
                    TarjetaOpcion(opcion = opcion, onClick = { onNavegar(opcion.ruta) })
                }
            }

            Text(
                text = "Cerrar sesión",
                color = TextoCremaApagado,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCerrarSesion)
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun TarjetaOpcion(opcion: OpcionMenu, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = opcion.titulo,
                color = TextoCrema,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = opcion.descripcion,
                color = TextoCremaApagado,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
