package com.tuempresa.possystem.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuempresa.possystem.presentation.theme.EcoPosBottomNav
import com.tuempresa.possystem.presentation.theme.EcoPosColors
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.theme.EcoPosShapes

private data class ItemMenu(val titulo: String, val icono: ImageVector, val onClick: () -> Unit)

/**
 * Pantalla "Menú" estilo Eco POS: grid de accesos rápidos a ajustes y
 * herramientas secundarias, reemplazando el listado vertical de opciones
 * y el botón "Cerrar sesión" que antes vivían al fondo del Home.
 */
@Composable
fun PantallaMenuEcoPos(
    esAdmin: Boolean,
    onAjustes: () -> Unit,
    onClientes: () -> Unit,
    onDescuento: () -> Unit,
    onUnidad: () -> Unit,
    onEtiquetaPago: () -> Unit,
    onCopiaSeguridad: () -> Unit,
    onImportarProductos: () -> Unit,
    onImpresora: () -> Unit,
    onUsuarios: () -> Unit,
    onCambios: () -> Unit,
    onCerrarSesion: () -> Unit,
    onNavegarDestino: (EcoPosDestino) -> Unit
) {
    val items = buildList {
        add(ItemMenu("Ajustes", Icons.Filled.Settings, onAjustes))
        add(ItemMenu("Cliente", Icons.Filled.ContactMail, onClientes))
        add(ItemMenu("Descuento", Icons.Filled.Percent, onDescuento))
        add(ItemMenu("Unidad", Icons.Filled.Scale, onUnidad))
        add(ItemMenu("Etiqueta de Pago", Icons.Filled.Label, onEtiquetaPago))
        add(ItemMenu("Copia de seguridad", Icons.Filled.CloudUpload, onCopiaSeguridad))
        add(ItemMenu("Importar Productos", Icons.Filled.SwapVert, onImportarProductos))
        add(ItemMenu("Impresora", Icons.Filled.Print, onImpresora))
        if (esAdmin) {
            add(ItemMenu("Usuarios", Icons.Filled.People, onUsuarios))
        }
        add(ItemMenu("Cambios", Icons.Filled.CompareArrows, onCambios))
        add(ItemMenu("Cerrar sesión", Icons.Filled.Logout, onCerrarSesion))
    }

    Scaffold(
        containerColor = EcoPosColors.FondoNegro,
        bottomBar = {
            EcoPosBottomNav(seleccionado = EcoPosDestino.MENU, onSeleccionar = onNavegarDestino)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Menú",
                color = EcoPosColors.TextoBlanco,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    TarjetaMenu(item)
                }
            }
        }
    }
}

@Composable
private fun TarjetaMenu(item: ItemMenu) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(EcoPosShapes.TarjetaChica)
            .background(EcoPosColors.FondoTarjeta)
            .clickable(onClick = item.onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icono, contentDescription = item.titulo, tint = EcoPosColors.TextoBlanco)
        Text(
            item.titulo,
            color = EcoPosColors.TextoBlanco,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
