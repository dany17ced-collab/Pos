package com.tuempresa.possystem.presentation.reportes

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.ProductoMasVendido
import com.tuempresa.possystem.domain.reportes.ExportadorExcel
import com.tuempresa.possystem.domain.reportes.ExportadorPdf
import com.tuempresa.possystem.domain.reportes.GeneradorDatosReporte
import com.tuempresa.possystem.domain.reportes.PeriodoReporte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

enum class FormatoExportacion { EXCEL, PDF }

sealed class EstadoExportacion {
    object Inactivo : EstadoExportacion()
    object Generando : EstadoExportacion()
    data class Exitoso(val uri: Uri, val nombreArchivo: String) : EstadoExportacion()
    data class Error(val mensaje: String) : EstadoExportacion()
}

/**
 * Exportación de reportes detallados (Excel/PDF) de la tienda activa, y
 * ranking de productos más vendidos hoy. Exclusivo ADMIN.
 *
 * El cierre de caja (corte X/Z, resumen de turno) vive aparte en
 * CierreCajaViewModel/PantallaCierreCaja, accesible también para VENDEDOR —
 * antes ambas cosas estaban mezcladas aquí, lo que dejaba sin acceso a
 * cerrar caja a quien no fuera administrador.
 */
class ReportesViewModel(private val app: POSApplication) : ViewModel() {

    private val detalleVentaDao = app.database.detalleVentaDao()
    private val generadorReporte = GeneradorDatosReporte(app)

    private val _estadoExportacion = MutableStateFlow<EstadoExportacion>(EstadoExportacion.Inactivo)
    val estadoExportacion: StateFlow<EstadoExportacion> = _estadoExportacion.asStateFlow()

    // Rango fijo "hoy" para el ranking de más vendidos.
    private val inicioDeHoy: Long
        get() {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val masVendidosHoy: StateFlow<List<ProductoMasVendido>> =
        detalleVentaDao.observarProductosMasVendidos(inicioDeHoy, Long.MAX_VALUE, limite = 10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Genera el reporte detallado (semanal, mensual, o un rango personalizado)
     * de la tienda activa en el formato pedido y lo deja en cacheDir/reportes,
     * listo para compartir/abrir con un Uri de FileProvider (nunca se expone
     * una ruta file:// directa).
     *
     * Para PERSONALIZADO, `desde`/`hasta` son obligatorios (epoch millis del
     * primer y último día del rango elegido por el usuario).
     */
    fun exportarReporte(
        periodo: PeriodoReporte,
        formato: FormatoExportacion,
        desde: Long? = null,
        hasta: Long? = null
    ) {
        viewModelScope.launch {
            _estadoExportacion.value = EstadoExportacion.Generando
            try {
                val datos = withContext(Dispatchers.IO) {
                    generadorReporte.generar(
                        periodo = periodo,
                        desdePersonalizado = desde,
                        hastaPersonalizado = hasta
                    )
                }

                val carpeta = File(app.cacheDir, "reportes").apply { mkdirs() }
                val marcaTiempo = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(java.util.Date())
                val prefijoPeriodo = when (periodo) {
                    PeriodoReporte.SEMANAL -> "semanal"
                    PeriodoReporte.MENSUAL -> "mensual"
                    PeriodoReporte.PERSONALIZADO -> "personalizado"
                }
                val extension = if (formato == FormatoExportacion.EXCEL) "xlsx" else "pdf"
                val nombreArchivo = "reporte_${prefijoPeriodo}_$marcaTiempo.$extension"
                val archivo = File(carpeta, nombreArchivo)

                withContext(Dispatchers.IO) {
                    when (formato) {
                        FormatoExportacion.EXCEL -> ExportadorExcel.exportar(datos, archivo)
                        FormatoExportacion.PDF -> ExportadorPdf.exportar(datos, archivo)
                    }
                }

                val uri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    archivo
                )

                _estadoExportacion.value = EstadoExportacion.Exitoso(uri, nombreArchivo)
            } catch (ex: Exception) {
                _estadoExportacion.value = EstadoExportacion.Error(
                    ex.message ?: "No se pudo generar el reporte."
                )
            }
        }
    }

    fun limpiarEstadoExportacion() {
        _estadoExportacion.value = EstadoExportacion.Inactivo
    }
}
