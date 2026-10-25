package com.example.conjuntoresidencial.seguridad

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.conjuntoresidencial.R
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Entrar con huella o rostro. Al activarlo se guardan los datos de acceso de la cuenta
 * (usuario o torre/apto y contraseña) cifrados con una llave del Android Keystore que nunca
 * sale del celular; al entrar, la huella los descifra y la app inicia sesión con ellos.
 *
 * - Biometría fuerte (huella, rostro de clase 3): la llave solo funciona después de
 *   autenticarse (CryptoObject) y se invalida si se registra otra huella.
 * - Biometría débil (el rostro de muchos celulares): Android no permite atar la llave a ella;
 *   los datos siguen cifrados en el Keystore y el rostro solo autoriza su uso.
 *
 * Se guarda una sola cuenta; activar otra la reemplaza. Va aparte de la sesión
 * (SessionManager.clearSession no la borra) para poder entrar después de cerrar la app.
 */
object AccesoBiometrico {

    enum class Modo { FUERTE, DEBIL }

    /** Para Cuenta → Permisos: si el celular tiene biometría y si hay alguna registrada. */
    enum class Estado { CONFIGURADA, SIN_REGISTRAR, NO_DISPONIBLE }

    /**
     * Lo que hace falta para iniciar sesión: usuario (admin), documento (residente con cuenta
     * propia, P4) o torre y apto (residente con la contraseña del apartamento).
     */
    data class Credenciales(
        val rol: String,
        val usuario: String? = null,
        val torre: String? = null,
        val apto: String? = null,
        val contrasena: String,
        val documento: String? = null
    ) {
        val esAdmin get() = rol == ROL_ADMIN

        /** "Administrador · admin", "Documento 1010" o "Torre 1 · Apto 101": a quién entra la huella. */
        fun cuenta(context: Context): String = when {
            esAdmin -> context.getString(R.string.biometria_cuenta_admin, usuario)
            documento != null -> context.getString(R.string.biometria_cuenta_documento, documento)
            else -> context.getString(R.string.biometria_cuenta_residente, torre, apto)
        }
    }

    private const val ROL_ADMIN = "ADMIN"
    private const val PREFS = "ajustes"
    private const val CLAVE_DATOS = "biometria_datos"
    private const val CLAVE_IV = "biometria_iv"
    private const val CLAVE_MODO = "biometria_modo"
    private const val CLAVE_CUENTA = "biometria_cuenta"
    private const val CLAVE_NO_PREGUNTAR = "biometria_no_preguntar"
    private const val ALIAS_FUERTE = "miconjunto_biometria"
    private const val ALIAS_DEBIL = "miconjunto_biometria_debil"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"
    private val gson = Gson()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** La biometría que se puede usar ahora (null si no hay ninguna registrada). */
    fun modoDisponible(context: Context): Modo? {
        val gestor = BiometricManager.from(context)
        return when {
            gestor.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> Modo.FUERTE
            gestor.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> Modo.DEBIL
            else -> null
        }
    }

    fun estado(context: Context): Estado {
        if (modoDisponible(context) != null) return Estado.CONFIGURADA
        val gestor = BiometricManager.from(context)
        val sinRegistrar = listOf(BIOMETRIC_STRONG, BIOMETRIC_WEAK).any {
            gestor.canAuthenticate(it) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        }
        return if (sinRegistrar) Estado.SIN_REGISTRAR else Estado.NO_DISPONIBLE
    }

    fun activado(context: Context): Boolean = prefs(context).contains(CLAVE_DATOS)

    /** "Torre 1 · Apto 101": la cuenta que abre la huella, o null si no está activada. */
    fun cuentaGuardada(context: Context): String? =
        if (activado(context)) prefs(context).getString(CLAVE_CUENTA, null) else null

    /** Después de entrar con contraseña: ¿se ofrece activar la huella para esta cuenta? */
    fun conviene(context: Context, cuenta: String): Boolean =
        modoDisponible(context) != null && cuentaGuardada(context) != cuenta &&
            cuenta !in prefs(context).getStringSet(CLAVE_NO_PREGUNTAR, emptySet()).orEmpty()

    fun noVolverAPreguntar(context: Context, cuenta: String) {
        val actuales = prefs(context).getStringSet(CLAVE_NO_PREGUNTAR, emptySet()).orEmpty()
        prefs(context).edit { putStringSet(CLAVE_NO_PREGUNTAR, actuales + cuenta) }
    }

