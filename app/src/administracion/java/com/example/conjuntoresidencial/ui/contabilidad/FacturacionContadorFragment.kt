package com.example.conjuntoresidencial.ui.contabilidad

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentPantallaContenidoBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.model.FacturacionBorrador
import com.example.conjuntoresidencial.model.VistaPreviaDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * P11 bloque 7: la facturación del mes en su propia pantalla. El contador elige el mes, el concepto,
 * si es un valor fijo por apartamento o un total repartido por coeficiente, la fecha límite, el
 * interés de mora, la descripción y qué apartamentos quedan por fuera; ve la vista previa (cuánto
 * le toca a cada uno) y la manda al administrador. Con [FormulariosCartera.ARG_SOLICITUD] corrige
 * o edita esa facturación (vuelve a aprobación). Cada cuota queda con su referencia de pago.
 */
class FacturacionContadorFragment : Fragment() {

    private var _binding: FragmentPantallaContenidoBinding? = null
    private val binding get() = _binding!!
    private val solicitudId: Long? get() = arguments?.getLong(FormulariosCartera.ARG_SOLICITUD)?.takeIf { arguments?.containsKey(FormulariosCartera.ARG_SOLICITUD) == true }

    private lateinit var mes: TextInputLayout
    private lateinit var concepto: TextInputLayout
    private lateinit var modo: MaterialButtonToggleGroup
    private lateinit var valor: TextInputLayout
    private lateinit var total: TextInputLayout
    private lateinit var limite: TextInputLayout
    private lateinit var interes: TextInputLayout
    private lateinit var descripcion: TextInputLayout
    private lateinit var nota: TextInputLayout
    private lateinit var btnExcluir: MaterialButton
    private lateinit var previa: LinearLayout
    private var aptos: List<Apartamento> = emptyList()
    private var excluidos: MutableSet<Long> = mutableSetOf()
    private var aprobadaAntes = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPantallaContenidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cont_facturar_titulo, R.string.cont_facturar_sub)
        binding.swipe.isEnabled = false
        armar()
        cargarApartamentos()
        solicitudId?.let { cargarSolicitud(it) }
    }

    private fun armar() {
        val c = binding.contenido
        val siguiente = YearMonth.now().plusMonths(1)
        Bloques.seccion(c, getString(R.string.cont_que_se_cobra))
        mes = Bloques.campo(c, getString(R.string.cont_mes), InputType.TYPE_CLASS_TEXT, getString(R.string.cont_mes_formato), siguiente.toString())
        concepto = Bloques.campo(c, getString(R.string.cont_concepto), ayuda = getString(R.string.cont_concepto_fact_ayuda),
            valor = getString(R.string.cont_concepto_admin), maximo = 80)

        Bloques.seccion(c, getString(R.string.cont_como_se_cobra))
        modo = MaterialButtonToggleGroup(requireContext()).apply { isSingleSelection = true; isSelectionRequired = true }
        val fijo = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            id = View.generateViewId(); setText(R.string.cont_modo_fijo)
        }
        val coef = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            id = View.generateViewId(); setText(R.string.cont_modo_coeficiente)
        }
        modo.addView(fijo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        modo.addView(coef, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        c.addView(modo)
        modo.check(fijo.id)
        valor = Bloques.campo(c, getString(R.string.cont_valor_por_apto), InputType.TYPE_CLASS_NUMBER)
        total = Bloques.campo(c, getString(R.string.cont_total_repartir), InputType.TYPE_CLASS_NUMBER, getString(R.string.cont_total_ayuda))
        modo.addOnButtonCheckedListener { _, _, _ -> mostrarModo() }
        mostrarModo()

        Bloques.seccion(c, getString(R.string.cont_plazo))
        limite = FormulariosCartera.campoFecha(this, c, R.string.cont_fecha_limite, siguiente.atDay(10).toString())
        interes = Bloques.campo(c, getString(R.string.cont_interes), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            getString(R.string.cont_interes_ayuda))
        descripcion = Bloques.campo(c, getString(R.string.cont_descripcion),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            getString(R.string.cont_descripcion_ayuda), maximo = 300)

        Bloques.seccion(c, getString(R.string.cont_a_quien))
        btnExcluir = Bloques.boton(c, getString(R.string.cont_todos_incluidos), relleno = false) { elegirExcluidos() }

        Bloques.seccion(c, getString(R.string.cont_para_el_admin))
        nota = Bloques.campo(c, getString(R.string.cont_nota_admin), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            getString(R.string.cont_nota_ayuda), maximo = 500)

        Bloques.boton(c, getString(R.string.cont_ver_vista_previa), relleno = false, icono = R.drawable.ic_search) { verVistaPrevia() }
        previa = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        c.addView(previa)
        Bloques.boton(c, getString(if (solicitudId == null) R.string.cont_enviar_aprobacion else R.string.cont_reenviar_aprobacion),
            icono = R.drawable.ic_check_circle) { enviar() }
        Bloques.boton(c, getString(R.string.cont_ver_cuotas_mes), relleno = false, icono = R.drawable.ic_receipt) {
            findNavController().navigate(R.id.cuotasFragment)
        }
        binding.estado.mostrarContenido()
    }

    private fun mostrarModo() {
        val porCoef = modo.checkedButtonId == modo.getChildAt(1).id
        valor.visibility = if (porCoef) View.GONE else View.VISIBLE
        total.visibility = if (porCoef) View.VISIBLE else View.GONE
    }

    private fun cargarApartamentos() {
        viewLifecycleOwner.lifecycleScope.launch {
            aptos = try { RetrofitClient.instance.getApartamentos().body().orEmpty() } catch (_: Exception) { emptyList() }
            if (_binding != null) binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.cont_aptos_conjunto, aptos.size, aptos.size))
        }
    }

    /** Corregir o editar: el formulario con lo que pedía esa versión. */
    private fun cargarSolicitud(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val d = try { RetrofitClient.instance.detalleSolicitud(id).body() } catch (_: Exception) { null }
            if (_binding == null) return@launch
            val f = d?.detalle ?: run { aviso(R.string.cont_error_cargar).show(); return@launch }
            aprobadaAntes = d.solicitud.estado == "APROBADA" || d.solicitud.edicion
            mes.editText?.setText(f["periodo"] as? String)
            concepto.editText?.setText(f["concepto"] as? String)
            // Después de aprobada, el mes y el concepto quedan
            mes.isEnabled = !aprobadaAntes
            concepto.isEnabled = !aprobadaAntes
            if (aprobadaAntes) Bloques.parrafo(binding.contenido, getString(R.string.cont_editar_aprobada_ayuda)).also {
                binding.contenido.removeView(it); binding.contenido.addView(it, 0)
            }
            if (f["modo"] == "COEFICIENTE") modo.check(modo.getChildAt(1).id) else modo.check(modo.getChildAt(0).id)
            (f["valor"] as? Number)?.let { valor.editText?.setText(it.toLong().toString()) }
            (f["total"] as? Number)?.let { total.editText?.setText(it.toLong().toString()) }
            limite.editText?.setText(f["fechaLimite"] as? String)
            (f["interesMora"] as? Number)?.let { interes.editText?.setText(it.toString()) }
            descripcion.editText?.setText(f["descripcion"] as? String)
            excluidos = (f["excluidos"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }?.toMutableSet() ?: mutableSetOf()
            ponerExcluidos()
            nota.editText?.setText(d.solicitud.nota)
            binding.encabezado.ponerInfo(getString(R.string.cont_corrigiendo_version, d.solicitud.version))
        }
    }

    private fun elegirExcluidos() {
        if (aptos.isEmpty()) { aviso(R.string.cont_error_cargar).show(); return }
        val ordenados = aptos.sortedWith(compareBy({ it.torre.padStart(4, '0') }, { it.apto.padStart(6, '0') }))
        val marcados = ordenados.map { it.id in excluidos }.toBooleanArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cont_excluir_titulo)
            .setMultiChoiceItems(ordenados.map { getString(R.string.cont_torre_apto, it.torre, it.apto) }.toTypedArray(), marcados) { _, i, m ->
                marcados[i] = m
            }
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                excluidos = ordenados.filterIndexed { i, _ -> marcados[i] }.map { it.id }.toMutableSet()
                ponerExcluidos()
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun ponerExcluidos() {
        btnExcluir.text = if (excluidos.isEmpty()) getString(R.string.cont_todos_incluidos)
        else resources.getQuantityString(R.plurals.cont_excluidos, excluidos.size, excluidos.size)
    }

    private fun numero(til: TextInputLayout): Double? =
        til.textoLimpio.replace(".", "").replace(",", ".").replace("$", "").toDoubleOrNull()

    /** El borrador del formulario; null (con los errores marcados) si falta algo. */
    private fun borrador(): FacturacionBorrador? {
        val porCoef = modo.checkedButtonId == modo.getChildAt(1).id
        val m = mes.textoLimpio.takeIf { runCatching { YearMonth.parse(it) }.isSuccess }
        mes.error = if (m == null) getString(R.string.cont_mes_invalido) else null
        val v = if (porCoef) null else numero(valor)
        val t = if (porCoef) numero(total) else null
        valor.error = if (!porCoef && (v == null || v <= 0)) getString(R.string.cont_falta_valor) else null
        total.error = if (porCoef && (t == null || t <= 0)) getString(R.string.cont_falta_valor) else null
        val fl = limite.textoLimpio.ifEmpty { null }
        val fechaOk = fl == null || runCatching { LocalDate.parse(fl) }.isSuccess
        limite.error = if (!fechaOk) getString(R.string.cont_fecha_invalida) else null
        val i = interes.textoLimpio.replace(",", ".").ifEmpty { null }?.toDoubleOrNull()
        interes.error = if (interes.textoLimpio.isNotEmpty() && (i == null || i < 0 || i > 10)) getString(R.string.cont_interes_invalido) else null
        if (m == null || mes.error != null || valor.error != null || total.error != null || !fechaOk || interes.error != null) return null
        return FacturacionBorrador(m, concepto.textoLimpio.ifEmpty { null }, if (porCoef) "COEFICIENTE" else "FIJO",
            v, t, fl, i, descripcion.textoLimpio.ifEmpty { null }, excluidos.toList())
    }

    private fun verVistaPrevia() {
        val b = borrador() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.vistaPreviaFacturacion(b)
                if (_binding == null) return@launch
                val v = r.body()
                if (r.isSuccessful && v != null) pintarVista(v)
                else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.cont_error_cargar))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun pintarVista(v: VistaPreviaDTO) {
        previa.removeAllViews()
        Bloques.seccion(previa, getString(R.string.cont_vista_previa))
        Bloques.cifras(previa, v.apartamentos.toString() to getString(R.string.cont_aptos), Formatos.moneda(v.total) to getString(R.string.cont_total))
        if (v.minimo != null && v.maximo != null && v.minimo != v.maximo) {
            Bloques.parrafo(previa, getString(R.string.cont_rango, Formatos.moneda(v.minimo), Formatos.moneda(v.maximo)))
        }
        v.avisos.forEach { Bloques.parrafo(previa, "• $it", apagado = false) }
        if (v.valores.isNotEmpty()) {
            val t = Bloques.tarjeta(previa, getString(R.string.cont_por_apartamento))
            v.valores.take(40).forEach { Bloques.dato(t, getString(R.string.cont_torre_apto, it.torre, it.apto), Formatos.moneda(it.valor)) }
            if (v.valores.size > 40) Bloques.parrafo(t, getString(R.string.cont_y_mas, v.valores.size - 40))
        }
        binding.desplazar.post { binding.desplazar.smoothScrollTo(0, previa.top) }
    }

    private fun enviar() {
        val b = borrador() ?: return
        val detalle = mapOf("periodo" to b.periodo, "concepto" to b.concepto, "modo" to b.modo, "valor" to b.valor, "total" to b.total,
            "fechaLimite" to b.fechaLimite, "interesMora" to b.interesMora, "descripcion" to b.descripcion, "excluidos" to b.excluidos)
        FormulariosCartera.mandar(this, "FACTURACION", detalle, nota.textoLimpio.ifEmpty { null }, solicitudId) {
            if (_binding != null) findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
