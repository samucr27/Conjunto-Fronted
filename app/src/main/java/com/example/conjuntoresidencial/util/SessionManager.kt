package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.core.content.edit
import com.example.conjuntoresidencial.model.LoginResponse

class SessionManager(context: Context) {
    // Este archivo está excluido de las copias de seguridad (ver res/xml/backup_rules.xml)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit { putString("token", token) }
    fun getToken(): String? = prefs.getString("token", null)

    fun saveApartamentoId(id: Long) = prefs.edit { putLong("apartamento_id", id) }
    fun getApartamentoId(): Long = prefs.getLong("apartamento_id", -1L)

    fun saveTorre(torre: String) = prefs.edit { putString("torre", torre) }
    fun getTorre(): String = prefs.getString("torre", "") ?: ""

    fun saveApto(apto: String) = prefs.edit { putString("apto", apto) }
    fun getApto(): String = prefs.getString("apto", "") ?: ""

    fun getTorreApto(): String {
        val torre = getTorre()
        val apto = getApto()
        return if (torre.isNotEmpty() && apto.isNotEmpty()) "Torre $torre Apto $apto" else ""
    }

    fun saveRol(rol: String) = prefs.edit { putString("rol", rol) }
    fun getRol(): String? = prefs.getString("rol", null)
    // De la administración: el administrador o el contador (P4), que entra por el mismo login
    fun esAdmin(): Boolean = getRol() == "ADMIN" || esContador()
    // El contador solo maneja la cartera y la facturación
    fun esContador(): Boolean = getRol() == "CONTADOR"

    // P5: el administrador principal maneja el equipo; los asistentes no
    fun esPrincipal(): Boolean = getRol() == "ADMIN" && prefs.getBoolean("principal", false)
    // El arrendatario usa la app como residente, pero no gestiona quién vive en el apartamento
    fun esPropietario(): Boolean = getRol() == "PROPIETARIO"

    // Si entró con su documento (P4): quién es y con qué documento (para la huella)
    fun getResidenteId(): Long? = prefs.getLong("residente_id", -1L).takeIf { it > 0 }
    fun getNombre(): String = prefs.getString("nombre", "") ?: ""
    fun getDocumento(): String? = prefs.getString("documento", null)

    // Usuario del administrador (el residente se identifica con torre y apartamento)
    fun saveUsuario(usuario: String) = prefs.edit { putString("usuario", usuario) }
    fun getUsuario(): String = prefs.getString("usuario", "") ?: ""

    /** Guarda lo que devuelve el login (con contraseña o con huella); [documento] si entró con él. */
    fun guardar(respuesta: LoginResponse, documento: String? = null) {
        saveToken(respuesta.token)
        saveRol(respuesta.rol)
        if (respuesta.rol == "ADMIN" || respuesta.rol == "CONTADOR") {
            respuesta.usuario?.let(::saveUsuario)
            prefs.edit { putBoolean("principal", respuesta.principal == true) }
        } else {
            saveApartamentoId(respuesta.id)
            saveTorre(respuesta.torre ?: "")
            saveApto(respuesta.apto ?: "")
        }
        prefs.edit {
            if (respuesta.residenteId != null) {
                putLong("residente_id", respuesta.residenteId)
                putString("nombre", respuesta.nombre.orEmpty())
            } else {
                remove("residente_id"); remove("nombre")
            }
            if (documento != null) putString("documento", documento) else remove("documento")
        }
    }

    fun clearSession() = prefs.edit { clear() }

    companion object {
        const val PREFS_NAME = "residencial_session"
    }
}
