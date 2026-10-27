package com.example.conjuntoresidencial.ui.feed

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentPublicarBinding
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Nueva publicación (administración, P5): texto y hasta 6 imágenes o archivos (PDF, Word,
 * Excel) de 10 MB. A todos los residentes les llega el aviso.
 */
class PublicarFragment : Fragment() {
    private var _binding: FragmentPublicarBinding? = null
    private val binding get() = _binding!!

    private data class Adjunto(val uri: Uri, val nombre: String, val tamano: Long)
    private val adjuntos = mutableListOf<Adjunto>()

    private val elegir = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            if (adjuntos.size >= MAXIMO) {
                aviso(getString(R.string.publicar_maximo, MAXIMO)).show()
                return@forEach
            }
            val (nombre, tamano) = datosDe(uri)
            if (tamano > TAMANO_MAXIMO) aviso(getString(R.string.publicar_muy_grande, nombre)).show()
            else if (adjuntos.none { it.uri == uri }) adjuntos += Adjunto(uri, nombre, tamano)
        }
        pintarAdjuntos()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPublicarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.publicar_titulo, R.string.publicar_descripcion)
        binding.btnAdjuntar.setOnClickListener { elegir.launch(TIPOS) }
        binding.btnPublicar.setOnClickListener { publicar() }
        pintarAdjuntos()
    }

    private fun pintarAdjuntos() {
        binding.listaAdjuntos.removeAllViews()
        adjuntos.forEach { a ->
            val fila = layoutInflater.inflate(android.R.layout.simple_list_item_2, binding.listaAdjuntos, false)
            fila.findViewById<TextView>(android.R.id.text1).text = a.nombre
            fila.findViewById<TextView>(android.R.id.text2).text = getString(R.string.publicar_quitar_adjunto,
                android.text.format.Formatter.formatShortFileSize(requireContext(), a.tamano))
            fila.setOnClickListener { adjuntos.remove(a); pintarAdjuntos() }
            binding.listaAdjuntos.addView(fila)
        }
        binding.btnAdjuntar.isEnabled = adjuntos.size < MAXIMO
    }

    private fun publicar() {
        val texto = binding.etTextoPublicacion.text?.toString()?.trim().orEmpty()
        if (texto.isEmpty() && adjuntos.isEmpty()) {
            binding.tilTextoPublicacion.error = getString(R.string.publicar_vacia)
            return
        }
        binding.tilTextoPublicacion.error = null
        cargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Leer los archivos fuera del hilo principal
                val partes = withContext(Dispatchers.IO) {
                    adjuntos.map { a ->
                        val datos = requireContext().contentResolver.openInputStream(a.uri)!!.use { it.readBytes() }
                        val tipo = requireContext().contentResolver.getType(a.uri) ?: "application/octet-stream"
                        MultipartBody.Part.createFormData("archivos", a.nombre, datos.toRequestBody(tipo.toMediaTypeOrNull()))
                    }
                }
                val r = RetrofitClient.instance.publicar(
                    texto.takeIf { it.isNotEmpty() }?.let { it.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull()) },
                    partes
                )
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    // Inicio recarga el feed al volver
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(PUBLICADO, true)
                    findNavController().popBackStack()
                } else {
                    cargando(false)
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.publicar_error))).show()
                }
            } catch (e: Exception) {
                if (_binding == null) return@launch
                cargando(false)
                aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun cargando(si: Boolean) {
        binding.progresoPublicar.visibility = if (si) View.VISIBLE else View.GONE
        binding.btnPublicar.isEnabled = !si
        binding.btnAdjuntar.isEnabled = !si && adjuntos.size < MAXIMO
    }

    /** Nombre y tamaño del archivo elegido. */
    private fun datosDe(uri: Uri): Pair<String, Long> {
        requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) return (c.getString(0) ?: "archivo") to (if (c.isNull(1)) 0L else c.getLong(1))
        }
        return "archivo" to 0L
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val PUBLICADO = "publicado"
        private const val MAXIMO = 6
        private const val TAMANO_MAXIMO = 10L * 1024 * 1024
        private val TIPOS = arrayOf(
            "image/jpeg", "image/png", "image/webp", "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    }
}
