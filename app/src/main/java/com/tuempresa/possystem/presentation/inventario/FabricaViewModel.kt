package com.tuempresa.possystem.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Fábrica genérica para crear ViewModels con constructor manual (sin
 * Hilt/Koin). Se usa en todas las pantallas que necesitan pasarle `app`
 * (POSApplication) u otros parámetros al ViewModel, por ejemplo:
 *
 *   val viewModel: InventarioViewModel = viewModel(
 *       factory = fabricaSimple { InventarioViewModel(app) }
 *   )
 */
fun <T : ViewModel> fabricaSimple(crear: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return crear() as VM
        }
    }
}

/**
 * Igual que fabricaSimple, pero recibe además `app` (POSApplication) como
 * primer parámetro. Se usa en GrafoNavegacionPrincipal.kt, por ejemplo:
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
    return fabricaSimple(crear)
}
