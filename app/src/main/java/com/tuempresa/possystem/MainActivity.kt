package com.tuempresa.possystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel de prueba para verificar que el CRUD de inventario funciona
 * de punta a punta (Room -> Flow -> Compose). Se reemplaza en el paso 1
 * completo por la pantalla real de inventario (presentation/inventario/).
 */
class MainViewModel(application: POSApplication) : ViewModel() {
    private val productoDao = application.database.productoDao()

    val productos = productoDao.observarProductosActivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregarProductoDePrueba() {
        viewModelScope.launch {
            productoDao.insertar(
                ProductoEntity(
                    id = UUID.randomUUID().toString(),
                    sku = "SKU-${(1000..9999).random()}",
                    codigoBarras = "750${(100000..999999).random()}",
                    nombre = "Producto de prueba ${(1..100).random()}",
                    categoriaId = null,
                    precioCompra = 10.0,
                    precioVenta = 18.0,
                    impuestoPorcentaje = 16.0,
                    stockActual = 25,
                    stockMinimo = 5
                )
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application as POSApplication) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaInventarioDemo(viewModel)
                }
            }
        }
    }
}

@Composable
fun PantallaInventarioDemo(viewModel: MainViewModel) {
    val productos by viewModel.productos.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Inventario (${productos.size} productos)", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { viewModel.agregarProductoDePrueba() }) {
                Text("Agregar producto de prueba")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(productos) { producto ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                        Text("SKU: ${producto.sku} · Stock: ${producto.stockActual} · $${producto.precioVenta}")
                    }
                }
            }
        }
    }
}
