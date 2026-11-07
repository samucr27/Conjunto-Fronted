package com.example.conjuntoresidencial.ui.personal

import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.EmpresaDTO
import com.example.conjuntoresidencial.model.NominaDTO
import com.example.conjuntoresidencial.model.TrabajadorDTO
import com.example.conjuntoresidencial.model.TurnoDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.ponerTitulo
import com.example.conjuntoresidencial.util.Formatos
import java.time.LocalDate
import java.time.Period

/**
 * P11 bloque 8: la ficha de un trabajador: sus datos y antigüedad, sus turnos de las próximas dos
 * semanas (con agregar turno) y su nómina mes a mes. Se edita, se activa o desactiva, y se borra
 * si se registró por error (sin nómina).
 */
class TrabajadorDetalleFragment : PantallaHoja() {
    override val titulo = R.string.pers_ficha
    override val descripcion = R.string.pers_ficha_sub

    private val id: Long get() = requireArguments().getLong(Personal.ARG_TRABAJADOR)
    private var trabajador: TrabajadorDTO? = null
    private var todos: List<TrabajadorDTO> = emptyList()
    private var empresas: List<EmpresaDTO> = emptyList()
    private var turnos: List<TurnoDTO> = emptyList()
    private var nomina: List<NominaDTO> = emptyList()

    override suspend fun traer(): String? {
        val t = RetrofitClient.instance.getTrabajadores()
        todos = t.body().orEmpty()
        trabajador = todos.firstOrNull { it.id == id }
        empresas = RetrofitClient.instance.getEmpresasPersonal().body().orEmpty()
        val hoy = LocalDate.now()
        turnos = RetrofitClient.instance.getTurnos(hoy.toString(), hoy.plusDays(14).toString()).body().orEmpty().filter { it.trabajadorId == id }
        nomina = RetrofitClient.instance.nominaDeTrabajador(id).body().orEmpty()
        return mensajeDe(t) ?: if (trabajador == null) getString(R.string.pers_no_existe) else null
    }

    override fun pintar() {
        val t = trabajador ?: return
        binding.encabezado.ponerTitulo(t.nombreCompleto.orEmpty())
        binding.encabezado.ponerInfo(Personal.cargo(this, t.cargo))
        val datos = Bloques.tarjeta(hoja, getString(R.string.pers_datos),
            etiqueta = if (t.activo == false) getString(R.string.pers_inactivo) to TonoEstado.NEUTRO else getString(R.string.pers_activo) to TonoEstado.EXITO)
        Bloques.dato(datos, getString(R.string.pers_documento), t.documento)
        Bloques.dato(datos, getString(R.string.pers_telefono), t.telefono?.ifEmpty { null })
        Bloques.dato(datos, getString(R.string.pers_cargo), Personal.cargo(this, t.cargo))
        Bloques.dato(datos, getString(R.string.pers_vinculo),
            if (t.vinculo == "EMPRESA") getString(R.string.pers_de_empresa, t.empresa.orEmpty()) else getString(R.string.pers_directo))
        Bloques.dato(datos, getString(R.string.pers_salario), Formatos.moneda(t.salarioBase ?: 0.0))
        t.fechaIngreso?.let { f ->
            val p = runCatching { Period.between(LocalDate.parse(f), LocalDate.now()) }.getOrNull()
            Bloques.dato(datos, getString(R.string.pers_ingreso),
                Formatos.fecha(f) + (p?.let { " · " + getString(R.string.pers_antiguedad, it.years, it.months) } ?: ""))
        }
        Bloques.boton(hoja, getString(R.string.pers_editar_trabajador), relleno = false, icono = R.drawable.ic_edit) {
            Personal.formularioTrabajador(this, t, empresas) { recargar() }
        }
        Bloques.boton(hoja, getString(if (t.activo == false) R.string.pers_activar else R.string.pers_desactivar), relleno = false) {
            Personal.accion(this, R.string.pers_guardado, { RetrofitClient.instance.actualizarTrabajador(id, TrabajadorDTO(activo = t.activo == false)) }) { recargar() }
        }

        Bloques.seccion(hoja, getString(R.string.pers_proximos_turnos))
        if (t.activo != false) Bloques.boton(hoja, getString(R.string.pers_nuevo_turno), icono = R.drawable.ic_add) {
            Personal.formularioTurno(this, todos, null, LocalDate.now(), id) { recargar() }
        }
        if (turnos.isEmpty()) Bloques.parrafo(hoja, getString(R.string.pers_sin_turnos))
        turnos.forEach { u ->
            Bloques.tarjeta(hoja, Personal.dia(LocalDate.parse(u.fecha)),
                detalle = getString(R.string.pers_horario, u.horaInicio?.take(5), u.horaFin?.take(5)))
        }

        Bloques.seccion(hoja, getString(R.string.pers_nomina_historial))
        if (nomina.isEmpty()) Bloques.parrafo(hoja, getString(R.string.pers_sin_nomina))
        nomina.forEach { n ->
            Bloques.tarjeta(hoja, Personal.mes(n.periodo), detalle = getString(R.string.pers_neto, Formatos.moneda(n.neto)),
                extra = getString(R.string.pers_desglose, Formatos.moneda(n.salarioBase), Formatos.moneda(n.bonificaciones), Formatos.moneda(n.descuentos)),
                etiqueta = if (n.estado == "PAGADA") getString(R.string.pers_pagada) to TonoEstado.EXITO else getString(R.string.pers_por_pagar) to TonoEstado.ADVERTENCIA)
        }

        if (nomina.isEmpty()) {
            Bloques.seccion(hoja, getString(R.string.pers_zona_peligro))
            Bloques.boton(hoja, getString(R.string.pers_borrar), relleno = false, icono = R.drawable.ic_delete) {
                confirmar(getString(R.string.pers_borrar_titulo, t.nombreCompleto.orEmpty()), getString(R.string.pers_borrar_mensaje), R.string.pers_borrar) {
                    Personal.accion(this, R.string.pers_borrado, { RetrofitClient.instance.eliminarTrabajador(id) }) { findNavController().navigateUp() }
                }
            }
        }
    }
}
