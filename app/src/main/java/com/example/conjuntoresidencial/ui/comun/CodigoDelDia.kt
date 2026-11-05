package com.example.conjuntoresidencial.ui.comun

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ItemCodigoConjuntoBinding
import kotlinx.coroutines.launch

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
        tarjeta.tvCodigo.text = separarCodigo(hoy.codigo)
        tarjeta.tvCambia.setText(R.string.codigo_hoy_cambia)
        tarjeta.root.contentDescription = getString(R.string.codigo_hoy_accesible, hoy.codigo.toCharArray().joinToString(" "))
    }
}

/** "482913" → "482 913", más fácil de dictar. */
fun separarCodigo(codigo: String): String =
    if (codigo.length == 6) codigo.substring(0, 3) + " " + codigo.substring(3) else codigo
