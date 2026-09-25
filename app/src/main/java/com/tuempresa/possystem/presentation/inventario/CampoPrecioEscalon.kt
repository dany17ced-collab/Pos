package com.tuempresa.possystem.presentation.inventario

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * Campo de precio (o de stock): TextField directo, con el valor viniendo del
 * padre (igual que CampoTexto, que usan Nombre/Descripción/Precio de compra
 * y nunca tuvo problemas de foco). Se probó una versión con estado local
 * (remember) para evitar recomposición del árbol completo en cada tecla,
 * pero esa segunda fuente de verdad (local + StateFlow del padre) era la
 * causa real de que el toque a veces no enfocara el campo. El parámetro
 * `clave` ya no es necesario funcionalmente pero se deja para no romper los
 * call sites existentes.
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
    TextField(
        value = valorInicial,
        onValueChange = onTextoCambiado,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = ImeAction.Next),
        placeholder = { Text(if (tipoTeclado == KeyboardType.Decimal) "0.00" else "0") },
        modifier = modifier.width(ancho),
        colors = colores,
        shape = RoundedCornerShape(10.dp)
    )
}
