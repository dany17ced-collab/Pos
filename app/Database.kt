// En tu clase AppDatabase.kt, agrega:

@Database(
    entities = [
        // ... tus otras entidades
        UsuarioEntity::class
    ],
    version = 2  // Incrementa la versión
)
abstract class AppDatabase : RoomDatabase() {
    // ... tus otros DAOs
    abstract fun usuarioDao(): UsuarioDao
}
