package com.tuempresa.possystem.presentation.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Paleta cálida tipo boutique — nada de SaaS azul/gris genérico.
// Fondo carbón cálido, acento terracota rosado (textil), dorado suave para éxito.
private val FondoCarbon = Color(0xFF221B1D)
private val FondoCarbonSuave = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val AcentoTerracotaSuave = Color(0xFF5C4038)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)

@Composable
fun PantallaLogin(viewModel: LoginViewModel) {
    val pin by viewModel.pin.collectAsState()
    val estado by viewModel.estado.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EncabezadoLogin(estado = estado)

            IndicadorPin(
                longitud = pin.length,
                total = 4,
                esError = estado is EstadoLogin.Error
            )

            Column(
                modifier = Modifier.padding(top = 40.dp)
            ) {
                TecladoNumerico(
                    onDigito = viewModel::agregarDigito,
                    onBorrar = viewModel::borrarDigito,
                    habilitado = estado !is EstadoLogin.Verificando
                )
            }
        }
    }
}

@Composable
private fun EncabezadoLogin(estado: EstadoLogin) {
    val (titulo, subtitulo, colorSubtitulo) = when (estado) {
        is EstadoLogin.Ingresando -> Triple("Bienvenido", "Ingresa tu PIN para continuar", TextoCremaApagado)
        is EstadoLogin.Verificando -> Triple("Bienvenido", "Verificando…", TextoCremaApagado)
        is EstadoLogin.Error -> Triple("PIN incorrecto", "Intenta de nuevo", ColorError)
        is EstadoLogin.SinUsuarios -> Triple("Sin usuarios registrados", "Contacta al administrador", ColorError)
    }

    Text(
        text = titulo,
        color = TextoCrema,
        fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = subtitulo,
        color = colorSubtitulo,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun IndicadorPin(longitud: Int, total: Int, esError: Boolean) {
    // Pequeño "shake" horizontal cuando el PIN es incorrecto — el único momento
    // de animación no disparado por toque directo, y solo ocurre una vez por error.
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(esError) {
        if (esError) {
            launch {
                val secuencia = listOf(-14f, 14f, -10f, 10f, -4f, 0f)
                for (valor in secuencia) {
                    offsetX.animateTo(valor, animationSpec = tween(45))
                }
            }
        }
    }

    IndicadorPinContenido(offsetX.value, longitud, total, esError)
}

@Composable
private fun IndicadorPinContenido(offsetX: Float, longitud: Int, total: Int, esError: Boolean) {
    Row(
        modifier = Modifier
            .padding(top = 36.dp)
            .offset(x = offsetX.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(total) { index ->
            val lleno = index < longitud
            val color = when {
                esError -> ColorError
                lleno -> AcentoTerracota
                else -> AcentoTerracotaSuave
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun TecladoNumerico(
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit,
    habilitado: Boolean
) {
    // 3 columnas: 1-9, luego fila final con espacio / 0 / borrar
    val filas = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        filas.forEach { fila ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                fila.forEach { digito ->
                    TeclaNumerica(
                        texto = digito.toString(),
                        habilitado = habilitado,
                        onClick = { onDigito(digito) }
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Espacio vacío para alinear el 0 en el centro
            Box(modifier = Modifier.size(72.dp))
            TeclaNumerica(
                texto = "0",
                habilitado = habilitado,
                onClick = { onDigito('0') }
            )
            TeclaBorrar(habilitado = habilitado, onClick = onBorrar)
        }
    }
}

@Composable
private fun TeclaNumerica(texto: String, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(FondoCarbonSuave)
            .clickable(
                enabled = habilitado,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = TextoCrema,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TeclaBorrar(habilitado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                enabled = habilitado,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⌫",
            color = TextoCremaApagado,
            fontSize = 24.sp
        )
    }
}
