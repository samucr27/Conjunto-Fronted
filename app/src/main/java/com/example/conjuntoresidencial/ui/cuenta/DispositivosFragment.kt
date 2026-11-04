package com.example.conjuntoresidencial.ui.cuenta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentDispositivosBinding
import com.example.conjuntoresidencial.model.DispositivoCuentaDTO
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.example.conjuntoresidencial.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * P7 bloque 5: Cuenta → Mis dispositivos. Los celulares donde entró la cuenta, con su sesión.
 * Quitar uno cierra su sesión y, si vuelve a entrar, el servidor pide el código del correo.
 * Este celular no se quita aquí: para eso está "Cerrar sesión".
 */
class DispositivosFragment : Fragment() {

    private var _binding: FragmentDispositivosBinding? = null
    private val binding get() = _binding!!
    private val adaptador = DispositivoAdapter { opciones(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDispositivosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.dispositivos_titulo, R.string.dispositivos_descripcion,
            menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe) { cargar() })
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador
        binding.estado.vincular(binding.lista)
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.comun.dispositivos()
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.dispositivos_error_cargar)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            binding.estado.mostrarRecurso(resource, binding.swipe, R.drawable.ic_smartphone, getString(R.string.dispositivos_vacio),
                getString(R.string.dispositivos_vacio_mensaje), reintentar = { cargar() }) { lista ->
                adaptador.submitList(lista)
                val abiertas = lista.count { it.sesionAbierta }
                binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.resumen_dispositivos, abiertas, abiertas))
            }
        }
    }

    private fun opciones(d: DispositivoCuentaDTO) {
        if (d.actual) {
            aviso(R.string.dispositivos_este_celular_ayuda).show()
            return
        }
        val nombre = d.nombre ?: getString(R.string.dispositivos_sin_nombre)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dispositivos_quitar_titulo, nombre))
            .setMessage(R.string.dispositivos_quitar_mensaje)
            .setPositiveButton(R.string.dispositivos_quitar) { _, _ ->
                ejecutarAccion(getString(R.string.dispositivos_quitado, nombre), {
                    RetrofitClient.comun.quitarDispositivo(d.id)
                }) { cargar() }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DispositivoAdapter(private val onTocar: (DispositivoCuentaDTO) -> Unit) :
        ListAdapter<DispositivoCuentaDTO, DispositivoAdapter.VH>(DiffPorId { it.id }) {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nombre: TextView = v.findViewById(android.R.id.text1)
            val detalle: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val d = getItem(position)
            val contexto = h.itemView.context
            val nombre = d.nombre ?: contexto.getString(R.string.dispositivos_sin_nombre)
            h.nombre.text = if (d.actual) contexto.getString(R.string.dispositivos_este_celular, nombre) else nombre
            h.detalle.text = if (d.sesionAbierta) {
                contexto.getString(R.string.dispositivos_sesion_desde, fecha(d.sesionDesde))
            } else {
                contexto.getString(R.string.dispositivos_sin_sesion)
            }
            h.itemView.alpha = if (d.sesionAbierta) 1f else 0.6f
            h.itemView.setOnClickListener { onTocar(d) }
        }

        private fun fecha(iso: String?) = Formatos.fechaHora(iso?.take(10), iso?.substringAfter('T', "")?.take(5))
    }
}
