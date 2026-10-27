package com.example.conjuntoresidencial.ui.obras

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.ui.comun.aviso
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Certificado ARL elegido en el teléfono (foto o PDF), listo para enviarlo al backend. */
class CertificadoArl(val datos: ByteArray, val tipo: String) {

    fun parte(): MultipartBody.Part {
        val nombre = when (tipo) {
            "application/pdf" -> "arl.pdf"
            "image/png" -> "arl.png"
            else -> "arl.jpg"
        }
        return MultipartBody.Part.createFormData("archivo", nombre, datos.toRequestBody(tipo.toMediaTypeOrNull()))
    }

    companion object {
        // El backend acepta hasta 5 MB
        const val TAMANO_MAXIMO = 5 * 1024 * 1024
        val TIPOS = arrayOf("image/*", "application/pdf")
    }
}

/** Lee el archivo elegido; si no se puede leer o pesa más de 5 MB, avisa y devuelve null. */
fun Fragment.leerCertificado(uri: Uri): CertificadoArl? {
    val resolver = requireContext().contentResolver
    val datos = try {
        resolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
    when {
        datos == null -> aviso(R.string.arl_error_leer).show()
        datos.size > CertificadoArl.TAMANO_MAXIMO -> aviso(R.string.arl_archivo_grande).show()
        else -> return CertificadoArl(datos, resolver.getType(uri) ?: "application/octet-stream")
    }
    return null
}

/** Pide el enlace firmado (10 minutos) y abre el certificado en el navegador o el visor de PDF. */
fun Fragment.abrirCertificado(personalId: Long) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val r = RetrofitClient.instance.getCertificadoArl(personalId)
            val enlace = r.body()
            if (!r.isSuccessful || enlace == null) {
                aviso(RetrofitClient.mensajeDeError(r, getString(R.string.arl_error_ver))).show()
                return@launch
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace.url)))
        } catch (_: ActivityNotFoundException) {
            aviso(R.string.cuota_sin_visor).show()
        } catch (e: Exception) {
            aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
        }
    }
}
