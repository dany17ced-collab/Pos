package com.tuempresa.possystem.domain

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hashea PINs numéricos con PBKDF2 + sal aleatoria por usuario. Aunque el PIN
 * es corto (4-6 dígitos), usar hash+sal evita que la base de datos local
 * exponga los PINs en texto plano si el dispositivo se pierde o es inspeccionado.
 */
object PinHasher {
    private const val ITERACIONES = 120_000
    private const val LONGITUD_CLAVE = 256 // bits
    private const val ALGORITMO = "PBKDF2WithHmacSHA256"

    /** Genera una sal aleatoria nueva, codificada en Base64, para un usuario nuevo. */
    fun generarSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    /** Hashea un PIN dado con la sal proporcionada. Devuelve el hash en Base64. */
    fun hashear(pin: String, salBase64: String): String {
        val sal = android.util.Base64.decode(salBase64, android.util.Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), sal, ITERACIONES, LONGITUD_CLAVE)
        val factory = SecretKeyFactory.getInstance(ALGORITMO)
        val hash = factory.generateSecret(spec).encoded
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    /** Verifica si un PIN ingresado coincide con el hash guardado. */
    fun verificar(pinIngresado: String, salBase64: String, hashGuardado: String): Boolean {
        val hashCalculado = hashear(pinIngresado, salBase64)
        // Comparación en tiempo constante para evitar timing attacks
        return hashCalculado.length == hashGuardado.length &&
            hashCalculado.zip(hashGuardado).fold(true) { acc, (a, b) -> acc and (a == b) }
    }
}
