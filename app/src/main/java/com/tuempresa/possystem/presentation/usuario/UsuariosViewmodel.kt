package com.tuempresa.possystem.presentation.usuarios

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import com.tuempresa.possystem.data.local.repository.UsuarioRepository
import com.tuempresa.possystem.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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

/**
 * repository se usa para listar/buscar/filtrar/desactivar usuarios.
 * authRepository se usa para crear usuarios y cambiar PIN, ya que es el único
 * que conoce el hasheo (PinHasher) — ver domain/AuthRepository.kt.
 */
class UsuariosViewModel(
    private val repository: UsuarioRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _estadoUsuarios = MutableStateFlow<EstadoUsuarios>(EstadoUsuarios.Cargando)
    val estadoUsuarios: StateFlow<EstadoUsuarios> = _estadoUsuarios

    private val _estadoOperacion = MutableStateFlow<EstadoOperacionUsuario>(EstadoOperacionUsuario.Idle)
    val estadoOperacion: StateFlow<EstadoOperacionUsuario> = _estadoOperacion

    private val _usuariosFiltrados = MutableStateFlow<List<UsuarioEntity>>(emptyList())
    val usuariosFiltrados: StateFlow<List<UsuarioEntity>> = _usuariosFiltrados

    var textoBusqueda by mutableStateOf("")
        private set

    var rolFiltro by mutableStateOf<RolUsuario?>(null)
        private set

    private var ultimaListaUsuarios: List<UsuarioEntity> = emptyList()

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
                    ultimaListaUsuarios = usuarios
                    _estadoUsuarios.value = EstadoUsuarios.Exitoso(usuarios)
                    aplicarFiltros(usuarios)
                }
        }
    }

    fun buscarUsuarios(texto: String) {
        textoBusqueda = texto
        aplicarFiltros(ultimaListaUsuarios)
    }

    fun filtrarPorRol(rol: RolUsuario?) {
        rolFiltro = rol
        aplicarFiltros(ultimaListaUsuarios)
    }

    private fun aplicarFiltros(usuarios: List<UsuarioEntity>) {
        var resultado = usuarios

        if (textoBusqueda.isNotEmpty()) {
            resultado = resultado.filter {
                it.nombre.contains(textoBusqueda, ignoreCase = true)
            }
        }

        if (rolFiltro != null) {
            resultado = resultado.filter { it.rol == rolFiltro }
        }

        _usuariosFiltrados.value = resultado
    }

    /** Crea un usuario nuevo con PIN, o edita nombre/rol de uno existente. */
    fun guardarUsuario(
        usuarioExistente: UsuarioEntity?,
        nombre: String,
        pin: String,
        rol: RolUsuario
    ) {
        viewModelScope.launch {
            _estadoOperacion.value = EstadoOperacionUsuario.Procesando
            try {
                if (usuarioExistente == null) {
                    if (pin.isBlank()) {
                        _estadoOperacion.value = EstadoOperacionUsuario.Error("El PIN es obligatorio")
                        return@launch
                    }
                    authRepository.crearUsuario(nombre = nombre, pin = pin, rol = rol)
                } else {
                    if (pin.isNotBlank()) {
                        authRepository.cambiarPin(usuarioExistente, pin)
                    }
                    repository.actualizarUsuario(
                        usuarioExistente.copy(nombre = nombre, rol = rol)
                    )
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
                if (!authRepository.puedeDesactivarse(usuario)) {
                    _estadoOperacion.value = EstadoOperacionUsuario.Error(
                        "No puedes desactivar al último administrador activo"
                    )
                    return@launch
                }
                repository.desactivarUsuario(usuario.id)
                _estadoOperacion.value = EstadoOperacionUsuario.Exitoso
            } catch (e: Exception) {
                _estadoOperacion.value = EstadoOperacionUsuario.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun limpiarEstadoOperacion() {
        _estadoOperacion.value = EstadoOperacionUsuario.Idle
    }
}
