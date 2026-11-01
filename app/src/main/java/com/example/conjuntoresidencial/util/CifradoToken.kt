package com.example.conjuntoresidencial.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra el token de sesión con una llave del Android Keystore (AES-GCM) que no sale del celular.
 * Lo guardado empieza con [PREFIJO]; lo anterior (texto plano) se sigue leyendo y se cifra al
 * guardar la siguiente sesión.
 */
object CifradoToken {
    private const val ALIAS = "miconjunto_token"
    private const val PREFIJO = "v1:"
    private const val TAMANO_IV = 12

    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    /** El texto cifrado, o el mismo texto si el Keystore no responde (mejor entrar que quedar fuera). */
    fun cifrar(texto: String): String = try {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, llave()) }
        val datos = cifrador.iv + cifrador.doFinal(texto.toByteArray(Charsets.UTF_8))
        PREFIJO + Base64.encodeToString(datos, Base64.NO_WRAP)
    } catch (e: Exception) {
        texto
    }

    /** El texto original; null si estaba cifrado y ya no se puede abrir (la sesión se da por perdida). */
    fun descifrar(guardado: String): String? {
        if (!guardado.startsWith(PREFIJO)) return guardado
        return try {
            val datos = Base64.decode(guardado.removePrefix(PREFIJO), Base64.NO_WRAP)
            val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
                .apply { init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, datos, 0, TAMANO_IV)) }
            String(cifrador.doFinal(datos, TAMANO_IV, datos.size - TAMANO_IV), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
