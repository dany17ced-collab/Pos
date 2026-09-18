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
import com.tuempresa.possystem.presentation.ajustes.PantallaAjustesBoleta
import com.tuempresa.possystem.presentation.ajustes.PantallaAjustesImpresora
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaAjustesGenerales
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaEtiquetasPago
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaUnidades
import com.tuempresa.possystem.presentation.cambios.PantallaCambios
import com.tuempresa.possystem.presentation.clientes.PantallaAnadirCliente
import com.tuempresa.possystem.presentation.clientes.PantallaClientes
import com.tuempresa.possystem.presentation.descuentos.PantallaAnadirDescuento
import com.tuempresa.possystem.presentation.descuentos.PantallaDescuentos
import com.tuempresa.possystem.presentation.importar.PantallaImportarProductos
import com.tuempresa.possystem.presentation.home.CarroActivo
import com.tuempresa.possystem.presentation.home.PantallaHomeEcoPos
import com.tuempresa.possystem.presentation.home.PantallaMenuEcoPos
import com.tuempresa.possystem.presentation.home.PantallaProximamente
import com.tuempresa.possystem.presentation.theme.EcoPosDestino
import com.tuempresa.possystem.presentation.login.LoginViewModel
import com.tuempresa.possystem.presentation.login.PantallaLogin
import com.tuempresa.possystem.presentation.inventario.PantallaEditarPrecio
import com.tuempresa.possystem.presentation.inventario.PantallaEntradaMercaderia
import com.tuempresa.possystem.presentation.inventario.PantallaInventario
import com.tuempresa.possystem.presentation.inventario.PantallaInventarioConsulta
import com.tuempresa.possystem.presentation.inventario.PantallaNuevoProducto
import com.tuempresa.possystem.presentation.inventario.fabricaViewModel
import com.tuempresa.possystem.presentation.reportes.PantallaReportes
import com.tuempresa.possystem.presentation.usuarios.PantallaUsuarios
import com.tuempresa.possystem.presentation.venta.PantallaMisVentas
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
    const val USUARIOS = "usuarios"
    const val INVENTARIO_CONSULTA = "inventario_consulta"
    const val MIS_VENTAS = "mis_ventas"
    const val AJUSTES_BOLETA = "ajustes_boleta"
    const val AJUSTES_IMPRESORA = "ajustes_impresora"
    const val CAMBIOS = "cambios"
    const val MENU = "menu"
    const val CLIENTES = "clientes"
    const val DESCUENTOS = "descuentos"
    const val UNIDADES = "unidades"
    const val ETIQUETAS_PAGO = "etiquetas_pago"
    const val IMPORTAR_PRODUCTOS = "importar_productos"
    const val AJUSTES_GENERALES = "ajustes_generales"
    const val NUEVO_CLIENTE = "nuevo_cliente"
    const val NUEVO_DESCUENTO = "nuevo_descuento"
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
            PantallaHomeEcoPos(
                nombreTienda = usuarioActual?.nombre ?: "Eco POS",
                carrosActivos = emptyList<CarroActivo>(),
                onNuevoOrden = { navController.navigate(Rutas.VENTA) },
                onGastos = { navController.navigate("proximamente/gastos") },
                onIngresos = { navController.navigate(Rutas.MIS_VENTAS) },
                onAbrirCarro = { /* sin carritos guardados aún para el rol vendedor */ },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> { /* ya estamos aquí */ }
                        EcoPosDestino.INVENTARIO -> navController.navigate(Rutas.INVENTARIO_CONSULTA)
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate("proximamente/reportes")
                        EcoPosDestino.MENU -> navController.navigate(Rutas.MENU)
                    }
                }
            )
        }

        composable(Rutas.HOME_ADMIN) {
            PantallaHomeEcoPos(
                nombreTienda = usuarioActual?.nombre ?: "Eco POS",
                carrosActivos = emptyList<CarroActivo>(),
                onNuevoOrden = { navController.navigate(Rutas.VENTA) },
                onGastos = { navController.navigate("proximamente/gastos") },
                onIngresos = { navController.navigate(Rutas.REPORTES) },
                onAbrirCarro = { /* sin carritos guardados aún */ },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> { /* ya estamos aquí */ }
                        EcoPosDestino.INVENTARIO -> navController.navigate(Rutas.INVENTARIO)
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate(Rutas.REPORTES)
                        EcoPosDestino.MENU -> navController.navigate(Rutas.MENU)
                    }
                }
            )
        }

        composable(Rutas.MENU) {
            val esAdmin = usuarioActual?.rol == RolUsuario.ADMIN
            PantallaMenuEcoPos(
                esAdmin = esAdmin,
                onAjustes = { navController.navigate(Rutas.AJUSTES_GENERALES) },
                onClientes = { navController.navigate(Rutas.CLIENTES) },
                onDescuento = { navController.navigate(Rutas.DESCUENTOS) },
                onUnidad = { navController.navigate(Rutas.UNIDADES) },
                onEtiquetaPago = { navController.navigate(Rutas.ETIQUETAS_PAGO) },
                onCopiaSeguridad = { navController.navigate("proximamente/copia_seguridad") },
                onImportarProductos = { navController.navigate(Rutas.IMPORTAR_PRODUCTOS) },
                onImpresora = { navController.navigate(Rutas.AJUSTES_IMPRESORA) },
                onUsuarios = { navController.navigate(Rutas.USUARIOS) },
                onCambios = { navController.navigate(Rutas.CAMBIOS) },
                onCerrarSesion = { app.sessionManager.cerrarSesion() },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> navController.navigate(Rutas.VENTA)
                        EcoPosDestino.INVENTARIO -> navController.navigate(
                            if (esAdmin) Rutas.INVENTARIO else Rutas.INVENTARIO_CONSULTA
                        )
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate(
                            if (esAdmin) Rutas.REPORTES else "proximamente/reportes"
                        )
                        EcoPosDestino.MENU -> { /* ya estamos aquí */ }
                    }
                }
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

        composable(Rutas.USUARIOS) {
            PantallaUsuarios(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.INVENTARIO_CONSULTA) {
            PantallaInventarioConsulta(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.MIS_VENTAS) {
            PantallaMisVentas(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.AJUSTES_BOLETA) {
            PantallaAjustesBoleta(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.AJUSTES_IMPRESORA) {
            PantallaAjustesImpresora(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.CAMBIOS) {
            PantallaCambios(app = app, onVolver = { navController.popBackStack() })
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

        composable(Rutas.CLIENTES) {
            PantallaClientes(
                app = app,
                onVolver = { navController.popBackStack() },
                onNuevoCliente = { navController.navigate(Rutas.NUEVO_CLIENTE) }
            )
        }

        composable(Rutas.NUEVO_CLIENTE) {
            PantallaAnadirCliente(
                app = app,
                onVolver = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() }
            )
        }

        composable(Rutas.DESCUENTOS) {
            PantallaDescuentos(
                app = app,
                onVolver = { navController.popBackStack() },
                onNuevoDescuento = { navController.navigate(Rutas.NUEVO_DESCUENTO) }
            )
        }

        composable(Rutas.NUEVO_DESCUENTO) {
            PantallaAnadirDescuento(
                app = app,
                onVolver = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() }
            )
        }

        composable(Rutas.UNIDADES) {
            PantallaUnidades(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.ETIQUETAS_PAGO) {
            PantallaEtiquetasPago(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.AJUSTES_GENERALES) {
            PantallaAjustesGenerales(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.IMPORTAR_PRODUCTOS) {
            PantallaImportarProductos(app = app, onVolver = { navController.popBackStack() })
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
    else -> seccion
}
