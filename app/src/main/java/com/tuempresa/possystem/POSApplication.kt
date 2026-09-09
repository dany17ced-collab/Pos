package com.tuempresa.possystem

import android.app.Application
import com.tuempresa.possystem.data.local.AppDatabase
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.domain.AuthRepository
import com.tuempresa.possystem.domain.PrecioCalculator
import com.tuempresa.possystem.domain.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class POSApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.obtenerInstancia(this) }

    val authRepository: AuthRepository by lazy { AuthRepository(database.usuarioDao()) }
    val precioCalculator: PrecioCalculator by lazy { PrecioCalculator(database.precioEscalonDao()) }
    val sessionManager: SessionManager by lazy { SessionManager() }

    override fun onCreate() {
        super.onCreate()
        crearAdminInicialSiNoExiste()
        // Aquí más adelante: inicializar cliente de Supabase y encolar el
        // Worker periódico de sincronización (ver data/sync/SyncWorker.kt, paso 6).
    }

    /**
     * Si la base de datos no tiene ningún usuario todavía (primera vez que se
     * abre la app), crea un administrador por defecto con PIN 1234 para poder
     * entrar y configurar el resto del sistema (crear vendedores, productos, etc.).
     *
     * IMPORTANTE: este PIN debe cambiarse de inmediato desde el módulo de
     * Usuarios una vez dentro de la app.
     */
    private fun crearAdminInicialSiNoExiste() {
        CoroutineScope(Dispatchers.IO).launch {
            val yaHayUsuarios = database.usuarioDao().obtenerTodosActivos().isNotEmpty()
            if (!yaHayUsuarios) {
                authRepository.crearUsuario(
                    nombre = "Admin",
                    pin = "1234",
                    rol = RolUsuario.ADMIN
                )
            }
        }
    }
}
