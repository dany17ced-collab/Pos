package com.tuempresa.possystem

import android.app.Application
import com.tuempresa.possystem.data.local.AppDatabase
import com.tuempresa.possystem.domain.AuthRepository
import com.tuempresa.possystem.domain.PrecioCalculator

class POSApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.obtenerInstancia(this) }

    val authRepository: AuthRepository by lazy { AuthRepository(database.usuarioDao()) }
    val precioCalculator: PrecioCalculator by lazy { PrecioCalculator(database.precioEscalonDao()) }

    override fun onCreate() {
        super.onCreate()
        // Aquí más adelante: inicializar cliente de Supabase y encolar el
        // Worker periódico de sincronización (ver data/sync/SyncWorker.kt, paso 6).
    }
}
