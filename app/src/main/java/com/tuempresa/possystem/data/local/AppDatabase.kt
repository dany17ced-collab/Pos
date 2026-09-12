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
    version = 3,
    exportSchema = false
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

        /**
         * Migración 2 -> 3: rehace `cortes_caja` con el schema real que usa
         * ReportesViewModel (cajaId, fechaInicio, totalImpuestos, efectivoEsperado,
         * efectivoContado, usuarioId, sincronizado) y cambia su id de entero
         * autogenerado a String (UUID), igual que el resto de tablas nuevas.
         *
         * Como cambia el tipo de la clave primaria, no se puede hacer con
         * ALTER TABLE: se crea la tabla nueva y se migran los cortes viejos
         * dándoles un id de texto nuevo y cajaId = 'caja-principal' (única caja
         * que ha existido hasta ahora). fechaInicio no existía antes, así que
         * para los cortes históricos se aproxima con su propia fechaCorte.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cortes_caja_nueva (
                        id TEXT NOT NULL PRIMARY KEY,
                        cajaId TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        fechaInicio INTEGER NOT NULL,
                        fechaCorte INTEGER NOT NULL,
                        numeroTransacciones INTEGER NOT NULL,
                        totalVentas REAL NOT NULL,
                        totalEfectivo REAL NOT NULL,
                        totalTarjeta REAL NOT NULL,
                        totalTransferencia REAL NOT NULL,
                        totalDescuentos REAL NOT NULL,
                        totalImpuestos REAL NOT NULL,
                        fondoInicial REAL NOT NULL,
                        efectivoEsperado REAL NOT NULL,
                        efectivoContado REAL,
                        diferencia REAL,
                        usuarioId TEXT,
                        creadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO cortes_caja_nueva (
                        id, cajaId, tipo, fechaInicio, fechaCorte, numeroTransacciones,
                        totalVentas, totalEfectivo, totalTarjeta, totalTransferencia,
                        totalDescuentos, totalImpuestos, fondoInicial, efectivoEsperado,
                        efectivoContado, diferencia, usuarioId, creadoEn, sincronizado
                    )
                    SELECT
                        lower(hex(randomblob(16))), 'caja-principal', tipo, fechaCorte, fechaCorte,
                        numeroTransacciones, totalVentas, totalEfectivo, totalTarjeta,
                        totalTransferencia, totalDescuentos, 0.0, fondoInicial,
                        fondoInicial + totalEfectivo, efectivoDeclarado, diferencia, NULL,
                        fechaCorte, 0
                    FROM cortes_caja
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE cortes_caja")
                db.execSQL("ALTER TABLE cortes_caja_nueva RENAME TO cortes_caja")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cortes_caja_cajaId ON cortes_caja(cajaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cortes_caja_fechaCorte ON cortes_caja(fechaCorte)")
            }
        }

        fun obtenerInstancia(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_DB
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
