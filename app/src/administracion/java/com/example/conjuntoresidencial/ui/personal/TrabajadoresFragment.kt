package com.example.conjuntoresidencial.ui.personal

import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.EmpresaDTO
import com.example.conjuntoresidencial.model.TrabajadorDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.FiltroTorreApto
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * P11 bloque 8: los trabajadores del conjunto. Se buscan por nombre o documento y se filtran por
 * cargo y si están activos; tocar uno abre su ficha (datos, turnos y nómina).
 */
class TrabajadoresFragment : PantallaHoja() {
    override val titulo = R.string.pers_trabajadores
    override val descripcion = R.string.pers_trabajadores_sub

    private var todos: List<TrabajadorDTO> = emptyList()
    private var empresas: List<EmpresaDTO> = emptyList()
    private var cargo: String? = null
    private var estado: Boolean? = true
    private var texto: String = ""
    private lateinit var lista: LinearLayout

    override suspend fun traer(): String? {
        val t = RetrofitClient.instance.getTrabajadores()
        todos = t.body().orEmpty()
        empresas = RetrofitClient.instance.getEmpresasPersonal().body().orEmpty()
        return mensajeDe(t)
    }

    override fun pintar() {
        val activos = todos.count { it.activo != false }
        binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.pers_activos, activos, activos))
        Bloques.boton(hoja, getString(R.string.pers_nuevo_trabajador), icono = R.drawable.ic_add) {
            Personal.formularioTrabajador(this, null, empresas) { recargar() }
        }
        val buscar = Bloques.campo(hoja, getString(R.string.pers_buscar), valor = texto)
        buscar.setStartIconDrawable(R.drawable.ic_search)
        buscar.editText?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { texto = buscar.textoLimpio; pintarLista() }
        })
        val filtros = ChipGroup(requireContext()).apply { isSingleSelection = true; isSelectionRequired = true; isSingleLine = true }
        (listOf(null to getString(R.string.pers_todos_cargos)) + Personal.CARGOS.map { it.first to getString(it.second) }).forEach { (v, t) ->
            filtros.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = t; isCheckable = true; isChecked = v == cargo; setOnClickListener { cargo = v; pintarLista() }
            })
        }
        hoja.addView(android.widget.HorizontalScrollView(requireContext()).apply { isHorizontalScrollBarEnabled = false; addView(filtros) })
        val estados = ChipGroup(requireContext()).apply { isSingleSelection = true; isSelectionRequired = true }
        listOf(true to getString(R.string.pers_activos_chip), false to getString(R.string.pers_inactivos_chip), null to getString(R.string.pers_todos))
            .forEach { (v, t) ->
                estados.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                    text = t; isCheckable = true; isChecked = v == estado; setOnClickListener { estado = v; pintarLista() }
                })
            }
        hoja.addView(estados)
        lista = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        hoja.addView(lista)
        pintarLista()
    }

    private fun pintarLista() {
        lista.removeAllViews()
        val q = FiltroTorreApto.normalizar(texto)
        val visibles = todos.filter { t ->
            (cargo == null || t.cargo == cargo) && (estado == null || (t.activo != false) == estado) &&
                (q.isEmpty() || FiltroTorreApto.normalizar(t.nombreCompleto.orEmpty()).contains(q) || t.documento.orEmpty().contains(q))
        }.sortedBy { it.nombreCompleto }
        if (visibles.isEmpty()) { Bloques.parrafo(lista, getString(R.string.pers_sin_trabajadores)); return }
        visibles.forEach { t ->
            val vinculo = if (t.vinculo == "EMPRESA") getString(R.string.pers_de_empresa, t.empresa.orEmpty()) else getString(R.string.pers_directo)
            Bloques.tarjeta(lista, t.nombreCompleto.orEmpty(),
                detalle = Personal.cargo(this, t.cargo) + " · " + vinculo,
                extra = getString(R.string.pers_doc_salario, t.documento.orEmpty(), Formatos.moneda(t.salarioBase ?: 0.0)),
                etiqueta = if (t.activo == false) getString(R.string.pers_inactivo) to TonoEstado.NEUTRO else getString(R.string.pers_activo) to TonoEstado.EXITO) {
                findNavController().navigate(R.id.trabajadorDetalleFragment, bundleOf(Personal.ARG_TRABAJADOR to t.id))
            }
        }
    }
}
