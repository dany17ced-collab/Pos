// En tu clase POSApplication.kt:

class POSApplication : Application() {
    // ... tu código existente
    
    val usuarioRepository: UsuarioRepository by lazy {
        UsuarioRepository(database.usuarioDao())
    }
}
