package com.example.conjuntoresidencial.ui.admin

import android.text.InputType
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.pedirTexto
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.FragmentEquipoBinding
import com.example.conjuntoresidencial.model.CuentaAdministracion
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.Resource
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.comoLista
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.Response

/** Cuentas de la administración (P4 bloque 7): administradores y contadores. Solo el administrador. */
class EquipoFragment : Fragment() {
    private var _binding: FragmentEquipoBinding? = null
    private val binding get() = _binding!!
    private val adaptador = CuentaAdapter { opciones(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEquipoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(
            binding.encabezado, R.string.menu_equipo, R.string.equipo_subtitulo, menu = R.menu.menu_lista,
            alElegir = accionesDeLista(binding.swipe) { cargar() }
        )
        binding.rvEquipo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipo.adapter = adaptador
        binding.estado.vincular(binding.rvEquipo)
        binding.swipe.setOnRefreshListener { cargar() }
        controlesFlotantes?.mostrarBotonFlotante(R.string.equipo_agregar, R.drawable.ic_add) { agregar() }
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = try {
                val r = RetrofitClient.instance.getCuentasAdministracion()
                if (r.isSuccessful) Resource.Success(r.body().orEmpty())
                else Resource.Error(RetrofitClient.mensajeDeError(r, getString(R.string.equipo_error_cargar)))
            } catch (e: Exception) {
                Resource.Error(RetrofitClient.mensajeDeExcepcion(e))
            }
            if (_binding == null) return@launch
            binding.estado.mostrarRecurso(resource, binding.swipe, R.drawable.ic_people, getString(R.string.equipo_vacio),
                reintentar = { cargar() }) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(R.plurals.resumen_cuentas, lista.size))
            }
        }
    }

    private fun agregar() {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cuenta_administracion, null)
        val roles = listOf(ROL_CONTADOR, ROL_ADMIN)
        val etiquetas = roles.map { getString(etiquetaRol(it)) }
        val rol = vista.findViewById<AutoCompleteTextView>(R.id.etRolCuenta).apply { comoLista(etiquetas) }
        val tilUsuario = vista.findViewById<TextInputLayout>(R.id.tilUsuarioCuenta)
        val tilContrasena = vista.findViewById<TextInputLayout>(R.id.tilContrasenaCuenta)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.equipo_agregar)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val usuario = vista.findViewById<TextInputEditText>(R.id.etUsuarioCuenta).text?.toString()?.trim().orEmpty()
            val contrasena = vista.findViewById<TextInputEditText>(R.id.etContrasenaCuenta).text?.toString().orEmpty()
            tilUsuario.error = if (usuario.length < 3) getString(R.string.equipo_usuario_corto) else null
            tilContrasena.error = if (contrasena.length < 6) getString(R.string.error_contrasena_corta) else null
            if (tilUsuario.error != null || tilContrasena.error != null) return@setOnClickListener
            dialogo.dismiss()
            val elegido = roles[etiquetas.indexOf(rol.text.toString()).coerceAtLeast(0)]
            ejecutar(getString(R.string.equipo_creada, usuario)) {
                RetrofitClient.instance.crearCuentaAdministracion(CuentaAdministracion(null, usuario, elegido, contrasena))
            }
        }
    }

    private fun opciones(c: CuentaAdministracion) {
        val otroRol = if (c.rol == ROL_CONTADOR) ROL_ADMIN else ROL_CONTADOR
        elegirOpcion(c.usuario, listOf(
            (if (otroRol == ROL_CONTADOR) R.string.equipo_hacer_contador else R.string.equipo_hacer_admin) to {
                ejecutar(getString(R.string.equipo_rol_cambiado, c.usuario, getString(etiquetaRol(otroRol)))) {
                    RetrofitClient.instance.updateCuentaAdministracion(c.id!!, c.copy(rol = otroRol))
                }
            },
            R.string.equipo_restablecer to {
                pedirTexto(getString(R.string.equipo_restablecer_titulo, c.usuario), R.string.login_contrasena, null,
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    validar = { if (it.length < 6) getString(R.string.error_contrasena_corta) else null }) { nueva ->
                    ejecutar(getString(R.string.contrasena_restablecida)) {
                        RetrofitClient.instance.updateCuentaAdministracion(c.id!!, c.copy(rol = c.rol, contrasena = nueva))
                    }
                }
            },
            R.string.residente_eliminar_si to { confirmarEliminar(c) }
        ))
    }

    private fun confirmarEliminar(c: CuentaAdministracion) {
        if (c.usuario == SessionManager(requireContext()).getUsuario()) {
            aviso(getString(R.string.equipo_no_propia), Snackbar.LENGTH_LONG).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.equipo_eliminar_titulo, c.usuario))
            .setMessage(R.string.equipo_eliminar_mensaje)
            .setPositiveButton(R.string.residente_eliminar_si) { _, _ ->
                ejecutar(getString(R.string.equipo_eliminada)) { RetrofitClient.instance.eliminarCuentaAdministracion(c.id!!) }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun ejecutar(exito: String, peticion: suspend () -> Response<*>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val mensaje = try {
                val r = peticion()
                if (r.isSuccessful) exito.also { cargar() }
                else RetrofitClient.mensajeDeError(r, getString(R.string.residente_error_accion))
            } catch (e: Exception) {
                RetrofitClient.mensajeDeExcepcion(e)
            }
            aviso(mensaje, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class CuentaAdapter(private val onTocar: (CuentaAdministracion) -> Unit) :
        ListAdapter<CuentaAdministracion, CuentaAdapter.VH>(DiffPorId { it.id }) {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val usuario: TextView = v.findViewById(android.R.id.text1)
            val rol: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val c = getItem(position)
            h.usuario.text = c.usuario
            h.rol.setText(etiquetaRol(c.rol))
            h.itemView.setOnClickListener { onTocar(c) }
        }
    }

    companion object {
        private const val ROL_ADMIN = "ADMIN"
        private const val ROL_CONTADOR = "CONTADOR"

        fun etiquetaRol(rol: String) = if (rol == ROL_CONTADOR) R.string.rol_contador else R.string.rol_admin
    }
}
