package com.tuempresa.possystem.presentation.descuentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.DescuentoEntity
import com.tuempresa.possystem.data.local.entity.TipoValorDescuento
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DescuentosViewModel(private val app: POSApplication) : ViewModel() {

    private val dao = app.database.descuentoDao()

    val descuentos: StateFlow<List<DescuentoEntity>> = dao.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun guardar(
        nombre: String,
        codigoPromocional: String?,
        descripcion: String?,
        tipoValor: TipoValorDescuento,
        valor: Double,
        productoId: String?,
        onGuardado: () -> Unit
    ) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            dao.insertar(
                DescuentoEntity(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre.trim(),
                    codigoPromocional = codigoPromocional?.trim()?.takeIf { it.isNotBlank() },
                    descripcion = descripcion?.trim()?.takeIf { it.isNotBlank() },
                    tipoValor = tipoValor,
                    valor = valor,
                    productoId = productoId
                )
            )
            onGuardado()
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch { dao.marcarEliminado(id) }
    }
}
