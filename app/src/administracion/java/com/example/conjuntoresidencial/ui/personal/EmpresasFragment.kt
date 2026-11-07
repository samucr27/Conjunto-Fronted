package com.example.conjuntoresidencial.ui.personal

import android.content.Intent
import android.net.Uri
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.EmpresaDTO
import com.example.conjuntoresidencial.model.TrabajadorDTO
import com.example.conjuntoresidencial.ui.comun.Bloques
import com.example.conjuntoresidencial.ui.comun.TonoEstado
import com.example.conjuntoresidencial.ui.comun.confirmar
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * P11 bloque 8: las empresas que prestan personal (vigilancia, aseo...): sus datos, cuántos
 * trabajadores tiene en el conjunto, y crear, editar, activar o desactivar, llamar y eliminar.
 */
class EmpresasFragment : PantallaHoja() {
    override val titulo = R.string.pers_empresas
    override val descripcion = R.string.pers_empresas_sub

    private var empresas: List<EmpresaDTO> = emptyList()
    private var trabajadores: List<TrabajadorDTO> = emptyList()

    override suspend fun traer(): String? {
        val e = RetrofitClient.instance.getEmpresasPersonal()
        empresas = e.body().orEmpty()
        trabajadores = RetrofitClient.instance.getTrabajadores().body().orEmpty()
        return mensajeDe(e)
    }

    override fun pintar() {
        binding.encabezado.ponerInfo(resources.getQuantityString(R.plurals.pers_n_empresas, empresas.size, empresas.size))
        Bloques.boton(hoja, getString(R.string.pers_nueva_empresa), icono = R.drawable.ic_add) {
            Personal.formularioEmpresa(this, null) { recargar() }
        }
        if (empresas.isEmpty()) Bloques.parrafo(hoja, getString(R.string.pers_sin_empresas))
        empresas.sortedBy { it.nombre }.forEach { e ->
            val suyos = trabajadores.count { it.empresaId == e.id && it.activo != false }
            Bloques.tarjeta(hoja, e.nombre.orEmpty(),
                detalle = listOfNotNull(e.nit?.ifEmpty { null }?.let { getString(R.string.pers_nit_linea, it) },
                    e.contacto?.ifEmpty { null }, e.telefono?.ifEmpty { null }).joinToString(" · ").ifEmpty { null },
                extra = resources.getQuantityString(R.plurals.pers_trabajadores_en, suyos, suyos),
                etiqueta = if (e.activa == false) getString(R.string.pers_inactiva) to TonoEstado.NEUTRO else getString(R.string.pers_activa) to TonoEstado.EXITO) {
                opciones(e, suyos)
            }
        }
    }

    private fun opciones(e: EmpresaDTO, suyos: Int) {
        val ops = mutableListOf<Pair<Int, () -> Unit>>(
            R.string.pers_editar_empresa to { Personal.formularioEmpresa(this, e) { recargar() } },
            (if (e.activa == false) R.string.pers_activar else R.string.pers_desactivar) to {
                Personal.accion(this, R.string.pers_guardado,
                    { RetrofitClient.instance.actualizarEmpresaPersonal(e.id!!, e.copy(activa = e.activa == false)) }) { recargar() }
            }
        )
        if (!e.telefono.isNullOrBlank()) ops += R.string.pers_llamar to {
            runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${e.telefono}"))) }
        }
        ops += R.string.pers_eliminar to {
            confirmar(getString(R.string.pers_eliminar_empresa_titulo, e.nombre.orEmpty()),
                if (suyos > 0) getString(R.string.pers_eliminar_empresa_con, suyos) else null, R.string.pers_eliminar) {
                Personal.accion(this, R.string.pers_borrado, { RetrofitClient.instance.eliminarEmpresaPersonal(e.id!!) }) { recargar() }
            }
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle(e.nombre)
            .setItems(ops.map { getString(it.first) }.toTypedArray()) { _, i -> ops[i].second() }.show()
    }
}
