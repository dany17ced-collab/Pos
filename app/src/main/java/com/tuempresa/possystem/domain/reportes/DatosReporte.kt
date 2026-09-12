package com.tuempresa.possystem.domain.reportes

import com.tuempresa.possystem.POSApplication
import com.tuempresa.possystem.data.local.dao.ProductoMasVendido
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.VentaEntity
import java.util.Calendar

enum class PeriodoReporte {
    SEMANAL, MENSUAL
}

/** Una fila del inventario en el momento de generar el reporte. */
data class FilaInventarioReporte(
    val sku: String,
    val nombre: String,
    val categoria: String,
    val stockActual: Int,
    val stockMinimo: Int,
    val precioCompra: Double,
    val precioVenta: Double,
    val valorCosto: Double,   // stockActual * precioCompra
    val valorVenta: Double,   // stockActual * precioVenta
    val bajoMinimo: Boolean
)

/** Ventas agrupadas por día, para el desglose diario del reporte. */
data class FilaVentaDiaria(
    val fecha: Long, // inicio del día (epoch millis)
    val numeroVentas: Int,
    val totalVentas: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val totalTransferencia: Double,
    val margenBruto: Double
)

/**
 * Todo lo necesario para armar el reporte detallado (semanal o mensual),
 * ya consolidado y listo para volcar a Excel o PDF sin volver a tocar la BD.
 */
data class DatosReporte(
    val periodo: PeriodoReporte,
    val desde: Long,
    val hasta: Long,
    val generadoEn: Long,
    val generadoPor: String,

    // Resumen general del periodo
    val totalVentas: Double,
    val totalEfectivo: Double,
    val totalTarjeta: Double,
    val totalTransferencia: Double,
    val totalDescuentos: Double,
    val totalImpuestos: Double,
    val margenBrutoTotal: Double,
    val numeroTransacciones: Int,
    val numeroVentasAnuladas: Int,
    val ticketPromedio: Double,

    val ventasPorDia: List<FilaVentaDiaria>,
    val productosMasVendidos: List<ProductoMasVendido>,
    val cortesDeCaja: List<CorteCajaEntity>,
    val inventarioActual: List<FilaInventarioReporte>,

    val valorTotalInventarioCosto: Double,
    val valorTotalInventarioVenta: Double,
    val productosStockBajo: Int
)

/**
 * Recopila todos los datos del reporte desde la base de datos local.
 * No depende de la UI: se puede llamar desde cualquier ViewModel.
 */
class GeneradorDatosReporte(private val app: POSApplication) {

