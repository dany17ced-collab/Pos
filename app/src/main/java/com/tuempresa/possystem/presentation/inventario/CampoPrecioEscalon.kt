package com.tuempresa.possystem.presentation.inventario

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * Campo de precio (o de stock) con estado de texto ENTERAMENTE local.
 *
 * La versión anterior usaba `value = escalon.precioTexto` leyendo directo
 * del StateFlow del ViewModel: cada tecla disparaba
 * ViewModel -> StateFlow -> recomposición -> TextField, y ese ciclo por
 * tecla es lo que se sospecha causaba toques perdidos en listas con varios
 * TextField generados por forEach. Aquí el TextField es dueño de su propio
 * texto (remember) y solo AVISA hacia afuera con onTextoCambiado; nunca
 * vuelve a leer su propio valor desde el padre mientras el usuario escribe.
 */
@Composable
fun CampoPrecioEscalon(
    clave: String,
    valorInicial: String,
    onTextoCambiado: (String) -> Unit,
    colores: TextFieldColors,
    modifier: Modifier = Modifier,
    ancho: androidx.compose.ui.unit.Dp = 100.dp,
    tipoTeclado: KeyboardType = KeyboardType.Decimal
) {
    var texto by remember(clave) { mutableStateOf(valorInicial) }

    TextField(
        value = texto,
        onValueChange = { nuevo ->
            texto = nuevo
            onTextoCambiado(nuevo)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = ImeAction.Next),
        placeholder = { Text(if (tipoTeclado == KeyboardType.Decimal) "0.00" else "0") },
        modifier = modifier.width(ancho),
        colors = colores,
        shape = RoundedCornerShape(10.dp)
    )
}
