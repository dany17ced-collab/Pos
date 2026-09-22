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
import kotlinx.coroutines.launch
import com.tuempresa.possystem.CrashHandler
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.presentation.ajustes.PantallaAjustesBoleta
import com.tuempresa.possystem.presentation.ajustes.PantallaAjustesImpresora
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaAjustesGenerales
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaCategoriasColores
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaEtiquetasPago
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaUnidades
import com.tuempresa.possystem.presentation.cambios.PantallaCambios
import com.tuempresa.possystem.presentation.clientes.PantallaAnadirCliente
import com.tuempresa.possystem.presentation.clientes.PantallaClientes
import com.tuempresa.possystem.presentation.descuentos.PantallaAnadirDescuento
import com.tuempresa.possystem.presentation.descuentos.PantallaDescuentos
import com.tuempresa.possystem.presentation.importar.PantallaImportarProductos
import com.tuempresa.possystem.presentation.home.PantallaInicioAdmin
import com.tuempresa.possystem.presentation.home.PantallaInicioVendedor
import com.tuempresa.possystem.presentation.ajustesecopos.PantallaAjustes
import com.tuempresa.possystem.presentation.inventario.PantallaInventarioCategorias
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
import com.tuempresa.possystem.presentation.caja.PantallaCierreCaja
import com.tuempresa.possystem.presentation.reportes.PantallaReportes
import com.tuempresa.possystem.presentation.reportes.PantallaReporteConsolidado
import com.tuempresa.possystem.presentation.tiendas.PantallaTiendas
import com.tuempresa.possystem.presentation.usuarios.PantallaUsuarios
import com.tuempresa.possystem.presentation.venta.PantallaMisVentas
import com.tuempresa.possystem.presentation.venta.PantallaVenta

private object Rutas {
    const val LOGIN = "login"
    const val HOME_ADMIN = "home_admin"
    const val HOME_VENDEDOR = "home_vendedor"
    const val VENTA = "venta"
    const val INVENTARIO = "inventario"
    const val INVENTARIO_CATEGORIAS = "inventario_categorias"
    const val NUEVO_PRODUCTO = "nuevo_producto"
    const val ENTRADA_MERCADERIA = "entrada_mercaderia"
    const val EDITAR_PRECIO = "editar_precio/{productoId}"
    const val REPORTES = "reportes"
    const val REPORTE_CONSOLIDADO = "reporte_consolidado"
    const val CIERRE_CAJA = "cierre_caja"
    const val USUARIOS = "usuarios"
    const val TIENDAS = "tiendas"
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
    const val CATEGORIAS_COLORES = "categorias_colores"
    const val NUEVO_CLIENTE = "nuevo_cliente"
    const val NUEVO_DESCUENTO = "nuevo_descuento"
}

