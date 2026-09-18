# POS System — Kotlin + Jetpack Compose + Supabase

## Qué incluye este paso (Paso 1: Base de datos local)

- Estructura completa del proyecto Gradle (Kotlin DSL).
- Dependencias configuradas: Compose, Room, Supabase (Postgrest/Realtime/Storage/Auth),
  ML Kit + CameraX, WorkManager, iText (PDF), Coil.
- **Entidades Room**: `CategoriaEntity`, `ProductoEntity` (con variantes), `VentaEntity`,
  `DetalleVentaEntity`, `MovimientoInventarioEntity`, `CorteCajaEntity`.
- **DAOs** con la lógica de negocio ya resuelta:
  - `VentaDao.registrarVentaCompleta()`: transacción atómica que valida stock,
    descuenta inventario y registra la venta — o revierte todo si falla.
  - `MovimientoInventarioDao`: entradas (compras), salidas (mermas) y ajustes (conteo físico).
  - `CorteCajaDao.calcularResumen()`: agregados listos para el corte X/Z.
  - `DetalleVentaDao.observarProductosMasVendidos()`: reporte con margen real.
- `MainActivity` con una pantalla de prueba que confirma que Room + Compose + Flow
  funcionan de punta a punta (agrega productos y los ves en una lista reactiva).

## Cómo abrir el proyecto

1. Abre Android Studio (Koala o más reciente) → **Open** → selecciona la carpeta `pos-app/`.
2. Deja que Gradle sincronice (puede tardar la primera vez, descarga dependencias).
3. Ejecuta en un emulador o dispositivo físico con **Android 8.0 (API 26)** o superior.
4. Verás la pantalla de inventario de prueba: toca "Agregar producto de prueba"
   varias veces y confirma que la lista crece — eso valida que Room está funcionando.

## Dónde van tus credenciales (IMPORTANTE — no se suben a git)

Crea (o edita) el archivo `local.properties` en la raíz del proyecto y agrega:

```properties
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-clave-anonima-publica
```

Estas se leen en `app/build.gradle.kts` vía `buildConfigField` y quedan disponibles
en código Kotlin como `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_ANON_KEY`.
Este archivo ya está en `.gitignore`, así que nunca se sube al repositorio.

**Aún no necesitas crear el proyecto en Supabase** — eso lo hacemos en el paso 6
(sincronización). Por ahora el POS funciona 100% offline con Room.

## Siguiente paso

Con esta base ya puedes construir:
- **Paso 2**: pantallas Compose reales de alta/edición de productos y categorías
  (reemplazando la demo en `MainActivity`).
- **Paso 3**: el carrito de compras y la pantalla de checkout, que ya puede usar
  `VentaDao.registrarVentaCompleta()` tal cual está.

Dime cuándo quieras seguir con el Paso 2 (CRUD de inventario completo con UI) o el
Paso 3 (carrito y checkout) y seguimos armando archivos reales sobre esta base.
