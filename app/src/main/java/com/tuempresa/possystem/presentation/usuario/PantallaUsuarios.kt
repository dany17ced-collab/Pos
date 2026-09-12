package com.tuempresa.possystem.presentation.usuarios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import com.tuempresa.possystem.presentation.inventario.fabricaSimple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorError = Color(0xFFE08585)

private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

@Composable
fun PantallaUsuarios(
    app: POSApplication,
    onVolver: () -> Unit
) {
    val viewModel: UsuariosViewModel = viewModel(
        factory = fabricaSimple { UsuariosViewModel(app.usuarioRepository, app.authRepository) }
    )

    val estadoUsuarios by viewModel.estadoUsuarios.collectAsState()
    val usuarios by viewModel.usuariosFiltrados.collectAsState()
    val estadoOperacion by viewModel.estadoOperacion.collectAsState()

    var mostrarDialogoUsuario by remember { mutableStateOf(false) }
    var usuarioEditar by remember { mutableStateOf<UsuarioEntity?>(null) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf<UsuarioEntity?>(null) }

    LaunchedEffect(estadoOperacion) {
        if (estadoOperacion is EstadoOperacionUsuario.Exitoso) {
            mostrarDialogoUsuario = false
            usuarioEditar = null
            viewModel.limpiarEstadoOperacion()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoCarbon) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                Text(
                    text = "Usuarios",
                    color = TextoCrema,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                FloatingActionButton(
                    onClick = {
                        usuarioEditar = null
                        mostrarDialogoUsuario = true
                    },
                    containerColor = AcentoTerracota,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("+", fontSize = 24.sp, color = FondoCarbon)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TextField(
                    value = viewModel.textoBusqueda,
                    onValueChange = { viewModel.buscarUsuarios(it) },
                    placeholder = { Text("Buscar por nombre...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoTarjeta,
                        unfocusedContainerColor = FondoTarjeta,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewModel.rolFiltro == null,
                        onClick = { viewModel.filtrarPorRol(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcentoTerracota
                        )
                    )
                    FilterChip(
                        selected = viewModel.rolFiltro == RolUsuario.ADMIN,
                        onClick = { viewModel.filtrarPorRol(RolUsuario.ADMIN) },
                        label = { Text("Admin") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcentoTerracota
                        )
                    )
                    FilterChip(
                        selected = viewModel.rolFiltro == RolUsuario.VENDEDOR,
                        onClick = { viewModel.filtrarPorRol(RolUsuario.VENDEDOR) },
                        label = { Text("Vendedor") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcentoTerracota
                        )
                    )
                }
            }

            when (val estado = estadoUsuarios) {
                is EstadoUsuarios.Cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AcentoTerracota)
                    }
                }
                is EstadoUsuarios.Exitoso -> {
                    if (usuarios.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay usuarios registrados",
                                color = TextoCremaApagado,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(usuarios, key = { it.id }) { usuario ->
                                TarjetaUsuario(
                                    usuario = usuario,
                                    onEditar = {
                                        usuarioEditar = usuario
                                        mostrarDialogoUsuario = true
                                    },
                                    onEliminar = { mostrarConfirmacionEliminar = usuario }
                                )
                            }
                        }
                    }
                }
                is EstadoUsuarios.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            estado.mensaje,
                            color = ColorError,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoUsuario) {
        DialogoAgregarEditarUsuario(
            usuario = usuarioEditar,
            onCancelar = {
                mostrarDialogoUsuario = false
                usuarioEditar = null
                viewModel.limpiarEstadoOperacion()
            },
            onGuardar = { nombre, pin, rol ->
                viewModel.guardarUsuario(
                    usuarioExistente = usuarioEditar,
                    nombre = nombre,
                    pin = pin,
                    rol = rol
                )
            },
            estadoOperacion = estadoOperacion
        )
    }

    mostrarConfirmacionEliminar?.let { usuario ->
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = null },
            title = { Text("Desactivar usuario") },
            text = { Text("¿Estás seguro de desactivar a ${usuario.nombre}? Ya no podrá iniciar sesión.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarUsuario(usuario)
                        mostrarConfirmacionEliminar = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ColorError
                    )
                ) {
                    Text("Desactivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TarjetaUsuario(
    usuario: UsuarioEntity,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AcentoTerracota),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = usuario.nombre.first().uppercase(),
                color = FondoCarbon,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    usuario.nombre,
                    color = TextoCrema,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!usuario.activo) {
                    Text(
                        " (Inactivo)",
                        color = ColorError,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgeRol(rol = usuario.rol)
                Text(
                    "Creado: ${formatoFechaHora.format(Date(usuario.creadoEn))}",
                    color = TextoCremaApagado,
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onEditar) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = AcentoTerracota
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Desactivar",
                    tint = ColorError
                )
            }
        }
    }
}

@Composable
private fun BadgeRol(rol: RolUsuario) {
    val (color, texto) = when (rol) {
        RolUsuario.ADMIN -> Color(0xFFFF6B6B) to "Admin"
        RolUsuario.VENDEDOR -> AcentoTerracota to "Vendedor"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            texto,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoAgregarEditarUsuario(
    usuario: UsuarioEntity?,
    onCancelar: () -> Unit,
    onGuardar: (String, String, RolUsuario) -> Unit,
    estadoOperacion: EstadoOperacionUsuario
) {
    var nombre by remember { mutableStateOf(usuario?.nombre ?: "") }
    var pin by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf(usuario?.rol ?: RolUsuario.VENDEDOR) }
    var mostrarPin by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val pinValido = usuario != null || (pin.length in 4..6)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (usuario == null) "Nuevo usuario" else "Editar usuario") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                    label = { Text(if (usuario != null) "Nuevo PIN (opcional)" else "PIN (4 a 6 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (mostrarPin) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { mostrarPin = !mostrarPin }) {
                            Text(if (mostrarPin) "Ocultar" else "Ver", fontSize = 11.sp)
                        }
                    }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (rol) {
                            RolUsuario.ADMIN -> "Administrador"
                            RolUsuario.VENDEDOR -> "Vendedor"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Administrador") },
                            onClick = {
                                rol = RolUsuario.ADMIN
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Vendedor") },
                            onClick = {
                                rol = RolUsuario.VENDEDOR
                                expanded = false
                            }
                        )
                    }
                }

                if (estadoOperacion is EstadoOperacionUsuario.Error) {
                    Text(
                        estadoOperacion.mensaje,
                        color = ColorError,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isNotEmpty() && pinValido) {
                        onGuardar(nombre, pin, rol)
                    }
                },
                enabled = estadoOperacion !is EstadoOperacionUsuario.Procesando &&
                        nombre.isNotEmpty() && pinValido
            ) {
                Text(
                    if (estadoOperacion is EstadoOperacionUsuario.Procesando) "Guardando..."
                    else "Guardar"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
