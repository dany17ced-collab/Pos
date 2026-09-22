package com.tuempresa.possystem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuempresa.possystem.data.local.dao.CambioDao
import com.tuempresa.possystem.data.local.dao.CategoriaDao
import com.tuempresa.possystem.data.local.dao.ClienteDao
import com.tuempresa.possystem.data.local.dao.ConfiguracionTiendaDao
import com.tuempresa.possystem.data.local.dao.CorteCajaDao
import com.tuempresa.possystem.data.local.dao.DescuentoDao
import com.tuempresa.possystem.data.local.dao.DetalleVentaDao
import com.tuempresa.possystem.data.local.dao.EtiquetaPagoDao
import com.tuempresa.possystem.data.local.dao.MovimientoInventarioDao
import com.tuempresa.possystem.data.local.dao.PrecioEscalonDao
import com.tuempresa.possystem.data.local.dao.ProductoDao
import com.tuempresa.possystem.data.local.dao.TiendaDao
import com.tuempresa.possystem.data.local.dao.UnidadMedidaDao
import com.tuempresa.possystem.data.local.dao.UsuarioDao
import com.tuempresa.possystem.data.local.dao.VentaDao
import com.tuempresa.possystem.data.local.entity.CambioEntity
import com.tuempresa.possystem.data.local.entity.CategoriaEntity
import com.tuempresa.possystem.data.local.entity.ClienteEntity
import com.tuempresa.possystem.data.local.entity.ConfiguracionTiendaEntity
import com.tuempresa.possystem.data.local.entity.CorteCajaEntity
import com.tuempresa.possystem.data.local.entity.DescuentoEntity
import com.tuempresa.possystem.data.local.entity.DetalleVentaEntity
import com.tuempresa.possystem.data.local.entity.EtiquetaPagoEntity
import com.tuempresa.possystem.data.local.entity.MovimientoInventarioEntity
import com.tuempresa.possystem.data.local.entity.PrecioEscalonEntity
import com.tuempresa.possystem.data.local.entity.ProductoEntity
import com.tuempresa.possystem.data.local.entity.TiendaEntity
import com.tuempresa.possystem.data.local.entity.UnidadMedidaEntity
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
        UsuarioEntity::class,
        ConfiguracionTiendaEntity::class,
        CambioEntity::class,
        ClienteEntity::class,
        DescuentoEntity::class,
        EtiquetaPagoEntity::class,
        UnidadMedidaEntity::class,
        TiendaEntity::class
    ],
    version = 11,
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
    abstract fun configuracionTiendaDao(): ConfiguracionTiendaDao
    abstract fun cambioDao(): CambioDao
    abstract fun clienteDao(): ClienteDao
    abstract fun descuentoDao(): DescuentoDao
    abstract fun etiquetaPagoDao(): EtiquetaPagoDao
    abstract fun unidadMedidaDao(): UnidadMedidaDao
    abstract fun tiendaDao(): TiendaDao

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

        /**
         * Migración 3 -> 4: agrega la tabla `configuracion_tienda`, usada para
         * personalizar la boleta impresa/compartida (logo, eslogan, dirección,
         * teléfono, RUC, pie de página) y recordar la última impresora Bluetooth
         * emparejada. Es una tabla de una sola fila (id fijo "config"); no existe
         * fila hasta que el usuario guarda los ajustes por primera vez.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS configuracion_tienda (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombreTienda TEXT NOT NULL,
                        eslogan TEXT NOT NULL,
                        direccion TEXT NOT NULL,
                        telefono TEXT NOT NULL,
                        ruc TEXT NOT NULL,
                        piePagina TEXT NOT NULL,
                        rutaLogo TEXT,
                        macImpresora TEXT,
                        nombreImpresora TEXT,
                        actualizadoEn INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migración 4 -> 5: agrega el módulo de cambios/devoluciones de prenda.
         *
         * - `configuracion_tienda` gana 3 columnas: el link de redes sociales
         *   (se imprime como QR en la boleta) y los plazos en días para aceptar
         *   un cambio por talla o por falla de fábrica.
         * - Tabla nueva `cambios`: cada fila es un cambio de una variante por
         *   otra, siempre vinculado a la venta original (nunca "suelto").
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE configuracion_tienda ADD COLUMN linkRedesSociales TEXT")
                db.execSQL("ALTER TABLE configuracion_tienda ADD COLUMN diasPlazoCambioTalla INTEGER NOT NULL DEFAULT 7")
                db.execSQL("ALTER TABLE configuracion_tienda ADD COLUMN diasPlazoCambioFabrica INTEGER NOT NULL DEFAULT 30")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cambios (
                        id TEXT NOT NULL PRIMARY KEY,
                        ventaOriginalId TEXT NOT NULL,
                        folioVentaOriginal INTEGER,
                        productoDevueltoId TEXT NOT NULL,
                        nombreProductoDevuelto TEXT NOT NULL,
                        precioUnitarioDevuelto REAL NOT NULL,
                        productoEntregadoId TEXT NOT NULL,
                        nombreProductoEntregado TEXT NOT NULL,
                        precioUnitarioEntregado REAL NOT NULL,
                        cantidad INTEGER NOT NULL,
                        motivo TEXT NOT NULL,
                        diferencia REAL NOT NULL,
                        fueraDePlazo INTEGER NOT NULL,
                        usuarioId TEXT,
                        fecha INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        FOREIGN KEY(ventaOriginalId) REFERENCES ventas(id) ON DELETE RESTRICT,
                        FOREIGN KEY(productoDevueltoId) REFERENCES productos(id) ON DELETE RESTRICT,
                        FOREIGN KEY(productoEntregadoId) REFERENCES productos(id) ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cambios_ventaOriginalId ON cambios(ventaOriginalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cambios_productoDevueltoId ON cambios(productoDevueltoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cambios_productoEntregadoId ON cambios(productoEntregadoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cambios_motivo ON cambios(motivo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cambios_fecha ON cambios(fecha)")
            }
        }

        /**
         * v5 -> v6: `configuracion_tienda` gana `descripcion`, un campo opcional
         * para contar qué ofrece la tienda (se imprime debajo del eslogan en la
         * boleta solo cuando no está vacío).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE configuracion_tienda ADD COLUMN descripcion TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v6 -> v7: `configuracion_tienda` gana `politicaCambios`, un texto
         * opcional con la ley/plazo/condiciones de cambios y devoluciones que
         * se imprime como advertencia debajo del QR de cambios en la boleta.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE configuracion_tienda ADD COLUMN politicaCambios TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v7 -> v8: agrega a `ventas` el tipo de comprobante (boleta/factura) y los
         * datos opcionales del cliente (documento/nombre, y RUC/razón social para
         * factura), capturados justo antes de emitir el comprobante.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ventas ADD COLUMN tipoComprobante TEXT NOT NULL DEFAULT 'BOLETA'")
                db.execSQL("ALTER TABLE ventas ADD COLUMN clienteDocumento TEXT")
                db.execSQL("ALTER TABLE ventas ADD COLUMN clienteNombre TEXT")
                db.execSQL("ALTER TABLE ventas ADD COLUMN clienteRuc TEXT")
                db.execSQL("ALTER TABLE ventas ADD COLUMN clienteRazonSocial TEXT")
            }
        }

        /**
         * v8 -> v9: agrega las tablas del look "Eco POS" del menú: clientes,
         * descuentos reutilizables en el carrito, etiquetas de pago editables
         * (Efectivo/Yape/Transferencia...) y el catálogo de unidades de medida
         * (pcs, kg, mL...) usado en "Precio > Unidad" del formulario de producto.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS clientes (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        telefono TEXT,
                        codigoBarras TEXT,
                        email TEXT,
                        direccion TEXT,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_nombre ON clientes(nombre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_codigoBarras ON clientes(codigoBarras)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS descuentos (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        codigoPromocional TEXT,
                        descripcion TEXT,
                        tipoValor TEXT NOT NULL,
                        valor REAL NOT NULL,
                        productoId TEXT,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS etiquetas_pago (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        metodoPagoBase TEXT NOT NULL,
                        esEditable INTEGER NOT NULL,
                        orden INTEGER NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS unidades_medida (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        esFraccional INTEGER NOT NULL,
                        esEditable INTEGER NOT NULL,
                        orden INTEGER NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Etiquetas de pago de fábrica, igual a las precargadas en Eco POS.
                val ahora = System.currentTimeMillis()
                val etiquetasFabrica = listOf(
                    Triple("Cash", "EFECTIVO", false),
                    Triple("Efectivo", "EFECTIVO", true),
                    Triple("Yape", "TRANSFERENCIA", true),
                    Triple("Transferencia", "TRANSFERENCIA", true)
                )
                etiquetasFabrica.forEachIndexed { index, (nombre, metodoBase, editable) ->
                    db.execSQL(
                        """
                        INSERT INTO etiquetas_pago
                        (id, nombre, metodoPagoBase, esEditable, orden, creadoEn, actualizadoEn, sincronizado, eliminado)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0)
                        """.trimIndent(),
                        arrayOf(
                            java.util.UUID.randomUUID().toString(), nombre, metodoBase,
                            if (editable) 1 else 0, index, ahora, ahora
                        )
                    )
                }

                // Unidades de fábrica, igual al catálogo pcs/mg/g/kg/... de Eco POS.
                val unidadesFabrica = listOf(
                    Pair("pcs", false), Pair("mg", true), Pair("g", true), Pair("kg", true),
                    Pair("oz", true), Pair("lb", true), Pair("mL", true), Pair("L", true),
                    Pair("qt", true), Pair("gal", true), Pair("cm", true), Pair("m", true),
                    Pair("in", true), Pair("ft", true)
                )
                unidadesFabrica.forEachIndexed { index, (nombre, fraccional) ->
                    db.execSQL(
                        """
                        INSERT INTO unidades_medida
                        (id, nombre, esFraccional, esEditable, orden, creadoEn, sincronizado, eliminado)
                        VALUES (?, ?, ?, 0, ?, ?, 0, 0)
                        """.trimIndent(),
                        arrayOf(
                            java.util.UUID.randomUUID().toString(), nombre,
                            if (fraccional) 1 else 0, index, ahora
                        )
                    )
                }
            }
        }

        /**
         * v9 -> v10: soporte multitienda. Crea la tabla `tiendas` y una
         * "Tienda Principal" por defecto para no dejar huérfanos los datos
         * existentes, luego agrega `tiendaId` a `productos`, `ventas` y
         * `configuracion_tienda` apuntando todos a esa tienda por defecto.
         *
         * `productos` y `ventas` se recrean (en vez de ALTER TABLE ADD COLUMN)
         * porque `tiendaId` es NOT NULL con FOREIGN KEY, y SQLite no permite
         * agregar así una columna a una tabla con filas existentes en un solo
         * paso. `configuracion_tienda` también se recrea porque cambia de ser
         * una tabla de fila única (id fijo "config") a una fila por tienda.
         *
         * También se recalcula el índice único de sku de global a
         * (tiendaId, sku), ya que dos sucursales pueden repetir un SKU.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val ahora = System.currentTimeMillis()
                val idTiendaPrincipal = TiendaEntity.ID_TIENDA_PRINCIPAL

                // --- tabla nueva: tiendas ---
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tiendas (
                        id TEXT NOT NULL PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        activa INTEGER NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO tiendas (id, nombre, activa, creadoEn, actualizadoEn, sincronizado, eliminado)
                    VALUES ('$idTiendaPrincipal', 'Tienda Principal', 1, $ahora, $ahora, 0, 0)
                    """.trimIndent()
                )

                // --- productos: agrega tiendaId (recreación de tabla) ---
                db.execSQL(
                    """
                    CREATE TABLE productos_nueva (
                        id TEXT NOT NULL PRIMARY KEY,
                        tiendaId TEXT NOT NULL,
                        sku TEXT NOT NULL,
                        codigoBarras TEXT,
                        nombre TEXT NOT NULL,
                        descripcion TEXT,
                        categoriaId TEXT,
                        precioCompra REAL NOT NULL,
                        precioVenta REAL NOT NULL,
                        impuestoPorcentaje REAL NOT NULL,
                        stockActual INTEGER NOT NULL,
                        stockMinimo INTEGER NOT NULL,
                        stockMaximo INTEGER,
                        fotoUrl TEXT,
                        productoBaseId TEXT,
                        nombreVariante TEXT,
                        talla TEXT,
                        color TEXT,
                        activo INTEGER NOT NULL,
                        creadoEn INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        eliminado INTEGER NOT NULL,
                        FOREIGN KEY(categoriaId) REFERENCES categorias(id) ON DELETE SET NULL,
                        FOREIGN KEY(productoBaseId) REFERENCES productos_nueva(id) ON DELETE CASCADE,
                        FOREIGN KEY(tiendaId) REFERENCES tiendas(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO productos_nueva
                    SELECT id, '$idTiendaPrincipal', sku, codigoBarras, nombre, descripcion, categoriaId,
                        precioCompra, precioVenta, impuestoPorcentaje, stockActual, stockMinimo, stockMaximo,
                        fotoUrl, productoBaseId, nombreVariante, talla, color, activo,
                        creadoEn, actualizadoEn, sincronizado, eliminado
                    FROM productos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE productos")
                db.execSQL("ALTER TABLE productos_nueva RENAME TO productos")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_categoriaId ON productos(categoriaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_productoBaseId ON productos(productoBaseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_tiendaId ON productos(tiendaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_codigoBarras ON productos(codigoBarras)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_productos_tiendaId_sku ON productos(tiendaId, sku)"
                )

                // --- ventas: agrega tiendaId (recreación de tabla) ---
                db.execSQL(
                    """
                    CREATE TABLE ventas_nueva (
                        id TEXT NOT NULL PRIMARY KEY,
                        tiendaId TEXT NOT NULL,
                        folio INTEGER,
                        fecha INTEGER NOT NULL,
                        subtotal REAL NOT NULL,
                        impuestos REAL NOT NULL,
                        descuento REAL NOT NULL,
                        total REAL NOT NULL,
                        metodoPago TEXT NOT NULL,
                        montoRecibido REAL,
                        cambio REAL,
                        cajaId TEXT NOT NULL,
                        usuarioId TEXT,
                        estado TEXT NOT NULL,
                        notaAnulacion TEXT,
                        tipoComprobante TEXT NOT NULL,
                        clienteDocumento TEXT,
                        clienteNombre TEXT,
                        clienteRuc TEXT,
                        clienteRazonSocial TEXT,
                        creadoEn INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL,
                        FOREIGN KEY(tiendaId) REFERENCES tiendas(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO ventas_nueva
                    SELECT id, '$idTiendaPrincipal', folio, fecha, subtotal, impuestos, descuento, total,
                        metodoPago, montoRecibido, cambio, cajaId, usuarioId, estado, notaAnulacion,
                        tipoComprobante, clienteDocumento, clienteNombre, clienteRuc, clienteRazonSocial,
                        creadoEn, sincronizado
                    FROM ventas
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE ventas")
                db.execSQL("ALTER TABLE ventas_nueva RENAME TO ventas")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_fecha ON ventas(fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_cajaId ON ventas(cajaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_sincronizado ON ventas(sincronizado)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_tiendaId ON ventas(tiendaId)")

                // --- configuracion_tienda: de fila única a una fila por tienda ---
                db.execSQL(
                    """
                    CREATE TABLE configuracion_tienda_nueva (
                        id TEXT NOT NULL PRIMARY KEY,
                        tiendaId TEXT NOT NULL,
                        nombreTienda TEXT NOT NULL,
                        eslogan TEXT NOT NULL,
                        descripcion TEXT NOT NULL,
                        direccion TEXT NOT NULL,
                        telefono TEXT NOT NULL,
                        ruc TEXT NOT NULL,
                        piePagina TEXT NOT NULL,
                        rutaLogo TEXT,
                        macImpresora TEXT,
                        nombreImpresora TEXT,
                        linkRedesSociales TEXT,
                        diasPlazoCambioTalla INTEGER NOT NULL,
                        diasPlazoCambioFabrica INTEGER NOT NULL,
                        politicaCambios TEXT NOT NULL,
                        actualizadoEn INTEGER NOT NULL,
                        FOREIGN KEY(tiendaId) REFERENCES tiendas(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO configuracion_tienda_nueva
                    SELECT id, '$idTiendaPrincipal', nombreTienda, eslogan, descripcion, direccion, telefono,
                        ruc, piePagina, rutaLogo, macImpresora, nombreImpresora, linkRedesSociales,
                        diasPlazoCambioTalla, diasPlazoCambioFabrica, politicaCambios, actualizadoEn
                    FROM configuracion_tienda
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE configuracion_tienda")
                db.execSQL("ALTER TABLE configuracion_tienda_nueva RENAME TO configuracion_tienda")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_configuracion_tienda_tiendaId ON configuracion_tienda(tiendaId)"
                )
                // Si la tabla vino vacía (nunca se guardó configuración de boleta),
                // se crea una fila por defecto para que la Tienda Principal siempre
                // tenga dónde guardar su nombre/RUC/impresora sin crashear al leer.
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO configuracion_tienda
                    (id, tiendaId, nombreTienda, eslogan, descripcion, direccion, telefono, ruc, piePagina,
                     diasPlazoCambioTalla, diasPlazoCambioFabrica, politicaCambios, actualizadoEn)
                    SELECT 'config-$idTiendaPrincipal', '$idTiendaPrincipal', '', '', '', '', '', '',
                        '¡Gracias por su compra!', 7, 30, '', $ahora
                    WHERE NOT EXISTS (SELECT 1 FROM configuracion_tienda WHERE tiendaId = '$idTiendaPrincipal')
                    """.trimIndent()
                )
            }
        }

        /**
         * v10 -> v11: agrega `tiendaId` a `cortes_caja`. Se recrea la tabla porque
         * `tiendaId` es NOT NULL con FOREIGN KEY. Los cortes existentes se asignan
         * a la Tienda Principal (la única tienda que puede existir en una base que
         * viene de v10, ya que multitienda recién se agregó en la migración anterior).
         *
         * También se ajusta calcularResumen() de forma implícita: como cajaId ya
         * viene de cajaIdDeTienda(tiendaId) desde el código de app (único por
         * tienda), no hace falta cambiar esa query — pero el historial y los
         * reportes ahora sí pueden filtrar explícitamente por tiendaId.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val idTiendaPrincipal = TiendaEntity.ID_TIENDA_PRINCIPAL

                db.execSQL(
                    """
                    CREATE TABLE cortes_caja_nueva (
                        id TEXT NOT NULL PRIMARY KEY,
                        tiendaId TEXT NOT NULL,
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
                        sincronizado INTEGER NOT NULL,
                        FOREIGN KEY(tiendaId) REFERENCES tiendas(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO cortes_caja_nueva
                    SELECT id, '$idTiendaPrincipal', cajaId, tipo, fechaInicio, fechaCorte,
                        numeroTransacciones, totalVentas, totalEfectivo, totalTarjeta, totalTransferencia,
                        totalDescuentos, totalImpuestos, fondoInicial, efectivoEsperado, efectivoContado,
                        diferencia, usuarioId, creadoEn, sincronizado
                    FROM cortes_caja
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE cortes_caja")
                db.execSQL("ALTER TABLE cortes_caja_nueva RENAME TO cortes_caja")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cortes_caja_cajaId ON cortes_caja(cajaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cortes_caja_fechaCorte ON cortes_caja(fechaCorte)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cortes_caja_tiendaId ON cortes_caja(tiendaId)")

                // Los cajaId antiguos quedaron como "caja-principal" (el valor fijo
                // que usaba el código antes de derivarlo de la tienda); se
                // actualizan al nuevo formato caja-<tiendaId> para que coincidan
                // con lo que el código genera ahora vía cajaIdDeTienda(). Se corrige
                // tanto en cortes_caja como en ventas, que usan el mismo cajaId.
                db.execSQL(
                    "UPDATE cortes_caja SET cajaId = 'caja-$idTiendaPrincipal' WHERE cajaId = 'caja-principal'"
                )
                db.execSQL(
                    "UPDATE ventas SET cajaId = 'caja-$idTiendaPrincipal' WHERE cajaId = 'caja-principal'"
                )
            }
        }

        fun obtenerInstancia(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_DB
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11
                    )
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
