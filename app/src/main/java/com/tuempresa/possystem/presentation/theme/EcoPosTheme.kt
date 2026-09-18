package com.tuempresa.possystem.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Paleta y tokens visuales estilo "Eco POS": fondo negro puro (no gris cálido),
 * tarjetas gris oscuro neutro, acentos verde menta (positivo/CTA principal),
 * rojo salmón (gastos/eliminar) y lila-azulado (acciones secundarias/CTA de
 * navegación tipo "Nuevo orden").
 *
 * Todas las pantallas nuevas y migradas deben importar estos tokens en vez de
 * declarar su propia paleta local, para que un cambio de marca futuro sea un
 * solo archivo.
 */
object EcoPosColors {
    val FondoNegro = Color(0xFF000000)
    val FondoTarjeta = Color(0xFF1C1C1E)
    val FondoTarjetaClara = Color(0xFF2C2C2E)
    val FondoInput = Color(0xFF3A3A3C)
    val FondoNavBar = Color(0xFF0A0A0A)

    val VerdeMenta = Color(0xFF8FD9A8)
    val VerdeMentaOscuro = Color(0xFF6FBF8A)
    val RojoSalmon = Color(0xFFE88B85)
    val LilaAzulado = Color(0xFF7B8FD4)
    val AmbarCategoria = Color(0xFFAD8A3D)
    val AzulCeleste = Color(0xFF6FB8E0)

    val TextoBlanco = Color(0xFFFFFFFF)
    val TextoGris = Color(0xFFAEAEB2)
    val TextoGrisApagado = Color(0xFF8E8E93)
    val ColorError = Color(0xFFE08585)
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
