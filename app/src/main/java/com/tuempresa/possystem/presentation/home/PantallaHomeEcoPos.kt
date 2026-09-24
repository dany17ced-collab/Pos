package com.tuempresa.possystem.presentation.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.tuempresa.possystem.presentation.theme.EcoPosBottomNav
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

/** Un carrito guardado/en curso, mostrado en la sección "Carros activos". */
data class CarroActivo(
    val id: String,
    val etiqueta: String,
    val totalTexto: String,
    val cantidadItems: Int
)

/**
 * Pantalla principal estilo Eco POS: reemplaza el listado de tarjetas de
 * PantallaHomeVendedor/Admin. Mantiene los mismos destinos de negocio
 * (vender, inventario, reportes, etc.) pero con la disposición visual del
 * dashboard de referencia: Gastos/Ingresos arriba, "Nuevo orden" como CTA
 * principal, y carros activos debajo.
 */
@Composable
fun PantallaHomeEcoPos(
    nombreTienda: String,
    esAdmin: Boolean,
    carrosActivos: List<CarroActivo>,
    onNuevoOrden: () -> Unit,
    onGastos: () -> Unit,
    onIngresos: () -> Unit,
    onAbrirCarro: (String) -> Unit,
    onNavegarDestino: (EcoPosDestino) -> Unit
) {
    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        bottomBar = {
            EcoPosBottomNav(
                seleccionado = EcoPosDestino.VENDER,
                esAdmin = esAdmin,
                onSeleccionar = onNavegarDestino
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = nombreTienda.uppercase(),
                color = EcoPosColors.TextoBlanco,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotonAccionGrande(
                    texto = "Gastos",
                    icono = Icons.Filled.Description,
                    colorFondo = EcoPosColors.RojoSalmon,
                    colorTexto = Color(0xFF5A1F1B),
                    onClick = onGastos,
                    modifier = Modifier.weight(1f)
                )
                BotonAccionGrande(
                    texto = "Ingresos",
                    icono = Icons.Filled.AttachMoney,
                    colorFondo = EcoPosColors.VerdeMenta,
                    colorTexto = Color(0xFF1B4A2A),
                    onClick = onIngresos,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(EcoPosShapes.Tarjeta)
                    .background(EcoPosColors.AcentoAmbar)
                    .clickable(onClick = onNuevoOrden)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AddShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFF2B1E0F),
                    modifier = Modifier.size(30.dp)
                )
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        "Nuevo orden",
                        color = Color(0xFF2B1E0F),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crear nueva transacción con productos",
                        color = Color(0xFF4A3820),
                        fontSize = 13.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EcoPosColors.FondoTarjetaClara)
                    .padding(20.dp)
            ) {
                Text(
                    "CARROS ACTIVOS",
                    color = EcoPosColors.TextoGris,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (carrosActivos.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = EcoPosColors.TextoBlanco,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No tienes carrito activo",
                            color = EcoPosColors.TextoBlanco,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(carrosActivos, key = { it.id }) { carro ->
                            TarjetaCarroActivo(carro = carro, onClick = { onAbrirCarro(carro.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonAccionGrande(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    colorFondo: Color,
    colorTexto: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(EcoPosShapes.Tarjeta)
            .background(colorFondo)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colorTexto.copy(alpha = 0.15f))
                .padding(6.dp)
        ) {
            Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(18.dp))
        }
        Text(
            texto,
            color = colorTexto,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun TarjetaCarroActivo(carro: CarroActivo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(carro.etiqueta, color = EcoPosColors.TextoBlanco, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "${carro.cantidadItems} producto(s)",
                color = EcoPosColors.TextoGrisApagado,
                fontSize = 12.sp
            )
        }
        Text(carro.totalTexto, color = EcoPosColors.VerdeMenta, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