    /**
     * Pide la huella y guarda las credenciales cifradas. [alTerminar] recibe true si quedó
     * activado; false si el usuario canceló o falló (no se cambia lo que hubiera).
     */
    fun activar(fragment: Fragment, credenciales: Credenciales, alTerminar: (Boolean) -> Unit) {
        val context = fragment.requireContext()
        val modo = modoDisponible(context) ?: return alTerminar(false)
        val cifrador = try {
            Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, llave(modo)) }
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Se registró otra huella desde la última vez: se crea una llave nueva
            borrarLlave(modo)
            Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, llave(modo)) }
        }
        pedir(
            fragment, modo, cifrador,
            titulo = context.getString(R.string.biometria_activar_titulo),
            subtitulo = credenciales.cuenta(context),
            botonNegativo = context.getString(R.string.btn_cancelar),
            alExito = { c ->
                val datos = c.doFinal(gson.toJson(credenciales).toByteArray(Charsets.UTF_8))
                prefs(context).edit {
                    putString(CLAVE_DATOS, Base64.encodeToString(datos, Base64.NO_WRAP))
                    putString(CLAVE_IV, Base64.encodeToString(c.iv, Base64.NO_WRAP))
                    putString(CLAVE_MODO, modo.name)
                    putString(CLAVE_CUENTA, credenciales.cuenta(context))
                }
                // La otra llave (si había) ya no sirve
                borrarLlave(if (modo == Modo.FUERTE) Modo.DEBIL else Modo.FUERTE)
                alTerminar(true)
            },
            alCancelar = { alTerminar(false) }
        )
    }

    /**
     * Pide la huella y entrega las credenciales guardadas. [alFallar] recibe un mensaje para
     * mostrar, o null si el usuario solo canceló (eligió usar la contraseña).
     */
    fun entrar(fragment: Fragment, alObtener: (Credenciales) -> Unit, alFallar: (String?) -> Unit) {
        val context = fragment.requireContext()
        val p = prefs(context)
        val datos = p.getString(CLAVE_DATOS, null)
        val iv = p.getString(CLAVE_IV, null)
        val modo = p.getString(CLAVE_MODO, null)?.let { runCatching { Modo.valueOf(it) }.getOrNull() }
        if (datos == null || iv == null || modo == null) return alFallar(null)
        val cifrador = try {
            Cipher.getInstance(TRANSFORMACION).apply {
                init(Cipher.DECRYPT_MODE, llave(modo), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            desactivar(context)
            return alFallar(context.getString(R.string.biometria_invalidada))
        } catch (_: Exception) {
            desactivar(context)
            return alFallar(context.getString(R.string.biometria_error_datos))
        }
        pedir(
            fragment, modo, cifrador,
            titulo = context.getString(R.string.biometria_entrar_titulo),
            subtitulo = p.getString(CLAVE_CUENTA, null),
            botonNegativo = context.getString(R.string.biometria_usar_contrasena),
            alExito = { c ->
                val credenciales = runCatching {
                    gson.fromJson(String(c.doFinal(Base64.decode(datos, Base64.NO_WRAP)), Charsets.UTF_8), Credenciales::class.java)
                }.getOrNull()
                if (credenciales != null) alObtener(credenciales)
                else {
                    desactivar(context)
                    alFallar(context.getString(R.string.biometria_error_datos))
                }
            },
            alCancelar = alFallar
        )
    }

    /** Borra los datos guardados y las llaves: hará falta la contraseña otra vez. */
    fun desactivar(context: Context) {
        prefs(context).edit {
            remove(CLAVE_DATOS); remove(CLAVE_IV); remove(CLAVE_MODO); remove(CLAVE_CUENTA)
        }
        Modo.entries.forEach(::borrarLlave)
    }

    private fun pedir(
        fragment: Fragment, modo: Modo, cifrador: Cipher,
        titulo: String, subtitulo: String?, botonNegativo: String,
        alExito: (Cipher) -> Unit, alCancelar: (String?) -> Unit
    ) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setNegativeButtonText(botonNegativo)
            .setAllowedAuthenticators(if (modo == Modo.FUERTE) BIOMETRIC_STRONG else BIOMETRIC_WEAK)
            .build()
        val prompt = BiometricPrompt(fragment, ContextCompat.getMainExecutor(fragment.requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    alExito(resultado.cryptoObject?.cipher ?: cifrador)
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    val soloCancelo = codigo == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        codigo == BiometricPrompt.ERROR_USER_CANCELED || codigo == BiometricPrompt.ERROR_CANCELED
                    alCancelar(if (soloCancelo) null else mensaje.toString())
                }
            })
        if (modo == Modo.FUERTE) prompt.authenticate(info, BiometricPrompt.CryptoObject(cifrador))
        else prompt.authenticate(info)
    }

    private fun llave(modo: Modo): SecretKey {
        val alias = if (modo == Modo.FUERTE) ALIAS_FUERTE else ALIAS_DEBIL
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(alias, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .apply {
                if (modo == Modo.FUERTE) {
                    setUserAuthenticationRequired(true)
                    setInvalidatedByBiometricEnrollment(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    }
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    private fun borrarLlave(modo: Modo) {
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(if (modo == Modo.FUERTE) ALIAS_FUERTE else ALIAS_DEBIL)
        }
    }
}
