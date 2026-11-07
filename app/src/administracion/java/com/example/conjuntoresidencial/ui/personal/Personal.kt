package com.example.conjuntoresidencial.ui.personal

import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.EmpresaDTO
import com.example.conjuntoresidencial.model.TrabajadorDTO
import com.example.conjuntoresidencial.model.TurnoDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.contabilidad.FormulariosCartera
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * P11 bloque 8: lo común de las pantallas de personal y presupuesto: los cargos, el selector de mes
 * o de semana, la llamada que avisa y recarga, y los formularios de trabajador, empresa y turno.
 */
object Personal {

    const val ARG_TRABAJADOR = "trabajadorId"
    private val ES = Locale.forLanguageTag("es-CO")

    val CARGOS = listOf(
        "VIGILANTE" to R.string.cargo_vigilante, "ASEO" to R.string.cargo_aseo, "JARDINERIA" to R.string.cargo_jardineria,
        "MANTENIMIENTO" to R.string.cargo_mantenimiento, "ADMINISTRATIVO" to R.string.cargo_administrativo, "OTRO" to R.string.cargo_otro
    )

    fun cargo(f: Fragment, codigo: String?): String =
        CARGOS.firstOrNull { it.first == codigo }?.let { f.getString(it.second) } ?: codigo.orEmpty()

