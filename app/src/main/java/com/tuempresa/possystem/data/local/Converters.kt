package com.tuempresa.possystem.data.local

import androidx.room.TypeConverter
import com.tuempresa.possystem.data.local.entity.EstadoVenta
import com.tuempresa.possystem.data.local.entity.MetodoPago
import com.tuempresa.possystem.data.local.entity.MotivoCambio
import com.tuempresa.possystem.data.local.entity.RolUsuario
import com.tuempresa.possystem.data.local.entity.TipoComprobante
import com.tuempresa.possystem.data.local.entity.TipoCorte
import com.tuempresa.possystem.data.local.entity.TipoMovimiento

/**
 * Room no soporta enums de forma nativa: los guardamos como texto (nombre del enum)
 * y los reconstruimos al leer. Esto también hace que el dato sea legible
 * directamente en la tabla de Supabase/Postgres (columna tipo texto).
 */
class Converters {

    @TypeConverter
    fun fromMetodoPago(valor: MetodoPago): String = valor.name

    @TypeConverter
    fun toMetodoPago(valor: String): MetodoPago = MetodoPago.valueOf(valor)

    @TypeConverter
    fun fromEstadoVenta(valor: EstadoVenta): String = valor.name

    @TypeConverter
    fun toEstadoVenta(valor: String): EstadoVenta = EstadoVenta.valueOf(valor)

    @TypeConverter
    fun fromTipoMovimiento(valor: TipoMovimiento): String = valor.name

    @TypeConverter
    fun toTipoMovimiento(valor: String): TipoMovimiento = TipoMovimiento.valueOf(valor)

    @TypeConverter
    fun fromTipoCorte(valor: TipoCorte): String = valor.name

    @TypeConverter
    fun toTipoCorte(valor: String): TipoCorte = TipoCorte.valueOf(valor)

    @TypeConverter
    fun fromRolUsuario(valor: RolUsuario): String = valor.name

    @TypeConverter
    fun toRolUsuario(valor: String): RolUsuario = RolUsuario.valueOf(valor)

    @TypeConverter
    fun fromTipoComprobante(valor: TipoComprobante): String = valor.name

    @TypeConverter
    fun toTipoComprobante(valor: String): TipoComprobante = TipoComprobante.valueOf(valor)

    @TypeConverter
    fun fromMotivoCambio(valor: MotivoCambio): String = valor.name

    @TypeConverter
    fun toMotivoCambio(valor: String): MotivoCambio = MotivoCambio.valueOf(valor)
}
