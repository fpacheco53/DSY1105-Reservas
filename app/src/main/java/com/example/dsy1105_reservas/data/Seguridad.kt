package com.example.dsy1105_reservas.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de contraseñas con PBKDF2 (HMAC-SHA256) y sal aleatoria por usuario.
 */
object Seguridad {
    private const val ITERACIONES = 60_000
    private const val LARGO_CLAVE_BITS = 256

    fun nuevaSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(clave: String, sal: String): String {
        val spec = PBEKeySpec(
            clave.toCharArray(),
            Base64.getDecoder().decode(sal),
            ITERACIONES,
            LARGO_CLAVE_BITS
        )
        val fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return Base64.getEncoder().encodeToString(fabrica.generateSecret(spec).encoded)
    }

    fun verificar(clave: String, sal: String, hashEsperado: String): Boolean =
        MessageDigest.isEqual(
            hash(clave, sal).toByteArray(),
            hashEsperado.toByteArray()
        )
}
