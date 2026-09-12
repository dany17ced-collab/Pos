package com.tuempresa.possystem.presentation.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import com.tuempresa.possystem.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class EstadoUsuarios {
    object Cargando : EstadoUsuarios()
    data class Exitoso(val usuarios: List<UsuarioEntity>) : EstadoUsuarios()
    data class Error(val mensaje: String) : EstadoUsuarios()
}

sealed class EstadoOperacionUsuario {
    object Idle : EstadoOperacionUsuario()
    object Procesando : EstadoOperacionUsuario()
    object Exitoso : EstadoOperacionUsuario()
    data class Error(val mensaje: String) : EstadoOperacionUsuario()
}

class UsuariosViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {
    
    private val _estadoUsuarios = MutableStateFlow<EstadoUsuarios>(EstadoUsuarios.Cargando)
    val estadoUsuarios: StateFlow<EstadoUsuarios> = _estadoUsuarios
    
    private val _estadoOperacion = MutableStateFlow(EstadoOperacionUsuario.Idle)
    val estadoOperacion: StateFlow<EstadoOperacionUsuario> = _estadoOperacion
    
    private val _usuariosFiltrados = MutableStateFlow<List<UsuarioEntity>>(emptyList())
    val usuariosFiltrados: StateFlow<List<UsuarioEntity>> = _usuariosFiltrados
    
    var textoBusqueda by mutableStateOf("")
        private set
    
    var rolFiltro by mutableStateOf<RolUsuario?>(null)
        private set
    
    init {
        cargarUsuarios()
    }
    
    private fun cargarUsuarios() {
        viewModelScope.launch {
            repository.todosUsuarios
                .catch { e ->
                    _estadoUsuarios.value = EstadoUsuarios.Error("Error al cargar usuarios: ${e.message}")
                }
                .collect { usuarios ->
                    _estadoUsuarios.value = EstadoUsuarios.Exitoso(usuarios)
                    aplicarFiltros(usuarios)
                }
        }
    }
    
    fun buscarUsuarios(texto: String) {
        textoBusqueda = texto
        aplicarFiltrosActuales()
    }
    
    fun filtrarPorRol(rol: RolUsuario?) {
        rolFiltro = rol
        aplicarFiltrosActuales()
    }
    
    private fun aplicarFiltrosActuales() {
        val estadoActual = _estadoUsuarios.value
        if (estadoActual is EstadoUsuarios.Exitoso) {
            aplicarFiltros(estadoActual.usuarios)
        }
    }
    
    private fun aplicarFiltros(usuarios: List<UsuarioEntity>) {
        var resultado = usuarios
        
        // Filtrar por texto de búsqueda
        if (textoBusqueda.isNotEmpty()) {
            resultado = resultado.filter {
                it.nombre.contains(textoBusqueda, ignoreCase = true) ||
                it.nombreUsuario.contains(textoBusqueda, ignoreCase = true)
            }
        }
        
        // Filtrar por rol
        if (rolFiltro != null) {
            resultado = resultado.filter { it.rol == rolFiltro }
        }
        
        _usuariosFiltrados.value = resultado
    }
    
    fun guardarUsuario(
        id: Long = 0,
        nombre: String,
        nombreUsuario: String,
        contrasena: String,
        rol: RolUsuario,
        contrasenaActual: String? = null
    ) {
        viewModelScope.launch {
            _estadoOperacion.value = EstadoOperacionUsuario.Procesando
            
            try {
                // Validar que el nombre de usuario no exista (excepto para el mismo usuario)
                if (id == 0L || repository.obtenerUsuarioPorId(id)?.nombreUsuario != nombreUsuario) {
                    if (repository.existeNombreUsuario(nombreUsuario)) {
                        _estadoOperacion.value = EstadoOperacionUsuario.Error(
                            "El nombre de usuario ya está en uso"
                        )
                        return@launch
                    }
                }
                
                val usuario = if (id > 0) {
                    // Actualizar usuario existente
                    val existente = repository.obtenerUsuarioPorId(id)
                        ?: throw Exception("Usuario no encontrado")
                    
                    existente.copy(
                        nombre = nombre,
                        nombreUsuario = nombreUsuario,
                        contrasena = if (contrasena.isNotEmpty()) contrasena else existente.contrasena,
                        rol = rol
                    )
                } else {
                    // Crear nuevo usuario
                    UsuarioEntity(
                        nombre = nombre,
                        nombreUsuario = nombreUsuario,
                        contrasena = contrasena,
                        rol = rol
                    )
                }
                
                if (id > 0) {
                    repository.actualizarUsuario(usuario)
                } else {
                    repository.insertarUsuario(usuario)
                }
                
                _estadoOperacion.value = EstadoOperacionUsuario.Exitoso
            } catch (e: Exception) {
                _estadoOperacion.value = EstadoOperacionUsuario.Error(e.message ?: "Error al guardar")
            }
        }
    }
    
    fun eliminarUsuario(usuario: UsuarioEntity) {
        viewModelScope.launch {
            _estadoOperacion.value = EstadoOperacionUsuario.Procesando
            try {
                repository.desactivarUsuario(usuario.id)
                _estadoOperacion.value = EstadoOperacionUsuario.Exitoso
            } catch (e: Exception) {
                _estadoOperacion.value = EstadoOperacionUsuario.Error(e.message ?: "Error al eliminar")
            }
        }
    }
    
    fun actualizarUltimoAcceso(usuarioId: Long) {
        viewModelScope.launch {
            repository.actualizarUltimoAcceso(usuarioId)
        }
    }
    
    fun limpiarEstadoOperacion() {
        _estadoOperacion.value = EstadoOperacionUsuario.Idle
    }
}