@Composable
fun GrafoNavegacionPrincipal(app: POSApplication) {
    val navController = rememberNavController()
    val usuarioActual by app.sessionManager.usuarioActual.collectAsState()
    val tiendaActiva by app.sessionManager.tiendaActiva.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

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
            PantallaInicioVendedor(
                app = app,
                onNuevaVenta = { navController.navigate(Rutas.VENTA) },
                onCategoria = { navController.navigate(Rutas.INVENTARIO_CONSULTA) },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> { /* ya estamos aquí */ }
                        EcoPosDestino.INVENTARIO -> navController.navigate(Rutas.INVENTARIO_CATEGORIAS)
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate(Rutas.CIERRE_CAJA)
                        EcoPosDestino.MENU -> navController.navigate(Rutas.MENU)
                    }
                }
            )
        }

        composable(Rutas.HOME_ADMIN) {
            PantallaInicioAdmin(
                app = app,
                onSeleccionarTienda = { tiendaId ->
                    // Cambiar la tienda activa reactiva tiendaActiva vía SessionManager;
                    // el propio PantallaInicioAdmin vuelve a leer sus stats con
                    // LaunchedEffect cuando el usuario reingresa a esta pantalla.
                    coroutineScope.launch {
                        app.tiendaRepository.obtenerPorId(tiendaId)?.let { app.sessionManager.establecerTiendaActiva(it) }
                    }
                },
                onAgregarTienda = { navController.navigate(Rutas.TIENDAS) },
                onNuevaVenta = { navController.navigate(Rutas.VENTA) },
                onVendedores = { navController.navigate(Rutas.USUARIOS) },
                onReportes = { navController.navigate(Rutas.REPORTES) },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> { /* ya estamos aquí */ }
                        EcoPosDestino.INVENTARIO -> navController.navigate(Rutas.INVENTARIO_CATEGORIAS)
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate(Rutas.REPORTES)
                        EcoPosDestino.MENU -> navController.navigate(Rutas.MENU)
                    }
                }
            )
        }

        composable(Rutas.MENU) {
            val esAdmin = usuarioActual?.rol == RolUsuario.ADMIN
            PantallaAjustes(
                app = app,
                onVerTodasLasTiendas = { navController.navigate(Rutas.TIENDAS) },
                onVendedoresYPermisos = { navController.navigate(Rutas.USUARIOS) },
                onComprobantesYBoletas = { navController.navigate(Rutas.AJUSTES_BOLETA) },
                onImportarExcel = { navController.navigate(Rutas.IMPORTAR_PRODUCTOS) },
                onCategoriasYColores = { navController.navigate(Rutas.CATEGORIAS_COLORES) },
                onSeguridad = { navController.navigate(Rutas.USUARIOS) },
                onImpresora = { navController.navigate(Rutas.AJUSTES_IMPRESORA) },
                onCerrarSesion = { app.sessionManager.cerrarSesion() },
                onClientes = { navController.navigate(Rutas.CLIENTES) },
                onDescuentos = { navController.navigate(Rutas.DESCUENTOS) },
                onCambios = { navController.navigate(Rutas.CAMBIOS) },
                onUnidades = { navController.navigate(Rutas.UNIDADES) },
                onEtiquetasPago = { navController.navigate(Rutas.ETIQUETAS_PAGO) },
                onAjustesGenerales = { navController.navigate(Rutas.AJUSTES_GENERALES) },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> navController.navigate(
                            if (esAdmin) Rutas.HOME_ADMIN else Rutas.HOME_VENDEDOR
                        )
                        EcoPosDestino.INVENTARIO -> navController.navigate(Rutas.INVENTARIO_CATEGORIAS)
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate(
                            if (esAdmin) Rutas.REPORTES else Rutas.CIERRE_CAJA
                        )
                        EcoPosDestino.MENU -> { /* ya estamos aquí */ }
                    }
                }
            )
        }

        composable(Rutas.VENTA) {
            PantallaVenta(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.INVENTARIO_CATEGORIAS) {
            val esAdmin = usuarioActual?.rol == RolUsuario.ADMIN
            PantallaInventarioCategorias(
                app = app,
                esAdmin = esAdmin,
                onBuscar = { /* la propia pantalla filtra local; buscar algo concreto navega a la lista */ },
                onEscanear = { navController.navigate(if (esAdmin) Rutas.INVENTARIO else Rutas.INVENTARIO_CONSULTA) },
                onCategoria = { navController.navigate(if (esAdmin) Rutas.INVENTARIO else Rutas.INVENTARIO_CONSULTA) },
                onNavegarDestino = { destino ->
                    when (destino) {
                        EcoPosDestino.VENDER -> navController.navigate(
                            if (esAdmin) Rutas.HOME_ADMIN else Rutas.HOME_VENDEDOR
                        )
                        EcoPosDestino.INVENTARIO -> { /* ya estamos aquí */ }
                        EcoPosDestino.HISTORIAL -> navController.navigate(Rutas.MIS_VENTAS)
                        EcoPosDestino.REPORTES -> navController.navigate(
                            if (esAdmin) Rutas.REPORTES else Rutas.CIERRE_CAJA
                        )
                        EcoPosDestino.MENU -> navController.navigate(Rutas.MENU)
                    }
                }
            )
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
            PantallaReportes(
                app = app,
                onVolver = { navController.popBackStack() },
                onReporteConsolidado = { navController.navigate(Rutas.REPORTE_CONSOLIDADO) },
                onCierreCaja = { navController.navigate(Rutas.CIERRE_CAJA) }
            )
        }

        composable(Rutas.REPORTE_CONSOLIDADO) {
            PantallaReporteConsolidado(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.CIERRE_CAJA) {
            PantallaCierreCaja(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.USUARIOS) {
            PantallaUsuarios(app = app, onVolver = { navController.popBackStack() })
        }

        composable(Rutas.TIENDAS) {
            PantallaTiendas(app = app, onVolver = { navController.popBackStack() })
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

        composable(Rutas.CATEGORIAS_COLORES) {
            PantallaCategoriasColores(app = app, onVolver = { navController.popBackStack() })
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
