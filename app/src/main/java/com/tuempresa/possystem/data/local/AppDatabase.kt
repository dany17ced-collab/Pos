package com.tuempresa.possystem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tuempresa.possystem.data.local.dao.CategoriaDao
import com.tuempresa.possystem.data.local.dao.CorteCajaDao
import com.tuempresa.possystem.data.local.dao.DetalleVentaDao
import com.tuempresa.possystem.data.local.dao.MovimientoInventarioDao
import com.tuempresa.possystem.data.local.dao.ProductoDao
import com.tuempresa.possystem.data.local.dao.VentaDao
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.data.local.entity.VentaEntity

@Database(
    entities = [
        CategoriaEntity::class,
        ProductoEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class,
        MovimientoInventarioEntity::class,
        CorteCajaEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoriaDao(): CategoriaDao
    abstract fun productoDao(): ProductoDao
    abstract fun ventaDao(): VentaDao
    abstract fun detalleVentaDao(): DetalleVentaDao
    abstract fun movimientoInventarioDao(): MovimientoInventarioDao
    abstract fun corteCajaDao(): CorteCajaDao

    companion object {
        private const val NOMBRE_DB = "pos_system.db"

        @Volatile
        private var instancia: AppDatabase? = null

        fun obtenerInstancia(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_DB
                )
                    // .addMigrations(MIGRATION_1_2, ...) se agrega aquí cuando cambie el esquema
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
