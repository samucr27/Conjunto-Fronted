package com.example.conjuntoresidencial.ui.personal

import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.LineaPresupuesto
import com.example.conjuntoresidencial.model.ResumenPresupuesto
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.Bloques.textoLimpio
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.util.Formatos
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * P11 bloque 8: el presupuesto de personal de un mes, por cargo, frente a lo que va en nómina:
 * cada cargo con su monto (se edita aquí), la diferencia y si se pasó. Se puede copiar el del mes
 * anterior.
 */
class PresupuestoFragment : PantallaHoja() {
    override val titulo = R.string.pers_presupuesto
    override val descripcion = R.string.pers_presupuesto_sub

    private var periodo: String = YearMonth.now().toString()
    private var lineas: List<LineaPresupuesto> = emptyList()
    private var resumen: ResumenPresupuesto? = null
    private val campos = mutableMapOf<String, TextInputLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(PERIODO)?.let { periodo = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PERIODO, periodo)
    }

    override suspend fun traer(): String? {
        val r = RetrofitClient.instance.getPresupuesto(periodo)
        lineas = r.body().orEmpty()
        resumen = RetrofitClient.instance.getResumenPresupuesto(periodo).body()
        return mensajeDe(r)
    }

    override fun pintar() {
        val res = resumen
        binding.encabezado.ponerInfo(if (res == null) null else if (res.diferencia < 0)
            getString(R.string.pers_excedido, Formatos.moneda(-res.diferencia)) else getString(R.string.pers_disponible, Formatos.moneda(res.diferencia)))
        Personal.selector(hoja, Personal.mes(periodo), getString(R.string.pers_mes_anterior), getString(R.string.pers_mes_siguiente)) { paso ->
            periodo = YearMonth.parse(periodo).plusMonths(paso.toLong()).toString(); recargar()
        }
        res?.let {
            Bloques.cifras(hoja, Formatos.moneda(it.totalPresupuestado) to getString(R.string.pers_presupuestado),
                Formatos.moneda(it.totalNomina) to getString(R.string.pers_en_nomina))
        }
        Bloques.seccion(hoja, getString(R.string.pers_por_cargo))
        campos.clear()
        Personal.CARGOS.forEach { (codigo, nombre) ->
            val monto = lineas.firstOrNull { it.cargo == codigo }?.monto
            val enNomina = res?.lineas?.firstOrNull { it.cargo == codigo }?.nomina ?: 0.0
            val tarjeta = Bloques.tarjeta(hoja, getString(nombre), detalle = getString(R.string.pers_en_nomina_linea, Formatos.moneda(enNomina)),
                etiqueta = when {
                    monto == null || monto == 0.0 -> null
                    enNomina > monto -> getString(R.string.pers_se_paso) to TonoEstado.ERROR
                    else -> getString(R.string.pers_dentro) to TonoEstado.EXITO
                })
            campos[codigo] = Bloques.campo(tarjeta, getString(R.string.pers_monto), InputType.TYPE_CLASS_NUMBER, valor = monto?.toLong()?.toString())
        }
        Bloques.boton(hoja, getString(R.string.btn_guardar), icono = R.drawable.ic_check_circle) { guardar() }
        Bloques.boton(hoja, getString(R.string.pers_copiar_mes_anterior), relleno = false) { copiarAnterior() }
    }

    private fun guardar() {
        val nuevas = mutableListOf<LineaPresupuesto>()
        var error = false
        campos.forEach { (cargo, til) ->
            val t = til.textoLimpio.replace(".", "").replace(",", "")
            if (t.isEmpty()) { til.error = null; return@forEach }
            val v = t.toDoubleOrNull()
            til.error = if (v == null || v < 0) getString(R.string.pers_valor_invalido).also { error = true } else null
            if (v != null && v >= 0) nuevas += LineaPresupuesto(cargo, v)
        }
        if (error) return
        Personal.accion(this, R.string.pers_guardado, { RetrofitClient.instance.fijarPresupuesto(periodo, nuevas) }) { recargar() }
    }

    /** Llena los campos con los montos del mes anterior (falta guardar). */
    private fun copiarAnterior() {
        val anterior = YearMonth.parse(periodo).minusMonths(1).toString()
        viewLifecycleOwner.lifecycleScope.launch {
            val previas = try { RetrofitClient.instance.getPresupuesto(anterior).body().orEmpty() } catch (_: Exception) { emptyList() }
            if (!vivo) return@launch
            if (previas.isEmpty()) { aviso(R.string.pers_sin_presupuesto_anterior).show(); return@launch }
            previas.forEach { l -> campos[l.cargo]?.editText?.setText(l.monto.toLong().toString()) }
            aviso(R.string.pers_copiado_revisa).show()
        }
    }

    companion object { private const val PERIODO = "periodo" }
}
