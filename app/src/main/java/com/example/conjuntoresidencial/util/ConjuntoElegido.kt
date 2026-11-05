package com.example.conjuntoresidencial.util

import android.content.Context
import androidx.core.content.edit
import com.example.conjuntoresidencial.model.ConjuntoDTO

/**
 * P7: el conjunto al que entra esta app. Va en su propio archivo y no en la sesión: cerrar sesión
 * (o que caduque) no lo borra, así no hay que volver a elegirlo cada vez. AuthInterceptor lo manda
 * en la cabecera X-Conjunto; el servidor la usa antes de iniciar sesión (después manda el token).
 */
object ConjuntoElegido {

    data class Conjunto(val id: Long, val nombre: String, val departamento: String, val municipio: String)

    private const val ARCHIVO = "residencial_conjunto"

    private fun prefs(context: Context) = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun actual(context: Context): Conjunto? {
        val p = prefs(context)
        val id = p.getLong("id", -1L).takeIf { it > 0 } ?: return null
        return Conjunto(id, p.getString("nombre", "").orEmpty(), p.getString("departamento", "").orEmpty(),
            p.getString("municipio", "").orEmpty())
    }

    fun id(context: Context): Long? = prefs(context).getLong("id", -1L).takeIf { it > 0 }

    fun guardar(context: Context, c: ConjuntoDTO) = prefs(context).edit {
        // P9 bloque 2: otro conjunto, otro código
        if (prefs(context).getLong("id", -1L) != c.id) { remove("pase"); remove("pase_conjunto") }
        putLong("id", c.id)
        putString("nombre", c.nombre)
        putString("departamento", c.departamento)
        putString("municipio", c.municipio)
    }

    /** P9 bloque 2: el pase que dio el código del conjunto; null si es de otro conjunto o no hay. */
    fun pase(context: Context): String? {
        val p = prefs(context)
        val de = p.getLong("pase_conjunto", -1L)
        return p.getString("pase", null)?.takeIf { de > 0 && de == id(context) }
    }

    fun guardarPase(context: Context, conjuntoId: Long, pase: String) = prefs(context).edit {
        putString("pase", pase)
        putLong("pase_conjunto", conjuntoId)
    }

    /** El servidor ya no lo acepta (cambió el secreto del conjunto): se vuelve a pedir el código. */
    fun borrarPase(context: Context) = prefs(context).edit { remove("pase"); remove("pase_conjunto") }

    /**
     * El login trae el conjunto de la cuenta. Si no se había elegido ninguno (la app anterior, o un
     * servidor de un solo conjunto) se guarda ese; si es el mismo, se actualiza el nombre por si cambió.
     */
    fun alIniciarSesion(context: Context, id: Long?, nombre: String?) {
        if (id == null || nombre.isNullOrBlank()) return
        val elegido = id(context)
        if (elegido != null && elegido != id) return
        if (elegido == null) prefs(context).edit { putLong("id", id) }
        prefs(context).edit { putString("nombre", nombre) }
    }
}
