package com.example.conjuntoresidencial.ui.plataforma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentEventoPlataformaBinding
import com.example.conjuntoresidencial.databinding.ItemTextoConAccionBinding
import com.example.conjuntoresidencial.model.EventoPlataforma
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo

/**
 * P10: una operación de la auditoría con todo su detalle: qué se hizo, quién, cuándo, en qué
 * conjunto (con botón para abrirlo, solo si el evento es de uno) y el texto completo que guardó el
 * servidor. Los eventos sin conjunto (inicio de sesión) ya no intentan abrir un conjunto que no existe.
 */
class EventoPlataformaFragment : Fragment() {

    private var _binding: FragmentEventoPlataformaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventoPlataformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val a = requireArguments()
        val accion = a.getString(ARG_ACCION).orEmpty()
        val conjuntoId = a.getLong(ARG_CONJUNTO_ID).takeIf { it > 0 }
        val conjunto = a.getString(ARG_CONJUNTO)
        val fecha = fechaEvento(a.getString(ARG_FECHA))
        configurarEncabezado(binding.encabezado, getString(nombreAccion(accion)), R.string.evento_descripcion)
        binding.encabezado.ponerInfo(fecha)

        fila(R.drawable.ic_description, R.string.evento_accion, getString(nombreAccion(accion)), accion)
        fila(R.drawable.ic_person, R.string.evento_operador, a.getString(ARG_OPERADOR).orEmpty().ifBlank { "—" })
        fila(R.drawable.ic_event, R.string.evento_fecha, fecha.ifBlank { "—" })
        val filaConjunto = fila(R.drawable.ic_apartment, R.string.evento_conjunto,
            when {
                conjuntoId == null -> getString(R.string.evento_sin_conjunto)
                conjunto.isNullOrBlank() -> getString(R.string.evento_conjunto_borrado, conjuntoId)
                else -> conjunto
            })
        if (conjuntoId != null && !conjunto.isNullOrBlank()) {
            filaConjunto.btnAccion.isVisible = true
            filaConjunto.btnAccion.setText(R.string.panel_abrir)
            filaConjunto.btnAccion.setOnClickListener {
                findNavController().navigate(R.id.conjuntoDetalleFragment, bundleOf(ConjuntoDetalleFragment.ARG_CONJUNTO to conjuntoId))
            }
        }
        fila(R.drawable.ic_info, R.string.evento_detalle, a.getString(ARG_DETALLE).orEmpty().ifBlank { getString(R.string.evento_sin_detalle) })
        fila(R.drawable.ic_key, R.string.evento_registro, "#" + a.getLong(ARG_ID))
    }

    private fun fila(@DrawableRes icono: Int, @StringRes nombre: Int, valor: String, extra: String? = null): ItemTextoConAccionBinding {
        val f = ItemTextoConAccionBinding.inflate(layoutInflater, binding.lista, false)
        f.marcoIcono.isVisible = true
        f.imgIcono.setImageResource(icono)
        f.tvTitulo.setText(nombre)
        f.tvTitulo.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
        f.tvDetalle.text = if (extra.isNullOrBlank()) valor else "$valor ($extra)"
        f.tvDetalle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        f.tvDetalle.setTextIsSelectable(true)
        binding.lista.addView(f.root)
        return f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_OPERADOR = "operador"
        const val ARG_ACCION = "accion"
        const val ARG_CONJUNTO_ID = "conjuntoId"
        const val ARG_CONJUNTO = "conjunto"
        const val ARG_DETALLE = "detalle"
        const val ARG_FECHA = "fecha"

        fun argumentos(e: EventoPlataforma) = bundleOf(
            ARG_ID to e.id, ARG_OPERADOR to e.operador, ARG_ACCION to e.accion, ARG_CONJUNTO_ID to (e.conjuntoId ?: 0L),
            ARG_CONJUNTO to e.conjunto, ARG_DETALLE to e.detalle, ARG_FECHA to e.fecha
        )
    }
}
