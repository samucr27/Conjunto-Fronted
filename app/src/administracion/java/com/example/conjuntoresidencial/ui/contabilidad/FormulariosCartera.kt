package com.example.conjuntoresidencial.ui.contabilidad

import android.text.InputType
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.CuotaDTO
import com.example.conjuntoresidencial.model.EnvioSolicitud
import com.example.conjuntoresidencial.model.PropuestaSolicitud
import com.example.conjuntoresidencial.model.SolicitudContableDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** P11: textos de las solicitudes del contador (tipo, estado y su color). */
object TextosContables {
    fun tipo(f: Fragment, tipo: String): String = f.getString(when (tipo) {
        "FACTURACION" -> R.string.cont_tipo_facturacion
        "CUOTA_NUEVA" -> R.string.cont_tipo_cuota_nueva
        "CUOTA_CAMBIO" -> R.string.cont_tipo_cuota_cambio
        "CUOTA_ANULAR" -> R.string.cont_tipo_cuota_anular
        else -> R.string.cont_tipo_pago
    })

    fun estado(f: Fragment, s: SolicitudContableDTO): Pair<String, TonoEstado> = when (s.estado) {
        "APROBADA" -> f.getString(R.string.cont_estado_aprobada) to TonoEstado.EXITO
        "RECHAZADA" -> f.getString(R.string.cont_estado_devuelta) to TonoEstado.ERROR
        else -> f.getString(if (s.edicion) R.string.cont_estado_edicion else R.string.cont_estado_pendiente) to TonoEstado.ADVERTENCIA
    }

    fun accion(f: Fragment, accion: String): String = f.getString(when (accion) {
        "ENVIADA" -> R.string.cont_paso_enviada
        "CORREGIDA" -> R.string.cont_paso_corregida
        "EDITADA" -> R.string.cont_paso_editada
        "APROBADA" -> R.string.cont_paso_aprobada
        else -> R.string.cont_paso_devuelta
    })

    /** "Por conta · 7 de noviembre de 2026" */
    fun quien(f: Fragment, s: SolicitudContableDTO): String =
        f.getString(R.string.cont_por, s.solicitadaPor.orEmpty(), Formatos.fecha(s.actualizadaEn ?: s.creadaEn))
}

/**
 * P11: los formularios de lo que el contador pide sobre una cuota o un apartamento. Si
 * [solicitudId] no es null, es una corrección o edición de esa solicitud (vuelve a aprobación).
 */
object FormulariosCartera {

    const val ARG_APARTAMENTO = "apartamentoId"
    const val ARG_SOLICITUD = "solicitudId"
    const val ARG_ESTADO = "estado"

