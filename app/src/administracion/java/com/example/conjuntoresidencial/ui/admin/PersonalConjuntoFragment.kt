package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentPersonalConjuntoBinding
import com.example.conjuntoresidencial.model.*
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.LocalDate
import java.time.YearMonth

/**
 * Personal y presupuesto del conjunto (P6 bloque 7, solo administrador): trabajadores y las
 * empresas que los prestan, turnos sin cruces, nómina del mes y presupuesto por cargo, con el
 * resumen de lo presupuestado contra la nómina. Las listas y formularios van en diálogos.
 */
class PersonalConjuntoFragment : Fragment() {
    private var _binding: FragmentPersonalConjuntoBinding? = null
    private val binding get() = _binding!!
    private var periodo: String = YearMonth.now().toString()

    private val cargos = listOf(
        "VIGILANTE" to R.string.cargo_vigilante, "ASEO" to R.string.cargo_aseo, "JARDINERIA" to R.string.cargo_jardineria,
        "MANTENIMIENTO" to R.string.cargo_mantenimiento, "ADMINISTRATIVO" to R.string.cargo_administrativo, "OTRO" to R.string.cargo_otro
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPersonalConjuntoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.personal_conj_titulo, R.string.personal_descripcion)
        binding.btnPeriodo.setOnClickListener { cambiarPeriodo() }
        binding.btnTrabajadores.setOnClickListener { listaTrabajadores() }
        binding.btnEmpresas.setOnClickListener { listaEmpresas() }
        binding.btnTurnos.setOnClickListener { listaTurnos() }
        binding.btnNomina.setOnClickListener { nomina() }
        binding.btnPresupuesto.setOnClickListener { presupuesto() }
        cargarResumen()
    }

    private fun etiquetaCargo(codigo: String?) = cargos.firstOrNull { it.first == codigo }?.let { getString(it.second) } ?: codigo.orEmpty()

    // ---------- Resumen y mes ----------

    private fun cambiarPeriodo() {
        pedirTexto(getString(R.string.personal_mes), R.string.personal_mes_hint, periodo, InputType.TYPE_CLASS_TEXT,
            validar = { if (Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(it)) null else getString(R.string.personal_mes_invalido) }) {
            periodo = it
            cargarResumen()
        }
    }

    private fun cargarResumen() {
        binding.btnPeriodo.text = getString(R.string.personal_mes_boton, periodo)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getResumenPresupuesto(periodo)
                val resumen = r.body()
                if (_binding == null) return@launch
                binding.tvResumenPresupuesto.text = if (!r.isSuccessful || resumen == null) {
                    RetrofitClient.mensajeDeError(r, getString(R.string.personal_error))
                } else if (resumen.lineas.isEmpty()) {
                    getString(R.string.personal_sin_resumen)
                } else {
                    (resumen.lineas.map { getString(R.string.personal_linea_resumen, etiquetaCargo(it.cargo),
                        Formatos.moneda(it.presupuestado), Formatos.moneda(it.nomina)) } +
                        getString(R.string.personal_total_resumen, Formatos.moneda(resumen.totalPresupuestado),
                            Formatos.moneda(resumen.totalNomina), Formatos.moneda(resumen.diferencia))).joinToString("\n")
                }
            } catch (e: Exception) {
                if (_binding != null) binding.tvResumenPresupuesto.text = RetrofitClient.mensajeDeExcepcion(e)
            }
        }
    }

    // ---------- Ayudas de formulario ----------

    private fun campo(hint: Int, inicial: String? = null, tipo: Int = InputType.TYPE_CLASS_TEXT): Pair<TextInputLayout, TextInputEditText> {
        val et = TextInputEditText(requireContext()).apply { inputType = tipo; setText(inicial) }
        val til = TextInputLayout(requireContext()).apply {
            this.hint = getString(hint)
            addView(et)
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        return til to et
    }

    private fun spinner(opciones: List<String>, seleccion: Int = 0) = Spinner(requireContext()).apply {
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)
        setSelection(seleccion.coerceAtLeast(0))
    }

    private fun contenido(vararg vistas: View): ScrollView {
        val margen = (24 * resources.displayMetrics.density).toInt()
        return ScrollView(requireContext()).apply {
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(margen, margen / 2, margen, 0)
                vistas.forEach { addView(it) }
            })
        }
    }

    private fun TextInputEditText.texto() = text?.toString()?.trim().orEmpty()

    /** Pide la lista al servidor y la muestra cuando llega; los errores se avisan. */
    private fun <T> cargar(llamada: suspend () -> Response<List<T>>, mostrar: (List<T>) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = llamada()
                if (_binding == null) return@launch
                if (r.isSuccessful) mostrar(r.body().orEmpty())
                else aviso(RetrofitClient.mensajeDeError(r, getString(R.string.personal_error))).show()
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun accion(exito: Int, llamada: suspend () -> Response<*>, despues: () -> Unit) =
        ejecutarAccion(getString(exito), llamada) { despues() }

    private fun dialogoFormulario(titulo: String, vista: View, validar: () -> Boolean, guardar: () -> Unit) {
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo).setView(vista)
            .setPositiveButton(R.string.btn_guardar, null).setNegativeButton(R.string.btn_cancelar, null).show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validar()) { dialogo.dismiss(); guardar() }
        }
    }

    // ---------- Trabajadores ----------

    private fun listaTrabajadores() = cargar({ RetrofitClient.instance.getTrabajadores() }) { lista ->
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.personal_trabajadores)
            .setItems(lista.map {
                getString(R.string.personal_fila_trabajador, it.nombreCompleto, etiquetaCargo(it.cargo)) +
                    (if (it.activo == false) " · " + getString(R.string.personal_inactivo) else "")
            }.toTypedArray()) { _, i -> opcionesTrabajador(lista[i]) }
            .setPositiveButton(R.string.personal_agregar) { _, _ -> formularioTrabajador(null) }
            .setNegativeButton(R.string.btn_cerrar, null).show()
    }

    private fun opcionesTrabajador(t: TrabajadorDTO) {
        elegirOpcion(t.nombreCompleto.orEmpty(), listOf(
            R.string.equipo_editar_datos to { formularioTrabajador(t) },
            (if (t.activo == false) R.string.personal_activar else R.string.personal_desactivar) to {
                accion(R.string.personal_guardado, { RetrofitClient.instance.actualizarTrabajador(t.id!!, TrabajadorDTO(activo = t.activo == false)) }) {}
            }
        ))
    }

    private fun formularioTrabajador(actual: TrabajadorDTO?) = cargar({ RetrofitClient.instance.getEmpresasPersonal() }) { empresas ->
        val nombre = campo(R.string.personal_nombre, actual?.nombreCompleto, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val documento = campo(R.string.porteria_documento, actual?.documento)
        val telefono = campo(R.string.personal_telefono, actual?.telefono, InputType.TYPE_CLASS_PHONE)
        val salario = campo(R.string.personal_salario, actual?.salarioBase?.toLong()?.toString(), InputType.TYPE_CLASS_NUMBER)
        val cargo = spinner(cargos.map { getString(it.second) }, cargos.indexOfFirst { it.first == actual?.cargo })
        val vinculo = spinner(listOf(getString(R.string.personal_directo), getString(R.string.personal_por_empresa)),
            if (actual?.vinculo == "EMPRESA") 1 else 0)
        val empresa = spinner(empresas.map { it.nombre.orEmpty() }, empresas.indexOfFirst { it.id == actual?.empresaId })
        dialogoFormulario(
            if (actual == null) getString(R.string.personal_agregar) else actual.nombreCompleto.orEmpty(),
            contenido(nombre.first, documento.first, telefono.first, salario.first, cargo, vinculo, empresa),
            validar = {
                nombre.first.error = if (nombre.second.texto().isEmpty()) getString(R.string.equipo_dato_obligatorio) else null
                documento.first.error = if (documento.second.texto().isEmpty()) getString(R.string.equipo_dato_obligatorio) else null
                val sinEmpresa = vinculo.selectedItemPosition == 1 && empresas.isEmpty()
                if (sinEmpresa) aviso(R.string.personal_sin_empresas).show()
                nombre.first.error == null && documento.first.error == null && !sinEmpresa
            }
        ) {
            val porEmpresa = vinculo.selectedItemPosition == 1
            val cuerpo = TrabajadorDTO(
                nombreCompleto = nombre.second.texto(), documento = documento.second.texto(), telefono = telefono.second.texto(),
                cargo = cargos[cargo.selectedItemPosition].first, vinculo = if (porEmpresa) "EMPRESA" else "DIRECTO",
                empresaId = if (porEmpresa) empresas[empresa.selectedItemPosition].id else null,
                salarioBase = salario.second.texto().toDoubleOrNull() ?: 0.0
            )
            accion(R.string.personal_guardado, {
                if (actual == null) RetrofitClient.instance.crearTrabajador(cuerpo)
                else RetrofitClient.instance.actualizarTrabajador(actual.id!!, cuerpo)
            }) {}
        }
    }

    // ---------- Empresas ----------

    private fun listaEmpresas() = cargar({ RetrofitClient.instance.getEmpresasPersonal() }) { lista ->
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.personal_empresas)
            .setItems(lista.map { it.nombre.orEmpty() }.toTypedArray()) { _, i -> opcionesEmpresa(lista[i]) }
            .setPositiveButton(R.string.personal_agregar) { _, _ -> formularioEmpresa(null) }
            .setNegativeButton(R.string.btn_cerrar, null).show()
    }

    private fun opcionesEmpresa(e: EmpresaDTO) {
        elegirOpcion(e.nombre.orEmpty(), listOf(
            R.string.equipo_editar_datos to { formularioEmpresa(e) },
            R.string.personal_conj_eliminar to {
                confirmar(getString(R.string.personal_eliminar_empresa), e.nombre, R.string.residente_eliminar_si) {
                    accion(R.string.personal_guardado, { RetrofitClient.instance.eliminarEmpresaPersonal(e.id!!) }) {}
                }
            }
        ))
    }

    private fun formularioEmpresa(actual: EmpresaDTO?) {
        val nombre = campo(R.string.personal_nombre, actual?.nombre)
        val nit = campo(R.string.personal_nit, actual?.nit)
        val contacto = campo(R.string.personal_contacto, actual?.contacto)
        val telefono = campo(R.string.personal_telefono, actual?.telefono, InputType.TYPE_CLASS_PHONE)
        dialogoFormulario(actual?.nombre ?: getString(R.string.personal_agregar),
            contenido(nombre.first, nit.first, contacto.first, telefono.first),
            validar = {
                nombre.first.error = if (nombre.second.texto().isEmpty()) getString(R.string.equipo_dato_obligatorio) else null
                nombre.first.error == null
            }
        ) {
            val cuerpo = EmpresaDTO(nombre = nombre.second.texto(), nit = nit.second.texto(), contacto = contacto.second.texto(),
                telefono = telefono.second.texto())
            accion(R.string.personal_guardado, {
                if (actual == null) RetrofitClient.instance.crearEmpresaPersonal(cuerpo)
                else RetrofitClient.instance.actualizarEmpresaPersonal(actual.id!!, cuerpo)
            }) {}
        }
    }

    // ---------- Turnos ----------

    private fun listaTurnos() {
        val hoy = LocalDate.now()
        cargar({ RetrofitClient.instance.getTurnos(hoy.toString(), hoy.plusDays(14).toString()) }) { lista ->
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.personal_turnos)
                .setItems(lista.map {
                    getString(R.string.personal_fila_turno, Formatos.fecha(it.fecha), it.horaInicio?.take(5), it.horaFin?.take(5),
                        it.trabajador, etiquetaCargo(it.cargo))
                }.toTypedArray()) { _, i ->
                    confirmar(getString(R.string.personal_eliminar_turno), null, R.string.residente_eliminar_si) {
                        accion(R.string.personal_guardado, { RetrofitClient.instance.eliminarTurno(lista[i].id!!) }) {}
                    }
                }
                .setPositiveButton(R.string.personal_agregar) { _, _ -> formularioTurno() }
                .setNegativeButton(R.string.btn_cerrar, null).show()
        }
    }

    private fun formularioTurno() = cargar({ RetrofitClient.instance.getTrabajadores() }) { todos ->
        val activos = todos.filter { it.activo != false }
        if (activos.isEmpty()) { aviso(R.string.personal_sin_trabajadores).show(); return@cargar }
        val trabajador = spinner(activos.map { getString(R.string.personal_fila_trabajador, it.nombreCompleto, etiquetaCargo(it.cargo)) })
        val fecha = campo(R.string.personal_fecha, LocalDate.now().toString())
        val inicio = campo(R.string.personal_hora_inicio, "06:00")
        val fin = campo(R.string.personal_hora_fin, "14:00")
        val horaValida = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        dialogoFormulario(getString(R.string.personal_turnos), contenido(trabajador, fecha.first, inicio.first, fin.first),
            validar = {
                fecha.first.error = if (runCatching { LocalDate.parse(fecha.second.texto()) }.isFailure) getString(R.string.personal_fecha_invalida) else null
                inicio.first.error = if (horaValida.matches(inicio.second.texto())) null else getString(R.string.personal_hora_invalida)
                fin.first.error = if (horaValida.matches(fin.second.texto())) null else getString(R.string.personal_hora_invalida)
                listOf(fecha, inicio, fin).all { it.first.error == null }
            }
        ) {
            accion(R.string.personal_guardado, {
                RetrofitClient.instance.crearTurno(TurnoDTO(trabajadorId = activos[trabajador.selectedItemPosition].id,
                    fecha = fecha.second.texto(), horaInicio = inicio.second.texto() + ":00", horaFin = fin.second.texto() + ":00"))
            }) {}
        }
    }

    // ---------- Nómina ----------

    private fun nomina() = cargar({ RetrofitClient.instance.getNomina(periodo) }) { lista ->
        val dialogo = MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.personal_nomina_mes, periodo))
        if (lista.isEmpty()) dialogo.setMessage(R.string.personal_nomina_vacia)
        else dialogo.setItems(lista.map {
            getString(R.string.personal_fila_nomina, it.trabajador, Formatos.moneda(it.neto),
                getString(if (it.estado == "PAGADA") R.string.personal_pagada else R.string.personal_pendiente))
        }.toTypedArray()) { _, i -> ajustarNomina(lista[i]) }
        dialogo.setPositiveButton(R.string.personal_generar_nomina) { _, _ ->
            accion(R.string.personal_nomina_generada, { RetrofitClient.instance.generarNomina(periodo) }) { cargarResumen() }
        }.setNegativeButton(R.string.btn_cerrar, null).show()
    }

    private fun ajustarNomina(n: NominaDTO) {
        if (n.estado == "PAGADA") { aviso(R.string.personal_nomina_pagada).show(); return }
        val bonificaciones = campo(R.string.personal_bonificaciones, n.bonificaciones.toLong().toString(), InputType.TYPE_CLASS_NUMBER)
        val descuentos = campo(R.string.personal_descuentos, n.descuentos.toLong().toString(), InputType.TYPE_CLASS_NUMBER)
        val pagada = spinner(listOf(getString(R.string.personal_pendiente), getString(R.string.personal_pagada)))
        dialogoFormulario(n.trabajador, contenido(bonificaciones.first, descuentos.first, pagada), validar = { true }) {
            accion(R.string.personal_guardado, {
                RetrofitClient.instance.ajustarNomina(n.id, CambioNomina(
                    bonificaciones = bonificaciones.second.texto().toDoubleOrNull() ?: 0.0,
                    descuentos = descuentos.second.texto().toDoubleOrNull() ?: 0.0,
                    estado = if (pagada.selectedItemPosition == 1) "PAGADA" else "GENERADA"))
            }) { cargarResumen() }
        }
    }

    // ---------- Presupuesto ----------

    private fun presupuesto() = cargar({ RetrofitClient.instance.getPresupuesto(periodo) }) { lineas ->
        val campos = cargos.map { (codigo, etiqueta) ->
            codigo to campo(etiqueta, lineas.firstOrNull { it.cargo == codigo }?.monto?.toLong()?.toString(), InputType.TYPE_CLASS_NUMBER)
        }
        dialogoFormulario(getString(R.string.personal_presupuesto_mes, periodo), contenido(*campos.map { it.second.first }.toTypedArray()),
            validar = { true }) {
            val nuevas = campos.mapNotNull { (codigo, c) -> c.second.texto().toDoubleOrNull()?.let { LineaPresupuesto(codigo, it) } }
            accion(R.string.personal_guardado, { RetrofitClient.instance.fijarPresupuesto(periodo, nuevas) }) { cargarResumen() }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
