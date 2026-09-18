package com.tuempresa.possystem.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ClienteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClientesViewModel(private val app: POSApplication) : ViewModel() {

    private val dao = app.database.clienteDao()

    val clientes: StateFlow<List<ClienteEntity>> = dao.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun guardar(
        nombre: String,
        telefono: String?,
        codigoBarras: String?,
        email: String?,
        direccion: String?,
        onGuardado: () -> Unit
    ) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.insertar(
                ClienteEntity(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre.trim(),
                    telefono = telefono?.trim()?.takeIf { it.isNotBlank() },
                    codigoBarras = codigoBarras?.trim()?.takeIf { it.isNotBlank() },
                    email = email?.trim()?.takeIf { it.isNotBlank() },
                    direccion = direccion?.trim()?.takeIf { it.isNotBlank() }
                )
            )
            onGuardado()
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch { dao.marcarEliminado(id) }
    }
}
