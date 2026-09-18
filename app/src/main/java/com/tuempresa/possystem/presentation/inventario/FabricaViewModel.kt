package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Igual que fabricaSimple (definida en PantallaInventario.kt), pero recibe
 * además `app` (POSApplication) como primer parámetro. Se usa en
 * GrafoNavegacionPrincipal.kt, por ejemplo:
 *
 *   val loginViewModel: LoginViewModel = viewModel(
 *       factory = fabricaViewModel(app) { LoginViewModel(app) }
 *   )
 *
 * `app` no se usa directamente dentro de la fábrica (el lambda ya lo captura
 * por closure), pero se recibe explícitamente para dejar claro en el punto de
 * uso de qué Application depende el ViewModel creado.
 */
fun <T : ViewModel, A> fabricaViewModel(app: A, crear: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return crear() as VM
        }
    }
}
