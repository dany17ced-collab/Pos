package com.tuempresa.possystem.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.domain.ResultadoLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EstadoLogin {
    object Ingresando : EstadoLogin()
    object Verificando : EstadoLogin()
    object Error : EstadoLogin()
    object SinUsuarios : EstadoLogin()
    object SinTiendas : EstadoLogin()
}

class LoginViewModel(private val app: POSApplication) : ViewModel() {

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _estado = MutableStateFlow<EstadoLogin>(EstadoLogin.Ingresando)
    val estado: StateFlow<EstadoLogin> = _estado.asStateFlow()

    companion object {
        private const val LONGITUD_PIN = 4
    }

    fun agregarDigito(digito: Char) {
        if (_pin.value.length >= LONGITUD_PIN) return
        _estado.value = EstadoLogin.Ingresando
        _pin.value += digito

        if (_pin.value.length == LONGITUD_PIN) {
            intentarLogin()
        }
    }

    fun borrarDigito() {
        if (_pin.value.isNotEmpty()) {
            _pin.value = _pin.value.dropLast(1)
            _estado.value = EstadoLogin.Ingresando
        }
    }

    private fun intentarLogin() {
        viewModelScope.launch {
            _estado.value = EstadoLogin.Verificando
            try {
                when (val resultado = app.authRepository.iniciarSesion(_pin.value)) {
                    is ResultadoLogin.Exitoso -> {
                        // Toda sesión arranca en la primera tienda activa disponible.
                        // Un ADMIN puede cambiarla luego desde Ajustes; un VENDEDOR
                        // queda operando en esa sucursal por el resto de su turno.
                        //
                        // Se reintenta un par de veces con una espera corta porque en
                        // un arranque en frío POSApplication.crearTiendaInicialSiNoExiste()
                        // corre en paralelo y puede no haber terminado todavía cuando el
                        // usuario teclea el PIN muy rápido.
                        var tienda = app.tiendaRepository.obtenerTiendasActivas().firstOrNull()
                        var intentos = 0
                        while (tienda == null && intentos < 3) {
                            kotlinx.coroutines.delay(300)
                            tienda = app.tiendaRepository.obtenerTiendasActivas().firstOrNull()
                            intentos++
                        }

                        if (tienda == null) {
                            _estado.value = EstadoLogin.SinTiendas
                            _pin.value = ""
                            return@launch
                        }

                        app.sessionManager.iniciarSesion(resultado.usuario)
                        app.sessionManager.establecerTiendaActiva(tienda)
                        // La navegación reacciona a sessionManager.usuarioActual; no se
                        // necesita estado adicional aquí.
                    }
                    is ResultadoLogin.PinIncorrecto -> {
                        _estado.value = EstadoLogin.Error
                        _pin.value = ""
                    }
                    is ResultadoLogin.SinUsuariosActivos -> {
                        _estado.value = EstadoLogin.SinUsuarios
                        _pin.value = ""
                    }
                }
            } catch (e: Exception) {
                // Cualquier error inesperado (BD no lista, etc.) se trata como PIN
                // incorrecto en vez de dejar que la excepción tumbe la app.
                _estado.value = EstadoLogin.Error
                _pin.value = ""
            }
        }
    }
}
