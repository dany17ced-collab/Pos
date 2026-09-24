package com.tuempresa.possystem.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Paleta y tokens visuales de la app, calcados de los mockups aprobados:
 * fondo gris-carbón oscuro (#1C1D21), tarjetas en gris más claro (#232429),
 * y acentos "vivos" saturados por función — rosa para la acción principal de
 * vender, azul para info/inventario, ámbar para alertas y dinero, verde para
 * montos positivos, morado para el rol administrador. Cada categoría de
 * producto tiene su propio color vivo para distinguirla de un vistazo.
 *
 * Todas las pantallas nuevas y migradas deben importar estos tokens en vez de
 * declarar su propia paleta local, para que un cambio de marca futuro sea un
 * solo archivo.
 */
object EcoPosColors {
    val FondoNegro = Color(0xFF1C1D21)
    val FondoTarjeta = Color(0xFF232429)
    val FondoTarjetaClara = Color(0xFF2A2B31)
    val FondoInput = Color(0xFF26272D)
    val FondoBorde = Color(0xFF34353C)
    val FondoNavBar = Color(0xFF202126)

    // Acentos vivos, tal como en los mockups aprobados.
    val RosaVivo = Color(0xFFFF2E63) // acción principal: Nueva venta, tab activo, categoría 1
    val RosaVivoClaro = Color(0xFFFF5470)
    val AzulVivo = Color(0xFF3EC1FF) // info, inventario, categoría 2, badge "modo vendedor"
    val AzulVivoOscuro = Color(0xFF1A8CFF)
    val AmbarVivo = Color(0xFFFFC93E) // alertas, stats secundarios, categoría 3
    val AmbarVivoOscuro = Color(0xFFFF9F1C)
    val VerdeVivo = Color(0xFF3EFFB0) // montos positivos, cobrar, categoría 4
    val VerdeVivoOscuro = Color(0xFF14D68C)
    val MoradoVivo = Color(0xFFB57BFF) // rol administrador
    val MoradoVivoOscuro = Color(0xFF8A3EFF)
    val RojoVivo = Color(0xFFFF6B8A) // alertas fuertes, vendedores activos ahora

    val VerdeMenta = VerdeVivo
    val VerdeMentaOscuro = VerdeVivoOscuro
    val RojoSalmon = RosaVivo
    val AcentoAmbar = AmbarVivo // acento principal heredado: precios, CTA de cobro/confirmar
    val AmbarCategoria = AmbarVivo
    val AzulCeleste = AzulVivo

    val TextoBlanco = Color(0xFFEDEDEF)
    val TextoGris = Color(0xFFAFAEA9)
    val TextoGrisApagado = Color(0xFF8B8D96)
    val ColorError = RojoVivo
    val ColorExito = VerdeVivo
}

object EcoPosShapes {
    val Campo = RoundedCornerShape(28.dp)
    val Tarjeta = RoundedCornerShape(16.dp)
    val TarjetaChica = RoundedCornerShape(12.dp)
    val Boton = RoundedCornerShape(28.dp)
    val Chip = RoundedCornerShape(20.dp)
}

@Composable
fun ecoPosCamposTextoColores() = TextFieldDefaults.colors(
    focusedContainerColor = EcoPosColors.FondoInput,
    unfocusedContainerColor = EcoPosColors.FondoInput,
    focusedTextColor = EcoPosColors.TextoBlanco,
    unfocusedTextColor = EcoPosColors.TextoBlanco,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = EcoPosColors.VerdeMenta,
    focusedPlaceholderColor = EcoPosColors.TextoGrisApagado,
    unfocusedPlaceholderColor = EcoPosColors.TextoGrisApagado
)
