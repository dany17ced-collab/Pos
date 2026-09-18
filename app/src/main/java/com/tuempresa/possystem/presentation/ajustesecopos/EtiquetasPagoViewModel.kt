package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.EtiquetaPagoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class EtiquetasPagoViewModel(app: POSApplication) : ViewModel() {

    private val dao = app.database.etiquetaPagoDao()

    val etiquetas: StateFlow<List<EtiquetaPagoEntity>> = dao.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun anadir(nombre: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.insertar(
                EtiquetaPagoEntity(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre.trim(),
                    orden = etiquetas.value.size
                )
            )
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch { dao.marcarEliminada(id) }
    }
}
