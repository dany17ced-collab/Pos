package com.tuempresa.possystem.presentation.ajustesecopos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.UnidadMedidaEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class UnidadesViewModel(app: POSApplication) : ViewModel() {

    private val dao = app.database.unidadMedidaDao()

    val unidades: StateFlow<List<UnidadMedidaEntity>> = dao.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun anadir(nombre: String, esFraccional: Boolean) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.insertar(
                UnidadMedidaEntity(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre.trim(),
                    esFraccional = esFraccional,
                    orden = unidades.value.size
                )
            )
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch { dao.marcarEliminada(id) }
    }
}
