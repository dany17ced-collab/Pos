package com.tuempresa.possystem

import android.app.Application
import com.tuempresa.possystem.data.local.AppDatabase

class POSApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.obtenerInstancia(this) }

    override fun onCreate() {
        super.onCreate()
        // Aquí más adelante: inicializar cliente de Supabase y encolar el
        // Worker periódico de sincronización (ver data/sync/SyncWorker.kt, paso 6).
    }
}
