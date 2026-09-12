package com.tuempresa.possystem.presentation.usuarios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

private val FondoCarbon = Color(0xFF221B1D)
private val FondoTarjeta = Color(0xFF2E2427)
private val AcentoTerracota = Color(0xFFD98E73)
private val TextoCrema = Color(0xFFF3E9E1)
private val TextoCremaApagado = Color(0xFFB6A199)
private val ColorExito = Color(0xFF8FBF8A)
private val ColorError = Color(0xFFE08585)

@Composable
fun PantallaUsuarios(
    app: POSApplication,
    onVolver: () -> Unit,
    onEditarUsuario: (UsuarioEntity) -> Unit = {},
    onNuevoUsuario: () -> Unit = {}
) {
    val viewModel: UsuariosViewModel = viewModel(
        factory = fabricaSimple { UsuariosViewModel(app.usuarioRepository) }
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
            // Header
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
            
            // Barra de búsqueda y filtros
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TextField(
                    value = viewModel.textoBusqueda,
                    onValueChange = { viewModel.buscarUsuarios(it) },
                    placeholder = { Text("Buscar por nombre o usuario...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FondoTarjeta,
                        unfocusedContainerColor = FondoTarjeta,
                        focusedTextColor = TextoCrema,
                        unfocusedTextColor = TextoCrema
                    ),
                    singleLine = true
                )
                
                // Filtros por rol
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
                        selected = viewModel.rolFiltro == RolUsuario.ADMINISTRADOR,
                        onClick = { viewModel.filtrarPorRol(RolUsuario.ADMINISTRADOR) },
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
                    FilterChip(
                        selected = viewModel.rolFiltro == RolUsuario.SUPERVISOR,
                        onClick = { viewModel.filtrarPorRol(RolUsuario.SUPERVISOR) },
                        label = { Text("Supervisor") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcentoTerracota
                        )
                    )
                }
            }
            
            // Lista de usuarios
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
    
    // Diálogo para agregar/editar usuario
    if (mostrarDialogoUsuario) {
        DialogoAgregarEditarUsuario(
            usuario = usuarioEditar,
            onCancelar = {
                mostrarDialogoUsuario = false
                usuarioEditar = null
                viewModel.limpiarEstadoOperacion()
            },
            onGuardar = { nombre, nombreUsuario, contrasena, rol ->
                viewModel.guardarUsuario(
                    id = usuarioEditar?.id ?: 0,
                    nombre = nombre,
                    nombreUsuario = nombreUsuario,
                    contrasena = contrasena,
                    rol = rol
                )
            },
            estadoOperacion = estadoOperacion
        )
    }
    
    // Diálogo de confirmación para eliminar
    mostrarConfirmacionEliminar?.let { usuario ->
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = null },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Estás seguro de eliminar a ${usuario.nombre}? Esta acción no se puede deshacer.") },
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
                    Text("Eliminar")
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
        // Avatar
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
            Text(
                "@${usuario.nombreUsuario}",
                color = TextoCremaApagado,
                fontSize = 13.sp
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeRol(rol = usuario.rol)
                if (usuario.ultimoAcceso != null) {
                    Text(
                        "Último acceso: ${formatoFechaHora.format(java.util.Date(usuario.ultimoAcceso))}",
                        color = TextoCremaApagado,
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        // Botones de acción
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onEditar) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = AcentoTerracota
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = ColorError
                )
            }
        }
    }
}

@Composable
private fun BadgeRol(rol: RolUsuario) {
    val (color, texto) = when (rol) {
        RolUsuario.ADMINISTRADOR -> Color(0xFFFF6B6B) to "Admin"
        RolUsuario.VENDEDOR -> AcentoTerracota to "Vendedor"
        RolUsuario.SUPERVISOR -> Color(0xFF4ECDC4) to "Supervisor"
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
    onGuardar: (String, String, String, RolUsuario) -> Unit,
    estadoOperacion: EstadoOperacionUsuario
) {
    var nombre by remember { mutableStateOf(usuario?.nombre ?: "") }
    var nombreUsuario by remember { mutableStateOf(usuario?.nombreUsuario ?: "") }
    var contrasena by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf(usuario?.rol ?: RolUsuario.VENDEDOR) }
    var mostrarContrasena by remember { mutableStateOf(false) }
    
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
                    value = nombreUsuario,
                    onValueChange = { nombreUsuario = it },
                    label = { Text("Nombre de usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                TextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text(if (usuario != null && contrasena.isEmpty()) "Dejar en blanco para mantener" else "Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (mostrarContrasena) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                            Icon(
                                if (mostrarContrasena) androidx.compose.material.icons.Icons.Default.Visibility
                                else androidx.compose.material.icons.Icons.Default.VisibilityOff,
                                contentDescription = if (mostrarContrasena) "Ocultar" else "Mostrar"
                            )
                        }
                    }
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (rol) {
                            RolUsuario.ADMINISTRADOR -> "Administrador"
                            RolUsuario.VENDEDOR -> "Vendedor"
                            RolUsuario.SUPERVISOR -> "Supervisor"
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
                                rol = RolUsuario.ADMINISTRADOR
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
                        DropdownMenuItem(
                            text = { Text("Supervisor") },
                            onClick = {
                                rol = RolUsuario.SUPERVISOR
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
                    if (nombre.isNotEmpty() && nombreUsuario.isNotEmpty() && 
                        (usuario != null || contrasena.isNotEmpty())) {
                        onGuardar(nombre, nombreUsuario, contrasena, rol)
                    }
                },
                enabled = estadoOperacion !is EstadoOperacionUsuario.Procesando &&
                        nombre.isNotEmpty() && nombreUsuario.isNotEmpty() &&
                        (usuario != null || contrasena.isNotEmpty())
            ) {
                Text(
                    if (estadoOperacion is EstadoOperacionUsuario.Procesando) "Guardando..." 
                    else if (usuario ==
