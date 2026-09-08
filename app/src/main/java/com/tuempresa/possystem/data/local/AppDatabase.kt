package com.tuempresa.possystem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuempresa.possystem.data.local.dao.CategoriaDao
import com.tuempresa.possystem.data.local.dao.CorteCajaDao
import com.tuempresa.possystem.data.local.dao.DetalleVentaDao
import com.tuempresa.possystem.data.local.dao.MovimientoInventarioDao
import com.tuempresa.possystem.data.local.dao.PrecioEscalonDao
import com.tuempresa.possystem.data.local.dao.ProductoDao
import com.tuempresa.possystem.data.local.dao.UsuarioDao
import com.tuempresa.possystem.data.local.dao.VentaDao
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.PrecioEscalonEntity
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.data.local.entity.UsuarioEntity
import com.tuempresa.possystem.data.local.entity.VentaEntity

@Database(
    entities = [
        CategoriaEntity::class,
        ProductoEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class,
        MovimientoInventarioEntity::class,
        CorteCajaEntity::class,
        PrecioEscalonEntity::class,
        UsuarioEntity::class
    ],
    version = 2,
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
    abstract fun precioEscalonDao(): PrecioEscalonDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        private const val NOMBRE_DB = "pos_system.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /**
         * Migración 1 -> 2: agrega soporte de variantes por talla/color en ropa,
         * escalones de precio por cantidad, y usuarios con PIN/roles.
         *
         * Los datos de prueba de la versión 1 (productos sin talla/color) se
         * conservan intactos: las columnas nuevas de `productos` quedan en NULL
         * para esos registros existentes.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- productos: nuevas columnas talla/color ---
                db.execSQL("ALTER TABLE productos ADD COLUMN talla TEXT")
                db.execSQL("ALTER TABLE productos ADD COLUMN color TEXT")

                // El índice único anterior de codigoBarras se reemplaza por uno no-único,
                // ya que ahora el producto padre y sus variantes comparten código de barras.
                db.execSQL("DROP INDEX IF EXISTS index_productos_codigoBarras")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_codigoBarras ON productos(codigoBarras)")

                // --- tabla nueva: precios_escalon ---
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS precios_escalon (
                        id TEXT NOT NULL PRIMARY KEY,
                        productoId TEXT NOT NULL,
                        cantidadMinima INTEGER NOT NULL,
                        etiqueta TEXT NOT NULL,
                        precioUnitario REAL NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL,
                        FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_precios_escalon_productoId ON precios_escalon(productoId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_precios_escalon_productoId_cantidadMinima " +
                        "ON precios_escalon(productoId, cantidadMinima)"
                )

                // --- tabla nueva: usuarios ---
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS usuarios (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        pinHash TEXT NOT NULL,
                        pinSal TEXT NOT NULL,
                        rol TEXT NOT NULL,
                        activo INTEGER NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_nombre ON usuarios(nombre)")
            }
        }

        fun obtenerInstancia(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_DB
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
