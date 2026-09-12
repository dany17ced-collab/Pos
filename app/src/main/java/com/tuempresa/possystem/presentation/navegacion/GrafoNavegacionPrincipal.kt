package com.tuempresa.possystem.presentation.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuempresa.possystem.CrashHandler
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.presentation.home.PantallaHomeAdmin
import com.tuempresa.possystem.presentation.home.PantallaHomeVendedor
import com.tuempresa.possystem.presentation.home.PantallaProximamente
import com.tuempresa.possystem.presentation.login.LoginViewModel
import com.tuempresa.possystem.presentation.login.PantallaLogin
import com.tuempresa.possystem.presentation.inventario.PantallaEditarPrecio
import com.tuempresa.possystem.presentation.inventario.PantallaEntradaMercaderia
import com.tuempresa.possystem.presentation.inventario.PantallaInventario
import com.tuempresa.possystem.presentation.inventario.PantallaNuevoProducto
import com.tuempresa.possystem.presentation.reportes.PantallaReportes
import com.tuempresa.possystem.presentation.venta.PantallaVenta

private object Rutas {
    const val LOGIN = "login"
    const val HOME_ADMIN = "home_admin"
    const val HOME_VENDEDOR = "home_vendedor"
    const val VENTA = "venta"
    const val INVENTARIO = "inventario"
    const val NUEVO_PRODUCTO = "nuevo_producto"
    const val ENTRADA_MERCADERIA = "entrada_mercaderia"
    const val EDITAR_PRECIO = "editar_precio/{productoId}"
    const val REPORTES = "reportes"
}

@Composable
fun GrafoNavegacionPrincipal(app: POSApplication) {
    val navController = rememberNavController()
    val usuarioActual by app.sessionManager.usuarioActual.collectAsState()

    // Cuando la sesión cambia (login o logout), redirige automáticamente
    // a la pantalla correspondiente sin que cada pantalla tenga que saberlo.
    // Se ignora el primer valor (arranque en null = login, que ya es el
    // startDestination del NavHost) para no navegar antes de que el grafo exista.
    var esPrimeraComposicion by remember { mutableStateOf(true) }
    LaunchedEffect(usuarioActual) {
        if (esPrimeraComposicion) {
            esPrimeraComposicion = false
            return@LaunchedEffect
        }
        val destino = when (usuarioActual?.rol) {
            RolUsuario.ADMIN -> Rutas.HOME_ADMIN
            RolUsuario.VENDEDOR -> Rutas.HOME_VENDEDOR
            null -> Rutas.LOGIN
        }
        navController.navigate(destino) {
            // popUpTo(0) causaba un crash porque 0 no es un id de destino válido
            // en este grafo (las rutas son strings, no ids numéricos). Se usa el id
            // raíz real del grafo de navegación para limpiar todo el historial.
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Rutas.LOGIN) {
        composable(Rutas.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(factory = fabricaViewModel(app) {
                LoginViewModel(app)
            })
            val ultimoError = remember { CrashHandler.leerUltimoError(app) }
            PantallaLogin(viewModel = loginViewModel, ultimoError = ultimoError)
        }

        composable(Rutas.HOME_VENDEDOR) {
            PantallaHomeVendedor(
                nombreUsuario = usuarioActual?.nombre ?: "",
                onNavegar = { ruta ->
                    when (ruta) {
                        "venta" -> navController.navigate(Rutas.VENTA)
                        else -> navController.navigate("proximamente/$ruta")
                    }
                },
                onCerrarSesion = { app.sessionManager.cerrarSesion() }
            )
        }

        composable(Rutas.HOME_ADMIN) {
            PantallaHomeAdmin(
                nombreUsuario = usuarioActual?.nombre ?: "",
                onNavegar = { ruta ->
                    when (ruta) {
                        "venta" -> navController.navigate(Rutas.VENTA)
                        "inventario" -> navController.navigate(Rutas.INVENTARIO)
                        "reportes" -> navController.navigate(Rutas.REPORTES)
                        else -> navController.navigate("proximamente/$ruta")
                    }
                },
                onCerrarSesion = { app.sessionManager.cerrarSesion() }
            )
        }

        composable(Rutas.VENTA) {
            PantallaVenta(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.INVENTARIO) {
            PantallaInventario(
                app = app,
                onVolver = { navController.popBackStack() },
                onNuevoProducto = { navController.navigate(Rutas.NUEVO_PRODUCTO) },
                onEntradaMercaderia = { navController.navigate(Rutas.ENTRADA_MERCADERIA) },
                onEditarPrecio = { productoId -> navController.navigate("editar_precio/$productoId") }
            )
        }

        composable(
            Rutas.EDITAR_PRECIO,
            arguments = listOf(navArgument("productoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
            PantallaEditarPrecio(
                app = app,
                productoId = productoId,
                onVolver = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() }
            )
        }

        composable(Rutas.REPORTES) {
            PantallaReportes(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.NUEVO_PRODUCTO) {
            PantallaNuevoProducto(
                app = app,
                onVolver = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() }
            )
        }

        composable(Rutas.ENTRADA_MERCADERIA) {
            PantallaEntradaMercaderia(
                app = app,
                onVolver = { navController.popBackStack() },
                onIrANuevoProducto = {
                    navController.navigate(Rutas.NUEVO_PRODUCTO) {
                        popUpTo(Rutas.ENTRADA_MERCADERIA) { inclusive = true }
                    }
                }
            )
        }

        composable("proximamente/{seccion}") { backStackEntry ->
            val seccion = backStackEntry.arguments?.getString("seccion") ?: ""
            PantallaProximamente(
                titulo = tituloParaSeccion(seccion),
                onVolver = { navController.popBackStack() }
            )
        }
    }
}

private fun tituloParaSeccion(seccion: String): String = when (seccion) {
    "venta" -> "Vender"
    "inventario" -> "Inventario"
    "inventario_consulta" -> "Consultar inventario"
    "mis_ventas" -> "Mis ventas de hoy"
    "reportes" -> "Reportes y caja"
    "usuarios" -> "Usuarios"
    else -> seccion
}

/** Fábrica simple de ViewModel para inyectar la Application manualmente (sin Hilt/Koin todavía). */
private fun <T : androidx.lifecycle.ViewModel> fabricaViewModel(
    app: POSApplication,
    crear: () -> T
): androidx.lifecycle.ViewModelProvider.Factory {
    return object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <U : androidx.lifecycle.ViewModel> create(modelClass: Class<U>): U {
            return crear() as U
        }
    }
}
