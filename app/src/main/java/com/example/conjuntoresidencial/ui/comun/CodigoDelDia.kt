package com.example.conjuntoresidencial.ui.comun

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ItemCodigoConjuntoBinding
import kotlinx.coroutines.launch

/** P10: tiempo que el código queda a la vista antes de ocultarse otra vez. */
private const val VISIBLE_MS = 30_000L

/**
 * P9 bloque 2: carga y muestra el código del conjunto de hoy (administración y portería). Si el
 * servidor aún no lo tiene (versión anterior) o falla la red, la tarjeta no aparece.
 */
fun Fragment.mostrarCodigoDelDia(tarjeta: ItemCodigoConjuntoBinding) {
    viewLifecycleOwner.lifecycleScope.launch {
        val hoy = try {
            RetrofitClient.comun.codigoDelDia().takeIf { it.isSuccessful }?.body()
        } catch (_: Exception) {
            null
        }
        if (view == null) return@launch
        tarjeta.root.isVisible = hoy != null
        if (hoy == null) return@launch
        tarjeta.ponerCodigo(hoy.codigo)
    }
}

/**
 * P10: pone el código oculto ("••• •••"); el botón del ojo lo muestra 30 segundos y lo vuelve a
 * ocultar, para que no quede a la vista de quien pase frente a la portería o la pantalla.
 */
fun ItemCodigoConjuntoBinding.ponerCodigo(codigo: String) {
    val contexto = root.context
    tvCambia.setText(R.string.codigo_hoy_cambia)
    val ocultar = Runnable { verCodigo(codigo, false) }
    verCodigo(codigo, false)
    btnVerCodigo.setOnClickListener {
        val mostrar = tvCodigo.tag != true
        verCodigo(codigo, mostrar)
        root.removeCallbacks(ocultar)
        if (mostrar) root.postDelayed(ocultar, VISIBLE_MS)
    }
    root.contentDescription = contexto.getString(R.string.codigo_hoy_titulo)
}

private fun ItemCodigoConjuntoBinding.verCodigo(codigo: String, visible: Boolean) {
    val contexto = root.context
    tvCodigo.tag = visible
    tvCodigo.text = if (visible) separarCodigo(codigo) else "••• •••"
    tvCodigo.contentDescription = if (visible) contexto.getString(R.string.codigo_hoy_accesible, codigo.toCharArray().joinToString(" "))
        else contexto.getString(R.string.codigo_oculto)
    btnVerCodigo.setIconResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
    btnVerCodigo.contentDescription = contexto.getString(if (visible) R.string.codigo_ocultar else R.string.codigo_mostrar)
}

/** "482913" → "482 913", más fácil de dictar. */
fun separarCodigo(codigo: String): String =
    if (codigo.length == 6) codigo.substring(0, 3) + " " + codigo.substring(3) else codigo
