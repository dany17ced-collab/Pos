package com.tuempresa.possystem.presentation.ajustes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class EstadoGuardadoAjustes {
    object Inactivo : EstadoGuardadoAjustes()
    object Guardando : EstadoGuardadoAjustes()
    object Guardado : EstadoGuardadoAjustes()
    data class Error(val mensaje: String) : EstadoGuardadoAjustes()
}

/**
 * Datos de tienda en edición dentro de la pantalla de Ajustes de boleta.
 * `logoUriTemporal` es la imagen recién elegida de la galería (aún no copiada
 * a almacenamiento interno); `rutaLogoGuardada` es la ruta ya persistida de
 * una configuración previa, si existe.
 */
data class FormularioAjustesBoleta(
    val nombreTienda: String = "",
    val eslogan: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val ruc: String = "",
    val piePagina: String = "",
    val logoUriTemporal: Uri? = null,
    val rutaLogoGuardada: String? = null
)

class AjustesBoletaViewModel(private val app: POSApplication) : ViewModel() {

    private val dao = app.database.configuracionTiendaDao()

    private val _formulario = MutableStateFlow(FormularioAjustesBoleta())
    val formulario: StateFlow<FormularioAjustesBoleta> = _formulario.asStateFlow()

    private val _estadoGuardado = MutableStateFlow<EstadoGuardadoAjustes>(EstadoGuardadoAjustes.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardadoAjustes> = _estadoGuardado.asStateFlow()

    init {
        cargarConfiguracionActual()
    }

    private fun cargarConfiguracionActual() {
        viewModelScope.launch {
            val existente = dao.obtener()
            if (existente != null) {
                _formulario.value = FormularioAjustesBoleta(
                    nombreTienda = existente.nombreTienda,
                    eslogan = existente.eslogan,
                    direccion = existente.direccion,
                    telefono = existente.telefono,
                    ruc = existente.ruc,
                    piePagina = existente.piePagina,
                    rutaLogoGuardada = existente.rutaLogo
                )
            }
        }
    }

    fun actualizarNombreTienda(valor: String) { _formulario.value = _formulario.value.copy(nombreTienda = valor) }
    fun actualizarEslogan(valor: String) { _formulario.value = _formulario.value.copy(eslogan = valor) }
    fun actualizarDireccion(valor: String) { _formulario.value = _formulario.value.copy(direccion = valor) }
    fun actualizarTelefono(valor: String) { _formulario.value = _formulario.value.copy(telefono = valor) }
    fun actualizarRuc(valor: String) { _formulario.value = _formulario.value.copy(ruc = valor) }
    fun actualizarPiePagina(valor: String) { _formulario.value = _formulario.value.copy(piePagina = valor) }

    fun elegirLogo(uri: Uri) {
        _formulario.value = _formulario.value.copy(logoUriTemporal = uri)
    }

    fun quitarLogo() {
        _formulario.value = _formulario.value.copy(logoUriTemporal = null, rutaLogoGuardada = null)
    }

    /**
     * Guarda los ajustes. Si se eligió un logo nuevo, lo copia a almacenamiento
     * interno de la app (persistente, no depende de que la imagen original
     * siga existiendo en la galería del usuario).
     */
    fun guardar() {
        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardadoAjustes.Guardando
            try {
                val actual = _formulario.value
                val rutaLogoFinal = withContext(Dispatchers.IO) {
                    val uriNueva = actual.logoUriTemporal
                    when {
                        uriNueva != null -> copiarLogoAAlmacenamientoInterno(uriNueva)
                        else -> actual.rutaLogoGuardada
                    }
                }

                val existente = dao.obtener()
                val configuracion = ConfiguracionTiendaEntity(
                    nombreTienda = actual.nombreTienda.trim(),
                    eslogan = actual.eslogan.trim(),
                    direccion = actual.direccion.trim(),
                    telefono = actual.telefono.trim(),
                    ruc = actual.ruc.trim(),
                    piePagina = actual.piePagina.trim().ifBlank { "¡Gracias por su compra!" },
                    rutaLogo = rutaLogoFinal,
                    macImpresora = existente?.macImpresora,
                    nombreImpresora = existente?.nombreImpresora,
                    actualizadoEn = System.currentTimeMillis()
                )
                dao.guardar(configuracion)
                _formulario.value = actual.copy(logoUriTemporal = null, rutaLogoGuardada = rutaLogoFinal)
                _estadoGuardado.value = EstadoGuardadoAjustes.Guardado
            } catch (e: Exception) {
                _estadoGuardado.value = EstadoGuardadoAjustes.Error("No se pudo guardar. Intenta de nuevo.")
            }
        }
    }

    private fun copiarLogoAAlmacenamientoInterno(uri: Uri): String? {
        return try {
            val carpeta = File(app.filesDir, "boleta").apply { mkdirs() }
            val archivoDestino = File(carpeta, "logo.png")
            app.contentResolver.openInputStream(uri)?.use { entrada ->
                FileOutputStream(archivoDestino).use { salida ->
                    entrada.copyTo(salida)
                }
            }
            archivoDestino.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun reiniciarEstadoGuardado() {
        _estadoGuardado.value = EstadoGuardadoAjustes.Inactivo
    }
}