    /** Manda (o reenvía) y avisa; [despues] recarga la pantalla. */
    fun mandar(f: Fragment, tipo: String, detalle: Map<String, Any?>, nota: String?, solicitudId: Long?, despues: () -> Unit) {
        f.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = if (solicitudId == null) RetrofitClient.instance.enviarSolicitud(EnvioSolicitud(tipo, detalle, nota))
                else RetrofitClient.instance.proponerSolicitud(solicitudId, PropuestaSolicitud(detalle, nota))
                if (f.view == null) return@launch
                if (r.isSuccessful) {
                    f.aviso(if (r.body()?.estado == "APROBADA") R.string.cont_aplicado else R.string.cont_enviado).show()
                    despues()
                } else f.aviso(RetrofitClient.mensajeDeError(r, f.getString(R.string.cont_error_enviar))).show()
            } catch (e: Exception) {
                if (f.view != null) f.aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun caja(f: Fragment): LinearLayout = LinearLayout(f.requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        val d = f.resources.displayMetrics.density
        setPadding((24 * d).toInt(), (4 * d).toInt(), (24 * d).toInt(), 0)
    }

    private fun scroll(f: Fragment, contenido: LinearLayout) = android.widget.ScrollView(f.requireContext()).apply { addView(contenido) }

    private fun numero(v: Any?): String? = when (v) {
        null -> null
        is Number -> if (v.toDouble() % 1.0 == 0.0) v.toLong().toString() else v.toString()
        else -> v.toString()
    }

    private fun dinero(til: TextInputLayout, obligatorio: Boolean): Double? {
        val t = til.textoLimpio.replace(".", "").replace(",", ".").replace("$", "").trim()
        if (t.isEmpty()) {
            til.error = if (obligatorio) til.context.getString(R.string.cont_falta_valor) else null
            return null
        }
        val v = t.toDoubleOrNull()
        til.error = if (v == null || v <= 0) til.context.getString(R.string.cont_valor_invalido) else null
        return v?.takeIf { it > 0 }
    }

    private fun fechaValida(til: TextInputLayout, obligatoria: Boolean): String? {
        val t = til.textoLimpio
        if (t.isEmpty()) { til.error = if (obligatoria) til.context.getString(R.string.cont_falta_fecha) else null; return null }
        val ok = runCatching { LocalDate.parse(t) }.isSuccess
        til.error = if (ok) null else til.context.getString(R.string.cont_fecha_invalida)
        return t.takeIf { ok }
    }

    private fun mesValido(til: TextInputLayout): String? {
        val t = til.textoLimpio
        val ok = runCatching { YearMonth.parse(t) }.isSuccess
        til.error = if (ok) null else til.context.getString(R.string.cont_mes_invalido)
        return t.takeIf { ok }
    }

    /** Un campo de fecha que abre el calendario al tocarlo (también se puede escribir AAAA-MM-DD). */
    fun campoFecha(f: Fragment, padre: LinearLayout, pista: Int, valor: String?): TextInputLayout {
        val til = Bloques.campo(padre, f.getString(pista), InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE,
            f.getString(R.string.cont_fecha_formato), valor)
        til.setEndIconMode(TextInputLayout.END_ICON_CUSTOM)
        til.setEndIconDrawable(R.drawable.ic_event)
        til.setEndIconContentDescription(R.string.cont_elegir_fecha)
        til.setEndIconOnClickListener {
            val inicial = runCatching { LocalDate.parse(til.textoLimpio) }.getOrNull() ?: LocalDate.now()
            android.app.DatePickerDialog(f.requireContext(), { _, a, m, d ->
                til.editText?.setText(LocalDate.of(a, m + 1, d).toString())
            }, inicial.year, inicial.monthValue - 1, inicial.dayOfMonth).show()
        }
        return til
    }

    /** Cuota nueva (multa, parqueadero, extraordinaria...) para un apartamento. */
    fun cuotaNueva(f: Fragment, apartamentoId: Long, inicial: Map<String, Any?>? = null, solicitudId: Long? = null, despues: () -> Unit) {
        val c = caja(f)
        val siguiente = YearMonth.now()
        val concepto = Bloques.campo(c, f.getString(R.string.cont_concepto), ayuda = f.getString(R.string.cont_concepto_ayuda),
            valor = inicial?.get("concepto") as? String, maximo = 80)
        val mes = Bloques.campo(c, f.getString(R.string.cont_mes), InputType.TYPE_CLASS_TEXT, f.getString(R.string.cont_mes_formato),
            inicial?.get("periodo") as? String ?: siguiente.toString())
        val valor = Bloques.campo(c, f.getString(R.string.cont_valor), InputType.TYPE_CLASS_NUMBER, valor = numero(inicial?.get("valor")))
        val limite = campoFecha(f, c, R.string.cont_fecha_limite, inicial?.get("fechaLimite") as? String)
        val descripcion = Bloques.campo(c, f.getString(R.string.cont_descripcion), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            valor = inicial?.get("descripcion") as? String, maximo = 300)
        val nota = Bloques.campo(c, f.getString(R.string.cont_nota_admin), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            f.getString(R.string.cont_nota_ayuda), maximo = 500)
        dialogo(f, if (solicitudId == null) R.string.cont_nueva_cuota else R.string.cont_corregir, c) {
            val m = mesValido(mes)
            val v = dinero(valor, true)
            val fl = fechaValida(limite, false)
            if (concepto.textoLimpio.isEmpty()) concepto.error = f.getString(R.string.cont_falta_concepto) else concepto.error = null
            if (m == null || v == null || concepto.textoLimpio.isEmpty() || (limite.textoLimpio.isNotEmpty() && fl == null)) return@dialogo false
            mandar(f, "CUOTA_NUEVA", mapOf("apartamentoId" to apartamentoId, "periodo" to m, "concepto" to concepto.textoLimpio,
                "valor" to v, "fechaLimite" to fl, "descripcion" to descripcion.textoLimpio.ifEmpty { null }),
                nota.textoLimpio.ifEmpty { null }, solicitudId, despues)
            true
        }
    }

    /** Cambiar el valor, la fecha límite, el concepto o la descripción de una cuota sin pagar. */
    fun cambio(f: Fragment, cuota: CuotaDTO?, cuotaId: Long, inicial: Map<String, Any?>? = null, solicitudId: Long? = null, despues: () -> Unit) {
        val c = caja(f)
        cuota?.let { Bloques.parrafo(c, f.getString(R.string.cont_cuota_actual, it.concepto ?: "Administración",
            Formatos.moneda(it.valor), Formatos.fecha(it.fechaLimite))) }
        val valor = Bloques.campo(c, f.getString(R.string.cont_valor_nuevo), InputType.TYPE_CLASS_NUMBER,
            f.getString(R.string.cont_vacio_igual), numero(inicial?.get("valor")))
        val limite = campoFecha(f, c, R.string.cont_fecha_limite_nueva, inicial?.get("fechaLimite") as? String)
        val concepto = Bloques.campo(c, f.getString(R.string.cont_concepto_nuevo), ayuda = f.getString(R.string.cont_vacio_igual),
            valor = inicial?.get("concepto") as? String, maximo = 80)
        val descripcion = Bloques.campo(c, f.getString(R.string.cont_descripcion), valor = inicial?.get("descripcion") as? String, maximo = 300)
        val nota = Bloques.campo(c, f.getString(R.string.cont_nota_admin), ayuda = f.getString(R.string.cont_nota_ayuda), maximo = 500)
        dialogo(f, if (solicitudId == null) R.string.cont_modificar_cuota else R.string.cont_corregir, c) {
            val v = dinero(valor, false)
            val fl = fechaValida(limite, false)
            if ((valor.textoLimpio.isNotEmpty() && v == null) || (limite.textoLimpio.isNotEmpty() && fl == null)) return@dialogo false
            if (v == null && fl == null && concepto.textoLimpio.isEmpty() && descripcion.textoLimpio.isEmpty()) {
                valor.error = f.getString(R.string.cont_nada_que_cambiar); return@dialogo false
            }
            mandar(f, "CUOTA_CAMBIO", mapOf("cuotaId" to cuotaId, "valor" to v, "fechaLimite" to fl,
                "concepto" to concepto.textoLimpio.ifEmpty { null }, "descripcion" to descripcion.textoLimpio.ifEmpty { null }),
                nota.textoLimpio.ifEmpty { null }, solicitudId, despues)
            true
        }
    }

    /** Anular una cuota sin pagar (cobrada por error). El motivo es obligatorio. */
    fun anular(f: Fragment, cuota: CuotaDTO?, cuotaId: Long, inicial: Map<String, Any?>? = null, solicitudId: Long? = null, despues: () -> Unit) {
        val c = caja(f)
        cuota?.let { Bloques.parrafo(c, f.getString(R.string.cont_cuota_actual, it.concepto ?: "Administración",
            Formatos.moneda(it.valor), Formatos.fecha(it.fechaLimite))) }
        val motivo = Bloques.campo(c, f.getString(R.string.cont_motivo_anular), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            valor = inicial?.get("motivo") as? String, maximo = 300)
        dialogo(f, if (solicitudId == null) R.string.cont_anular_cuota else R.string.cont_corregir, c) {
            if (motivo.textoLimpio.isEmpty()) { motivo.error = f.getString(R.string.cont_falta_motivo); return@dialogo false }
            mandar(f, "CUOTA_ANULAR", mapOf("cuotaId" to cuotaId, "motivo" to motivo.textoLimpio), null, solicitudId, despues)
            true
        }
    }

    /** Pago hecho en la oficina: medio, fecha y referencia o soporte. */
    fun pago(f: Fragment, cuota: CuotaDTO?, cuotaId: Long, inicial: Map<String, Any?>? = null, solicitudId: Long? = null, despues: () -> Unit) {
        val c = caja(f)
        cuota?.let { Bloques.parrafo(c, f.getString(R.string.cont_cuota_actual, it.concepto ?: "Administración",
            Formatos.moneda(it.valor), Formatos.fecha(it.fechaLimite))) }
        val medios = listOf("EFECTIVO", "TRANSFERENCIA", "CONSIGNACION", "CHEQUE", "OTRO")
        val nombres = f.resources.getStringArray(R.array.cont_medios_pago).toList()
        val grupo = com.google.android.material.chip.ChipGroup(f.requireContext()).apply { isSingleSelection = true; isSelectionRequired = true }
        val elegido = (inicial?.get("medio") as? String) ?: "EFECTIVO"
        medios.forEachIndexed { i, m ->
            grupo.addView(com.google.android.material.chip.Chip(f.requireContext(), null,
                com.google.android.material.R.attr.chipStyle).apply { text = nombres[i]; tag = m; isCheckable = true; isChecked = m == elegido })
        }
        Bloques.parrafo(c, f.getString(R.string.cont_medio))
        c.addView(grupo)
        val fecha = campoFecha(f, c, R.string.cont_fecha_pago, inicial?.get("fechaPago") as? String ?: LocalDate.now().toString())
        val referencia = Bloques.campo(c, f.getString(R.string.cont_referencia), ayuda = f.getString(R.string.cont_referencia_ayuda),
            valor = inicial?.get("referencia") as? String, maximo = 80)
        val nota = Bloques.campo(c, f.getString(R.string.cont_nota_pago), valor = inicial?.get("nota") as? String, maximo = 300)
        dialogo(f, if (solicitudId == null) R.string.cont_registrar_pago else R.string.cont_corregir, c) {
            val fp = fechaValida(fecha, true) ?: return@dialogo false
            val medio = grupo.findViewById<com.google.android.material.chip.Chip>(grupo.checkedChipId)?.tag as? String ?: "EFECTIVO"
            mandar(f, "PAGO_MANUAL", mapOf("cuotaId" to cuotaId, "medio" to medio, "fechaPago" to fp,
                "referencia" to referencia.textoLimpio.ifEmpty { null }, "nota" to nota.textoLimpio.ifEmpty { null }),
                null, solicitudId, despues)
            true
        }
    }

    /** El diálogo no se cierra si [alAceptar] devuelve false (hay errores en los campos). */
    private fun dialogo(f: Fragment, titulo: Int, contenido: LinearLayout, alAceptar: () -> Boolean) {
        val d = MaterialAlertDialogBuilder(f.requireContext())
            .setTitle(titulo)
            .setView(scroll(f, contenido))
            .setPositiveButton(R.string.cont_enviar_aprobacion, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (alAceptar()) d.dismiss() }
    }

    /** Abre el formulario de una solicitud para corregirla o editarla (desde su detalle). */
    fun editar(f: Fragment, s: SolicitudContableDTO, detalle: Map<String, Any?>, despues: () -> Unit) {
        fun id(clave: String) = (detalle[clave] as? Number)?.toLong()
        when (s.tipo) {
            "FACTURACION" -> f.findNavController().navigate(R.id.facturacionContadorFragment, bundleOf(ARG_SOLICITUD to s.id))
            "CUOTA_NUEVA" -> cuotaNueva(f, id("apartamentoId") ?: return, detalle, s.id, despues)
            "CUOTA_CAMBIO" -> cambio(f, null, id("cuotaId") ?: return, detalle, s.id, despues)
            "CUOTA_ANULAR" -> anular(f, null, id("cuotaId") ?: return, detalle, s.id, despues)
            else -> pago(f, null, id("cuotaId") ?: return, detalle, s.id, despues)
        }
    }
}