    fun mes(periodo: String): String = runCatching {
        val m = YearMonth.parse(periodo)
        m.month.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.titlecase(ES) } + " " + m.year
    }.getOrDefault(periodo)

    fun dia(fecha: LocalDate): String =
        fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.titlecase(ES) } + " " + fecha.dayOfMonth + " de " +
            fecha.month.getDisplayName(TextStyle.FULL, ES)

    /** ‹ Noviembre 2026 › : cambia de mes (o de semana) con las flechas. */
    fun selector(padre: ViewGroup, texto: String, anterior: String, siguiente: String, alMover: (Int) -> Unit) {
        val c = padre.context
        val fila = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun flecha(icono: Int, descripcion: String, paso: Int) = MaterialButton(c, null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            setIconResource(icono); contentDescription = descripcion; setOnClickListener { alMover(paso) }
        }
        fila.addView(flecha(R.drawable.ic_arrow_back, anterior, -1))
        fila.addView(Bloques.texto(c, texto, R.style.TextAppearance_App_TitleMedium).apply {
            gravity = Gravity.CENTER
            isFocusable = true
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fila.addView(flecha(R.drawable.ic_arrow_forward, siguiente, 1))
        padre.addView(fila)
    }

    /** Llama, avisa y recarga. */
    fun <T> accion(f: Fragment, exito: Int, llamada: suspend () -> Response<T>, despues: (T?) -> Unit = {}) {
        f.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = llamada()
                if (f.view == null) return@launch
                if (r.isSuccessful) { f.aviso(exito).show(); despues(r.body()) }
                else f.aviso(RetrofitClient.mensajeDeError(r, f.getString(R.string.pers_error))).show()
            } catch (e: Exception) {
                if (f.view != null) f.aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun caja(f: Fragment) = LinearLayout(f.requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        val d = f.resources.displayMetrics.density
        setPadding((24 * d).toInt(), (4 * d).toInt(), (24 * d).toInt(), 0)
    }

    private fun dialogo(f: Fragment, titulo: CharSequence, contenido: LinearLayout, boton: Int, alAceptar: () -> Boolean) {
        val d = MaterialAlertDialogBuilder(f.requireContext())
            .setTitle(titulo)
            .setView(android.widget.ScrollView(f.requireContext()).apply { addView(contenido) })
            .setPositiveButton(boton, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (alAceptar()) d.dismiss() }
    }

    private fun chips(f: Fragment, padre: LinearLayout, titulo: Int, opciones: List<Pair<String, String>>, elegido: String?): ChipGroup {
        Bloques.parrafo(padre, f.getString(titulo))
        val g = ChipGroup(f.requireContext()).apply { isSingleSelection = true; isSelectionRequired = true }
        opciones.forEach { (valor, texto) ->
            g.addView(Chip(f.requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = texto; tag = valor; isCheckable = true; isChecked = valor == elegido
            })
        }
        if (g.checkedChipId == android.view.View.NO_ID && g.childCount > 0) (g.getChildAt(0) as Chip).isChecked = true
        padre.addView(g)
        return g
    }

    private fun ChipGroup.elegido(): String? = findViewById<Chip>(checkedChipId)?.tag as? String

    private fun numero(til: TextInputLayout): Double? =
        til.textoLimpio.replace(".", "").replace(",", ".").replace("$", "").ifEmpty { null }?.toDoubleOrNull()

    /** Crear o editar un trabajador (con su empresa si es prestado). */
    fun formularioTrabajador(f: Fragment, actual: TrabajadorDTO?, empresas: List<EmpresaDTO>, despues: () -> Unit) {
        val c = caja(f)
        val nombre = Bloques.campo(c, f.getString(R.string.pers_nombre), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, valor = actual?.nombreCompleto)
        val documento = Bloques.campo(c, f.getString(R.string.pers_documento), InputType.TYPE_CLASS_NUMBER, valor = actual?.documento)
        val telefono = Bloques.campo(c, f.getString(R.string.pers_telefono), InputType.TYPE_CLASS_PHONE, valor = actual?.telefono)
        val cargo = chips(f, c, R.string.pers_cargo, CARGOS.map { it.first to f.getString(it.second) }, actual?.cargo)
        val activas = empresas.filter { it.activa != false || it.id == actual?.empresaId }
        val vinculo = chips(f, c, R.string.pers_vinculo, listOf("DIRECTO" to f.getString(R.string.pers_directo),
            "EMPRESA" to f.getString(R.string.pers_por_empresa)), actual?.vinculo ?: "DIRECTO")
        val empresa = chips(f, c, R.string.pers_empresa, activas.map { (it.id ?: 0).toString() to it.nombre.orEmpty() },
            actual?.empresaId?.toString())
        fun mostrarEmpresa() { empresa.visibility = if (vinculo.elegido() == "EMPRESA") android.view.View.VISIBLE else android.view.View.GONE }
        vinculo.setOnCheckedStateChangeListener { _, _ -> mostrarEmpresa() }
        mostrarEmpresa()
        val salario = Bloques.campo(c, f.getString(R.string.pers_salario), InputType.TYPE_CLASS_NUMBER,
            valor = actual?.salarioBase?.let { it.toLong().toString() })
        val ingreso = FormulariosCartera.campoFecha(f, c, R.string.pers_ingreso, actual?.fechaIngreso)
        dialogo(f, f.getString(if (actual == null) R.string.pers_nuevo_trabajador else R.string.pers_editar_trabajador), c, R.string.btn_guardar) {
            nombre.error = if (nombre.textoLimpio.isEmpty()) f.getString(R.string.pers_falta_nombre) else null
            documento.error = if (documento.textoLimpio.isEmpty()) f.getString(R.string.pers_falta_documento) else null
            val porEmpresa = vinculo.elegido() == "EMPRESA"
            if (porEmpresa && empresa.elegido() == null) { f.aviso(R.string.pers_falta_empresa).show(); return@dialogo false }
            val s = if (salario.textoLimpio.isEmpty()) null else numero(salario)
            salario.error = if (salario.textoLimpio.isNotEmpty() && (s == null || s < 0)) f.getString(R.string.pers_valor_invalido) else null
            val fi = ingreso.textoLimpio.ifEmpty { null }
            ingreso.error = if (fi != null && runCatching { LocalDate.parse(fi) }.isFailure) f.getString(R.string.cont_fecha_invalida) else null
            if (nombre.error != null || documento.error != null || salario.error != null || ingreso.error != null) return@dialogo false
            val dto = TrabajadorDTO(nombreCompleto = nombre.textoLimpio, documento = documento.textoLimpio,
                telefono = telefono.textoLimpio, cargo = cargo.elegido(), vinculo = vinculo.elegido(),
                empresaId = if (porEmpresa) empresa.elegido()?.toLongOrNull() else null, salarioBase = s, fechaIngreso = fi)
            accion(f, R.string.pers_guardado, {
                if (actual?.id == null) RetrofitClient.instance.crearTrabajador(dto) else RetrofitClient.instance.actualizarTrabajador(actual.id, dto)
            }) { despues() }
            true
        }
    }

    fun formularioEmpresa(f: Fragment, actual: EmpresaDTO?, despues: () -> Unit) {
        val c = caja(f)
        val nombre = Bloques.campo(c, f.getString(R.string.pers_empresa_nombre), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, valor = actual?.nombre)
        val nit = Bloques.campo(c, f.getString(R.string.pers_nit), InputType.TYPE_CLASS_TEXT, valor = actual?.nit)
        val contacto = Bloques.campo(c, f.getString(R.string.pers_contacto), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, valor = actual?.contacto)
        val telefono = Bloques.campo(c, f.getString(R.string.pers_telefono), InputType.TYPE_CLASS_PHONE, valor = actual?.telefono)
        dialogo(f, f.getString(if (actual == null) R.string.pers_nueva_empresa else R.string.pers_editar_empresa), c, R.string.btn_guardar) {
            nombre.error = if (nombre.textoLimpio.isEmpty()) f.getString(R.string.pers_falta_nombre) else null
            if (nombre.error != null) return@dialogo false
            val dto = EmpresaDTO(nombre = nombre.textoLimpio, nit = nit.textoLimpio, contacto = contacto.textoLimpio,
                telefono = telefono.textoLimpio, activa = actual?.activa ?: true)
            accion(f, R.string.pers_guardado, {
                if (actual?.id == null) RetrofitClient.instance.crearEmpresaPersonal(dto) else RetrofitClient.instance.actualizarEmpresaPersonal(actual.id, dto)
            }) { despues() }
            true
        }
    }

    /** Un campo de hora que abre el reloj al tocarlo. */
    private fun campoHora(f: Fragment, padre: LinearLayout, pista: Int, valor: String?): TextInputLayout {
        val til = Bloques.campo(padre, f.getString(pista), InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME,
            f.getString(R.string.pers_hora_formato), valor?.take(5))
        til.setEndIconMode(TextInputLayout.END_ICON_CUSTOM)
        til.setEndIconDrawable(R.drawable.ic_schedule)
        til.setEndIconContentDescription(R.string.pers_elegir_hora)
        til.setEndIconOnClickListener {
            val h = runCatching { LocalTime.parse(til.textoLimpio) }.getOrNull() ?: LocalTime.of(6, 0)
            android.app.TimePickerDialog(f.requireContext(), { _, hh, mm -> til.editText?.setText(String.format(Locale.US, "%02d:%02d", hh, mm)) },
                h.hour, h.minute, true).show()
        }
        return til
    }

    /**
     * Turno nuevo (o editar [actual]). Al crear se puede repetir los días siguientes. [fijo] deja
     * elegido un trabajador (desde su ficha).
     */
    fun formularioTurno(f: Fragment, trabajadores: List<TrabajadorDTO>, actual: TurnoDTO?, fecha: LocalDate?, fijo: Long?, despues: () -> Unit) {
        val c = caja(f)
        val activos = trabajadores.filter { it.activo != false }
        val quien = chips(f, c, R.string.pers_trabajador, activos.map { (it.id ?: 0).toString() to "${it.nombreCompleto} · ${cargo(f, it.cargo)}" },
            (actual?.trabajadorId ?: fijo)?.toString())
        if (fijo != null) quien.visibility = android.view.View.GONE
        val dia = FormulariosCartera.campoFecha(f, c, R.string.pers_fecha, actual?.fecha ?: (fecha ?: LocalDate.now()).toString())
        val inicio = campoHora(f, c, R.string.pers_hora_inicio, actual?.horaInicio ?: "06:00")
        val fin = campoHora(f, c, R.string.pers_hora_fin, actual?.horaFin ?: "14:00")
        val repetir = if (actual == null) Bloques.campo(c, f.getString(R.string.pers_repetir), InputType.TYPE_CLASS_NUMBER,
            f.getString(R.string.pers_repetir_ayuda), "0") else null
        dialogo(f, f.getString(if (actual == null) R.string.pers_nuevo_turno else R.string.pers_editar_turno), c, R.string.btn_guardar) {
            val d = runCatching { LocalDate.parse(dia.textoLimpio) }.getOrNull()
            val hi = runCatching { LocalTime.parse(inicio.textoLimpio) }.getOrNull()
            val hf = runCatching { LocalTime.parse(fin.textoLimpio) }.getOrNull()
            dia.error = if (d == null) f.getString(R.string.cont_fecha_invalida) else null
            inicio.error = if (hi == null) f.getString(R.string.pers_hora_invalida) else null
            fin.error = if (hf == null) f.getString(R.string.pers_hora_invalida) else if (hi != null && !hi.isBefore(hf)) f.getString(R.string.pers_fin_antes) else null
            val veces = (repetir?.textoLimpio?.toIntOrNull() ?: 0)
            repetir?.error = if (veces !in 0..30) f.getString(R.string.pers_repetir_invalido) else null
            val tid = quien.elegido()?.toLongOrNull()
            if (tid == null) { f.aviso(R.string.pers_falta_trabajador).show(); return@dialogo false }
            if (d == null || hi == null || hf == null || fin.error != null || repetir?.error != null) return@dialogo false
            val base = TurnoDTO(trabajadorId = tid, fecha = d.toString(), horaInicio = hi.toString(), horaFin = hf.toString())
            if (actual?.id != null) {
                accion(f, R.string.pers_guardado, { RetrofitClient.instance.actualizarTurno(actual.id, base) }) { despues() }
            } else {
                f.viewLifecycleOwner.lifecycleScope.launch {
                    var creados = 0
                    var error: String? = null
                    for (i in 0..veces) {
                        try {
                            val r = RetrofitClient.instance.crearTurno(base.copy(fecha = d.plusDays(i.toLong()).toString()))
                            if (r.isSuccessful) creados++ else if (error == null) error = RetrofitClient.mensajeDeError(r, f.getString(R.string.pers_error))
                        } catch (e: Exception) {
                            if (error == null) error = RetrofitClient.mensajeDeExcepcion(e)
                        }
                    }
                    if (f.view == null) return@launch
                    f.aviso(if (error == null) f.resources.getQuantityString(R.plurals.pers_turnos_creados, creados, creados)
                        else f.getString(R.string.pers_turnos_con_error, creados, error)).show()
                    despues()
                }
            }
            true
        }
    }

    /** Texto chico apagado para una línea dentro de una tarjeta. */
    fun linea(padre: ViewGroup, texto: String): TextView = Bloques.parrafo(padre, texto)
}
