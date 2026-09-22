package com.tuempresa.possystem.presentation.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import com.tuempresa.possystem.presentation.theme.EcoPosColors

private val FondoCarbon = EcoPosColors.FondoNegro
private val FondoTarjeta = EcoPosColors.FondoTarjeta
private val AcentoTerracota = EcoPosColors.AcentoAmbar
private val TextoCrema = EcoPosColors.TextoBlanco
private val TextoCremaApagado = EcoPosColors.TextoGrisApagado
private val ColorError = EcoPosColors.ColorError
private val ColorExito = EcoPosColors.ColorExito
/**
 * Ajuste ÚNICO y global de cómo se ve la boleta: logo, eslogan, dirección,
 * teléfono, RUC y mensaje de pie de página. No se configura por venta — se
 * guarda una sola vez aquí y se aplica automáticamente a toda boleta que se
 * comparta como PDF o se imprima en la térmica.
 */
@Composable
fun PantallaAjustesBoleta(app: POSApplication, onVolver: () -> Unit) {
    val viewModel: AjustesBoletaViewModel = viewModel(factory = fabricaSimple { AjustesBoletaViewModel(app) })

    val formulario by viewModel.formulario.collectAsState()
    val estadoGuardado by viewModel.estadoGuardado.collectAsState()

    val selectorImagen = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.elegirLogo(it) } }

    LaunchedEffect(estadoGuardado) {
        if (estadoGuardado is EstadoGuardadoAjustes.Guardado) {
            onVolver()
        }
    }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = FondoCarbon) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    color = TextoCrema,
                    fontSize = 26.sp,
                    modifier = Modifier.clickable(onClick = onVolver)
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = "Ajustes de boleta",
                        color = TextoCrema,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Se aplica a todas las boletas — no se pide por cada venta",
                        color = TextoCremaApagado,
                        fontSize = 12.sp
                    )
                }
            }

            SeccionLogo(
                rutaLogoGuardada = formulario.rutaLogoGuardada,
                logoUriTemporal = formulario.logoUriTemporal,
                onElegirLogo = {
                    selectorImagen.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onQuitarLogo = viewModel::quitarLogo
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                CampoAjuste(
                    etiqueta = "Nombre de la tienda",
                    valor = formulario.nombreTienda,
                    onCambio = viewModel::actualizarNombreTienda,
                    placeholder = "Ej. Hermanos Calderón"
                )
                CampoAjuste(
                    etiqueta = "Eslogan de marca",
                    valor = formulario.eslogan,
                    onCambio = viewModel::actualizarEslogan,
                    placeholder = "Ej. Moda que te acompaña"
                )
                CampoAjuste(
                    etiqueta = "Descripción (opcional)",
                    valor = formulario.descripcion,
                    onCambio = viewModel::actualizarDescripcion,
                    placeholder = "Ej. Ropa de algodón para toda la familia"
                )
                CampoAjuste(
                    etiqueta = "Dirección",
                    valor = formulario.direccion,
                    onCambio = viewModel::actualizarDireccion,
                    placeholder = "Ej. Jr. Los Álamos 123, Lima"
                )
                CampoAjuste(
                    etiqueta = "Teléfono",
                    valor = formulario.telefono,
                    onCambio = viewModel::actualizarTelefono,
                    placeholder = "Ej. 987 654 321"
                )
                CampoAjuste(
                    etiqueta = "RUC (opcional)",
                    valor = formulario.ruc,
                    onCambio = viewModel::actualizarRuc,
                    placeholder = "Ej. 20123456789"
                )
                CampoAjuste(
                    etiqueta = "Mensaje de pie de página",
                    valor = formulario.piePagina,
                    onCambio = viewModel::actualizarPiePagina,
                    placeholder = "¡Gracias por su compra!"
                )

                Text(
                    "Redes sociales",
                    color = TextoCrema,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Se imprime como código QR en la boleta",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
                CampoAjuste(
                    etiqueta = "Link (Instagram, WhatsApp, linktree, etc.)",
                    valor = formulario.linkRedesSociales,
                    onCambio = viewModel::actualizarLinkRedesSociales,
                    placeholder = "https://..."
                )

                Text(
                    "Plazos para cambios y devoluciones",
                    color = TextoCrema,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Días desde la fecha de venta en que se acepta el cambio",
                    color = TextoCremaApagado,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        CampoAjuste(
                            etiqueta = "Por cambio de talla",
                            valor = formulario.diasPlazoCambioTallaTexto,
                            onCambio = viewModel::actualizarDiasPlazoTalla,
                            placeholder = "7",
                            tipoTeclado = KeyboardType.Number
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CampoAjuste(
                            etiqueta = "Por falla de fábrica",
                            valor = formulario.diasPlazoCambioFabricaTexto,
                            onCambio = viewModel::actualizarDiasPlazoFabrica,
                            placeholder = "30",
                            tipoTeclado = KeyboardType.Number
                        )
                    }
                }

                CampoAjuste(
                    etiqueta = "Advertencia legal de cambios/devoluciones (opcional)",
                    valor = formulario.politicaCambios,
                    onCambio = viewModel::actualizarPoliticaCambios,
                    placeholder = "Ej. Según la Ley N° 3245, las devoluciones se aceptan hasta 72 horas. Si la prenda está lavada, no se aceptan cambios ni devoluciones.",
                    lineasMaximas = 4
                )
                Text(
                    "Se imprime con ⚠️ debajo del código QR de cambios en la boleta.",
                    color = TextoCremaApagado,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (estadoGuardado is EstadoGuardadoAjustes.Error) {
                    Text(
                        text = (estadoGuardado as EstadoGuardadoAjustes.Error).mensaje,
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Button(
                    onClick = { viewModel.guardar() },
                    enabled = estadoGuardado !is EstadoGuardadoAjustes.Guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoTerracota),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Text(
                        if (estadoGuardado is EstadoGuardadoAjustes.Guardando) "Guardando…" else "Guardar ajustes",
                        color = FondoCarbon,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionLogo(
    rutaLogoGuardada: String?,
    logoUriTemporal: android.net.Uri?,
    onElegirLogo: () -> Unit,
    onQuitarLogo: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Logo de la tienda", color = TextoCrema, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(
            "Aparece en la parte superior de la boleta compartida e impresa",
            color = TextoCremaApagado,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FondoTarjeta)
                    .clickable(onClick = onElegirLogo),
                contentAlignment = Alignment.Center
            ) {
                val modeloImagen = logoUriTemporal ?: rutaLogoGuardada
                if (modeloImagen != null) {
                    AsyncImage(
                        model = modeloImagen,
                        contentDescription = "Logo de la tienda",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text("📷", fontSize = 24.sp)
                }
            }

            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    if (logoUriTemporal != null || rutaLogoGuardada != null) "Cambiar logo" else "Subir logo",
                    color = AcentoTerracota,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onElegirLogo)
                )
                if (logoUriTemporal != null || rutaLogoGuardada != null) {
                    Text(
                        "Quitar logo",
                        color = ColorError,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable(onClick = onQuitarLogo)
                    )
                }
            }
        }
    }
}

@Composable
private fun CampoAjuste(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    lineasMaximas: Int = 1
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(etiqueta, color = TextoCremaApagado, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = valor,
            onValueChange = onCambio,
            placeholder = { Text(placeholder, color = TextoCremaApagado) },
            singleLine = lineasMaximas == 1,
            maxLines = lineasMaximas,
            keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FondoTarjeta,
                unfocusedContainerColor = FondoTarjeta,
                focusedTextColor = TextoCrema,
                unfocusedTextColor = TextoCrema,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AcentoTerracota
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
