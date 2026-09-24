package com.tuempresa.possystem.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Paleta y tokens visuales de la app: fondo casi negro con un matiz cálido,
 * tarjetas en un gris ligeramente más claro, acento ámbar para precios y
 * acciones de cobro (más cálido y propio que un lila genérico), verde para
 * confirmaciones y montos positivos, salmón para alertas/eliminar.
 *
 * Todas las pantallas nuevas y migradas deben importar estos tokens en vez de
 * declarar su propia paleta local, para que un cambio de marca futuro sea un
 * solo archivo.
 */
object EcoPosColors {
    val FondoNegro = Color(0xFF15161A)
    val FondoTarjeta = Color(0xFF1E2024)
    val FondoTarjetaClara = Color(0xFF282A2F)
    val FondoInput = Color(0xFF2A2C31)
    val FondoNavBar = Color(0xFF101114)

    val VerdeMenta = Color(0xFF5FAD82)
    val VerdeMentaOscuro = Color(0xFF4B8F69)
    val RojoSalmon = Color(0xFFD97D6E)
    val AcentoAmbar = Color(0xFFD4A15C) // acento principal: ámbar cálido (precios, CTA de cobro/confirmar)
    val AmbarCategoria = Color(0xFFD4A15C)
    val AzulCeleste = Color(0xFF6FA3B8)

    val TextoBlanco = Color(0xFFE8E6E1)
    val TextoGris = Color(0xFFAFAEA9)
    val TextoGrisApagado = Color(0xFF8F9199)
    val ColorError = Color(0xFFD97D6E)
    val ColorExito = VerdeMenta
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
