package com.tuempresa.possystem.presentation.caja

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuempresa.possystem.presentation.theme.EcoPosColors

private val FondoCarbon = EcoPosColors.FondoNegro
private val TextoCrema = EcoPosColors.TextoBlanco
private val AcentoTerracota = EcoPosColors.LilaAzulado
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
/**
 * Diálogo obligatorio (sin opción de cerrar tocando fuera) que pide contar y
 * declarar el efectivo con el que se abre el turno. Se muestra tanto cuando
 * un vendedor entra a Vender por primera vez tras un Corte Z, como cuando el
 * admin entra a Reportes en la misma situación — es el mismo turno de caja,
 * lo declare quien lo declare primero.
 */
@Composable
fun DialogoAperturaCaja(
    estadoGuardado: EstadoGuardadoApertura,
    onConfirmar: (fondoInicial: Double) -> Unit
) {
    var fondoTexto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Obligatorio: no se cierra tocando fuera, hay que declarar el fondo. */ },
        title = { Text("Abrir caja") },
        text = {
            Column {
                Text(
                    "Antes de vender, cuenta el efectivo con el que empiezas el turno.",
                    color = TextoCremaApagado,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    "¿Cuánto efectivo hay en caja para empezar?",
                    color = TextoCrema,
                    fontSize = 13.sp
                )
                TextField(
                    value = fondoTexto,
                    onValueChange = { nuevo ->
                        if (nuevo.isEmpty() || nuevo.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            fondoTexto = nuevo
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoCarbon,
                        unfocusedContainerColor = FondoCarbon,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    )
                )
                if (estadoGuardado is EstadoGuardadoApertura.Error) {
                    Text(
                        estadoGuardado.mensaje,
                        color = ColorError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fondo = fondoTexto.toDoubleOrNull() ?: return@TextButton
                    onConfirmar(fondo)
                },
                enabled = fondoTexto.toDoubleOrNull() != null && estadoGuardado !is EstadoGuardadoApertura.Guardando
            ) {
                Text(
                    if (estadoGuardado is EstadoGuardadoApertura.Guardando) "Guardando..." else "Empezar turno",
                    color = AcentoTerracota
                )
            }
        }
    )
}
