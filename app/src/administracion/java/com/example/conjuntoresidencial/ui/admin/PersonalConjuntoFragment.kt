package com.example.conjuntoresidencial.ui.admin

import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.EmpresaDTO
import com.example.conjuntoresidencial.model.NominaDTO
import com.example.conjuntoresidencial.model.ResumenPresupuesto
import com.example.conjuntoresidencial.model.TrabajadorDTO
import com.example.conjuntoresidencial.model.TurnoDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.personal.PantallaHoja
import com.example.conjuntoresidencial.ui.personal.Personal
import com.example.conjuntoresidencial.util.Formatos
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Personal y presupuesto del conjunto (P6 bloque 7; P11 bloque 8 lo separa en pantallas): el
 * resumen del mes (trabajadores, turnos de la semana, nómina y presupuesto) y la entrada a cada
 * parte: Trabajadores, Empresas, Turnos, Nómina y Presupuesto.
 */
class PersonalConjuntoFragment : PantallaHoja() {
    override val titulo = R.string.personal_conj_titulo
    override val descripcion = R.string.personal_descripcion

    private val periodo = YearMonth.now().toString()
    private var trabajadores: List<TrabajadorDTO> = emptyList()
    private var empresas: List<EmpresaDTO> = emptyList()
    private var turnos: List<TurnoDTO> = emptyList()
    private var nomina: List<NominaDTO> = emptyList()
    private var resumen: ResumenPresupuesto? = null

    override suspend fun traer(): String? {
        val t = RetrofitClient.instance.getTrabajadores()
        trabajadores = t.body().orEmpty()
        empresas = RetrofitClient.instance.getEmpresasPersonal().body().orEmpty()
        val lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        turnos = RetrofitClient.instance.getTurnos(lunes.toString(), lunes.plusDays(6).toString()).body().orEmpty()
        nomina = RetrofitClient.instance.getNomina(periodo).body().orEmpty()
        resumen = RetrofitClient.instance.getResumenPresupuesto(periodo).body()
        return mensajeDe(t)
    }

    override fun pintar() {
        val activos = trabajadores.count { it.activo != false }
        binding.encabezado.ponerInfo(Personal.mes(periodo))
        Bloques.cifras(hoja, activos.toString() to getString(R.string.pers_activos_cifra),
            turnos.size.toString() to getString(R.string.pers_turnos_esta_semana))
        Bloques.cifras(hoja, Formatos.moneda(nomina.sumOf { it.neto }) to getString(R.string.pers_nomina_del_mes),
            Formatos.moneda(resumen?.totalPresupuestado ?: 0.0) to getString(R.string.pers_presupuestado))

        val nav = findNavController()
        Bloques.seccion(hoja, getString(R.string.pers_secciones))
        Bloques.tarjeta(hoja, getString(R.string.pers_trabajadores),
            detalle = resources.getQuantityString(R.plurals.pers_activos, activos, activos) +
                " · " + Personal.CARGOS.mapNotNull { (c, n) -> trabajadores.count { it.cargo == c && it.activo != false }.takeIf { it > 0 }?.let { "$it ${getString(n).lowercase()}" } }
                .joinToString(", ").ifEmpty { getString(R.string.pers_ninguno) }) {
            nav.navigate(R.id.trabajadoresFragment)
        }
        Bloques.tarjeta(hoja, getString(R.string.pers_empresas),
            detalle = resources.getQuantityString(R.plurals.pers_n_empresas, empresas.size, empresas.size)) {
            nav.navigate(R.id.empresasFragment)
        }
        Bloques.tarjeta(hoja, getString(R.string.pers_turnos),
            detalle = resources.getQuantityString(R.plurals.pers_turnos_semana, turnos.size, turnos.size),
            extra = if (activos > 0 && turnos.isEmpty()) getString(R.string.pers_sin_turnos_semana) else null) {
            nav.navigate(R.id.turnosFragment)
        }
        val porPagar = nomina.count { it.estado != "PAGADA" }
        Bloques.tarjeta(hoja, getString(R.string.pers_nomina),
            detalle = if (nomina.isEmpty()) getString(R.string.pers_sin_generar) else resources.getQuantityString(R.plurals.pers_por_pagar_n, porPagar, porPagar),
            etiqueta = when {
                nomina.isEmpty() -> getString(R.string.pers_sin_generar_corto) to TonoEstado.ADVERTENCIA
                porPagar == 0 -> getString(R.string.pers_pagada) to TonoEstado.EXITO
                else -> getString(R.string.pers_por_pagar) to TonoEstado.ADVERTENCIA
            }) {
            nav.navigate(R.id.nominaFragment)
        }
        val r = resumen
        Bloques.tarjeta(hoja, getString(R.string.pers_presupuesto),
            detalle = if (r == null || r.totalPresupuestado == 0.0) getString(R.string.pers_sin_presupuesto)
            else if (r.diferencia < 0) getString(R.string.pers_excedido, Formatos.moneda(-r.diferencia))
            else getString(R.string.pers_disponible, Formatos.moneda(r.diferencia)),
            etiqueta = if (r != null && r.totalPresupuestado > 0) (if (r.diferencia < 0) getString(R.string.pers_se_paso) to TonoEstado.ERROR
                else getString(R.string.pers_dentro) to TonoEstado.EXITO) else null) {
            nav.navigate(R.id.presupuestoFragment)
        }
    }
}