    suspend fun generar(periodo: PeriodoReporte, referencia: Long = System.currentTimeMillis()): DatosReporte {
        val (desde, hasta) = rangoParaPeriodo(periodo, referencia)

        val ventaDao = app.database.ventaDao()
        val detalleVentaDao = app.database.detalleVentaDao()
        val corteCajaDao = app.database.corteCajaDao()
        val productoDao = app.database.productoDao()
        val categoriaDao = app.database.categoriaDao()

        val ventas = ventaDao.obtenerVentasEnRango(desde, hasta)

        val completadas = ventas.filter { it.estado == EstadoVenta.COMPLETADA }
        val totalVentas = completadas.sumOf { it.total }
        val totalEfectivo = completadas.filter { it.metodoPago == MetodoPago.EFECTIVO }.sumOf { it.total }
        val totalTarjeta = completadas.filter { it.metodoPago == MetodoPago.TARJETA }.sumOf { it.total }
        val totalTransferencia = completadas
            .filter { it.metodoPago == MetodoPago.TRANSFERENCIA || it.metodoPago == MetodoPago.QR }
            .sumOf { it.total }
        val totalDescuentos = completadas.sumOf { it.descuento }
        val totalImpuestos = completadas.sumOf { it.impuestos }
        val margenBrutoTotal = detalleVentaDao.calcularMargenTotal(desde, hasta)

        val masVendidos = detalleVentaDao.obtenerProductosMasVendidos(desde, hasta, limite = 30)

        val cortes = corteCajaDao.obtenerHistorialEnRango(desde, hasta)

        val productos = productoDao.obtenerTodosActivos()
        val categorias = categoriaDao.obtenerTodas().associateBy { it.id }

        val inventario = productos.map { p ->
            FilaInventarioReporte(
                sku = p.sku,
                nombre = p.nombreVariante?.let { "${p.nombre} ($it)" } ?: p.nombre,
                categoria = p.categoriaId?.let { categorias[it]?.nombre } ?: "Sin categoría",
                stockActual = p.stockActual,
                stockMinimo = p.stockMinimo,
                precioCompra = p.precioCompra,
                precioVenta = p.precioVenta,
                valorCosto = p.stockActual * p.precioCompra,
                valorVenta = p.stockActual * p.precioVenta,
                bajoMinimo = p.stockActual <= p.stockMinimo
            )
        }.sortedBy { it.nombre }

        val ventasPorDia = agruparPorDia(completadas, desde, hasta)

        return DatosReporte(
            periodo = periodo,
            desde = desde,
            hasta = hasta,
            generadoEn = System.currentTimeMillis(),
            generadoPor = app.sessionManager.usuarioActual.value?.nombre ?: "—",
            totalVentas = totalVentas,
            totalEfectivo = totalEfectivo,
            totalTarjeta = totalTarjeta,
            totalTransferencia = totalTransferencia,
            totalDescuentos = totalDescuentos,
            totalImpuestos = totalImpuestos,
            margenBrutoTotal = margenBrutoTotal,
            numeroTransacciones = completadas.size,
            numeroVentasAnuladas = ventas.size - completadas.size,
            ticketPromedio = if (completadas.isNotEmpty()) totalVentas / completadas.size else 0.0,
            ventasPorDia = ventasPorDia,
            productosMasVendidos = masVendidos,
            cortesDeCaja = cortes,
            inventarioActual = inventario,
            valorTotalInventarioCosto = inventario.sumOf { it.valorCosto },
            valorTotalInventarioVenta = inventario.sumOf { it.valorVenta },
            productosStockBajo = inventario.count { it.bajoMinimo }
        )
    }

    private fun agruparPorDia(ventas: List<VentaEntity>, desde: Long, hasta: Long): List<FilaVentaDiaria> {
        val cal = Calendar.getInstance()

        fun inicioDelDia(fecha: Long): Long {
            cal.timeInMillis = fecha
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val porDia = ventas.groupBy { inicioDelDia(it.fecha) }

        val dias = mutableListOf<FilaVentaDiaria>()
        var cursor = inicioDelDia(desde)
        val fin = inicioDelDia(hasta)
        while (cursor <= fin) {
            val ventasDelDia = porDia[cursor].orEmpty()
            dias.add(
                FilaVentaDiaria(
                    fecha = cursor,
                    numeroVentas = ventasDelDia.size,
                    totalVentas = ventasDelDia.sumOf { it.total },
                    totalEfectivo = ventasDelDia.filter { it.metodoPago == MetodoPago.EFECTIVO }.sumOf { it.total },
                    totalTarjeta = ventasDelDia.filter { it.metodoPago == MetodoPago.TARJETA }.sumOf { it.total },
                    totalTransferencia = ventasDelDia
                        .filter { it.metodoPago == MetodoPago.TRANSFERENCIA || it.metodoPago == MetodoPago.QR }
                        .sumOf { it.total },
                    margenBruto = 0.0 // se completa a nivel de UI/PDF si se requiere detalle por día; el total general ya viene en margenBrutoTotal
                )
            )
            cursor += 24L * 60 * 60 * 1000
        }
        return dias
    }

    private fun rangoParaPeriodo(periodo: PeriodoReporte, referencia: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = referencia
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val hasta = cal.timeInMillis

        cal.timeInMillis = referencia
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        when (periodo) {
            PeriodoReporte.SEMANAL -> cal.add(Calendar.DAY_OF_YEAR, -6) // últimos 7 días, incluyendo hoy
            PeriodoReporte.MENSUAL -> cal.add(Calendar.DAY_OF_YEAR, -29) // últimos 30 días, incluyendo hoy
        }
        val desde = cal.timeInMillis

        return desde to hasta
    }
}
