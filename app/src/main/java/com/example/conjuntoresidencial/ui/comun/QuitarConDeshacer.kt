package com.example.conjuntoresidencial.ui.comun

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * P11: lo que se quitó deslizando queda escondido mientras se ve "Deshacer"; si el aviso
 * desaparece sin usarse, se quita en el servidor. Si falla, vuelve a aparecer con el error.
 */
class QuitarConDeshacer(
    private val fragment: Fragment,
    private val repintar: () -> Unit,
    private val llamada: suspend (Long) -> Response<Unit>
) {
    private val escondidas = mutableSetOf<Long>()

    fun escondida(id: Long) = id in escondidas

    fun quitar(id: Long) {
        escondidas += id
        repintar()
        fragment.aviso(fragment.getString(R.string.corr_quitada), Snackbar.LENGTH_LONG)
            .setAction(R.string.deshacer) { escondidas -= id; repintar() }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(barra: Snackbar?, evento: Int) {
                    if (evento != DISMISS_EVENT_ACTION) confirmar(id)
                }
            })
            .show()
    }

    private fun confirmar(id: Long) {
        // El fragment puede haberse cerrado: igual se quita en el servidor
        val alcance = fragment.view?.let { fragment.viewLifecycleOwner.lifecycleScope } ?: fragment.lifecycleScope
        alcance.launch {
            val error = try {
                val r = llamada(id)
                if (r.isSuccessful) null else RetrofitClient.mensajeDeError(r, fragment.getString(R.string.corr_error_quitar))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            if (error != null) {
                escondidas -= id
                if (fragment.view != null) { repintar(); fragment.aviso(error).show() }
            }
        }
    }
}
