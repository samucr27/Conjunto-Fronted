package com.example.conjuntoresidencial.ui.admin

import com.example.conjuntoresidencial.ui.comun.BarraBusqueda
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.databinding.FragmentEquipoBinding
import com.example.conjuntoresidencial.model.CuentaAdministracion
import com.example.conjuntoresidencial.model.CupoEquipo
import com.example.conjuntoresidencial.ui.comun.DiffPorId
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.elegirOpcion
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.pedirTexto
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

/**
 * Mi equipo (solo el administrador principal, P5): asistentes (administran el conjunto pero no el
 * equipo) y contadores (solo la parte contable), con sus datos personales y un cupo por tipo.
 */
class EquipoFragment : Fragment() {
    private var _binding: FragmentEquipoBinding? = null
    private val binding get() = _binding!!
    private val adaptador = CuentaAdapter { opciones(it) }
    private var cuentas: List<CuentaAdministracion>? = null
    private lateinit var barra: BarraBusqueda

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEquipoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.menu_equipo, R.string.equipo_subtitulo, menu = R.menu.menu_equipo) { id ->
            when (id) {
                R.id.accion_actualizar -> { binding.swipe.isRefreshing = true; cargar(); true }
                R.id.accion_cupos -> { editarCupos(); true }
                else -> false
            }
        }
        binding.rvEquipo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipo.adapter = adaptador
        binding.estado.vincular(binding.rvEquipo)
        binding.swipe.setOnRefreshListener { cargar() }
        barra = BarraBusqueda(this, binding.busqueda, R.string.busqueda_equipo, listOf(
            R.string.busqueda_todos to null, R.string.busqueda_asistentes to ROL_ADMIN, R.string.busqueda_contadores to ROL_CONTADOR
        )) { cuentas?.let { mostrar(Resource.Success(it)) } }
        barra.vincular(binding.swipe, binding.rvEquipo)
        controlesFlotantes?.mostrarBotonFlotante(R.string.equipo_agregar, R.drawable.ic_add) { formulario(null) }
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
            if (resource is Resource.Success) cuentas = resource.data
            mostrar(resource)
        }
    }

    private fun mostrar(resource: Resource<List<CuentaAdministracion>>) {
        val filtrado = if (resource is Resource.Success) Resource.Success(resource.data.orEmpty().filter { c ->
            barra.coincide(c.nombreCompleto, c.usuario, c.documento, c.correo) && (barra.filtro == null || c.rol == barra.filtro)
        }) else resource
        binding.estado.mostrarRecurso(filtrado, binding.swipe, R.drawable.ic_people,
            if (barra.activa || barra.filtro != null) getString(R.string.busqueda_sin_resultados) else getString(R.string.equipo_vacio),
            reintentar = { cargar() }) { lista ->
            adaptador.submitList(lista)
            binding.encabezado.ponerInfo(resumen(R.plurals.resumen_cuentas, lista.size))
        }
    }

    /**
     * Nueva cuenta ([editando] null: pide todo) o editar los datos personales de una (sin usuario,
     * contraseña ni rol, que van por sus propias opciones).
     */
    private fun formulario(editando: CuentaAdministracion?) {
        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cuenta_administracion, null)
        fun campo(til: Int, et: Int) = vista.findViewById<TextInputLayout>(til) to vista.findViewById<TextInputEditText>(et)
        val nombre = campo(R.id.tilNombreCuenta, R.id.etNombreCuenta)
        val documento = campo(R.id.tilDocumentoCuenta, R.id.etDocumentoCuenta)
        val telefono = campo(R.id.tilTelefonoCuenta, R.id.etTelefonoCuenta)
        val correo = campo(R.id.tilCorreoCuenta, R.id.etCorreoCuenta)
        val usuario = campo(R.id.tilUsuarioCuenta, R.id.etUsuarioCuenta)
        val contrasena = campo(R.id.tilContrasenaCuenta, R.id.etContrasenaCuenta)
        val roles = listOf(ROL_ADMIN, ROL_CONTADOR)
        val etiquetas = roles.map { getString(etiquetaRol(it, false)) }
        val rol = vista.findViewById<AutoCompleteTextView>(R.id.etRolCuenta).apply { comoLista(etiquetas) }
        if (editando != null) {
            nombre.second.setText(editando.nombreCompleto)
            documento.second.setText(editando.documento)
            telefono.second.setText(editando.telefono)
            correo.second.setText(editando.correo)
            listOf(usuario.first, contrasena.first, vista.findViewById<View>(R.id.tilRolCuenta)).forEach { it.isVisible = false }
        }
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editando == null) getString(R.string.equipo_agregar) else editando.usuario)
            .setView(vista)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            fun texto(c: Pair<TextInputLayout, TextInputEditText>) = c.second.text?.toString()?.trim().orEmpty()
            val obligatorio = getString(R.string.equipo_dato_obligatorio)
            listOf(nombre, documento, telefono).forEach { it.first.error = if (texto(it).isEmpty()) obligatorio else null }
            correo.first.error = when {
                texto(correo).isEmpty() -> obligatorio
                !Patterns.EMAIL_ADDRESS.matcher(texto(correo)).matches() -> getString(R.string.equipo_correo_invalido)
                else -> null
            }
            if (editando == null) {
                usuario.first.error = if (texto(usuario).length < 3) getString(R.string.equipo_usuario_corto) else null
                contrasena.first.error =
                    if ((contrasena.second.text?.length ?: 0) < 6) getString(R.string.error_contrasena_corta) else null
            }
            if (listOf(nombre, documento, telefono, correo, usuario, contrasena).any { it.first.error != null }) {
                return@setOnClickListener
            }
            dialogo.dismiss()
            if (editando == null) {
                val elegido = roles[etiquetas.indexOf(rol.text.toString()).coerceAtLeast(0)]
                ejecutar(getString(R.string.equipo_creada, texto(usuario))) {
                    RetrofitClient.instance.crearCuentaAdministracion(CuentaAdministracion(null, texto(usuario), elegido,
                        contrasena.second.text.toString(), texto(nombre), texto(documento), texto(telefono), texto(correo)))
                }
            } else {
                ejecutar(getString(R.string.equipo_datos_guardados)) {
                    RetrofitClient.instance.updateCuentaAdministracion(editando.id!!, editando.copy(rol = editando.rol,
                        nombreCompleto = texto(nombre), documento = texto(documento), telefono = texto(telefono), correo = texto(correo)))
                }
            }
        }
    }

    private fun opciones(c: CuentaAdministracion) {
        val principal = c.principal == true
        val otroRol = if (c.rol == ROL_CONTADOR) ROL_ADMIN else ROL_CONTADOR
        elegirOpcion(c.nombreCompleto ?: c.usuario, listOfNotNull<Pair<Int, () -> Unit>>(
            R.string.equipo_editar_datos to { formulario(c) },
            ((if (otroRol == ROL_CONTADOR) R.string.equipo_hacer_contador else R.string.equipo_hacer_asistente) to {
                ejecutar(getString(R.string.equipo_rol_cambiado, c.usuario, getString(etiquetaRol(otroRol, false)))) {
                    RetrofitClient.instance.updateCuentaAdministracion(c.id!!, c.copy(rol = otroRol))
                }
            }).takeIf { !principal },
            R.string.equipo_restablecer to {
                pedirTexto(getString(R.string.equipo_restablecer_titulo, c.usuario), R.string.login_contrasena, null,
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    validar = { if (it.length < 6) getString(R.string.error_contrasena_corta) else null }) { nueva ->
                    ejecutar(getString(R.string.contrasena_restablecida)) {
                        RetrofitClient.instance.updateCuentaAdministracion(c.id!!, c.copy(contrasena = nueva))
                    }
                }
            },
            (R.string.residente_eliminar_si to { confirmarEliminar(c) }).takeIf { !principal }
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

    /** Cupo de asistentes y de contadores; vacío = sin límite. */
    private fun editarCupos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cupos = try {
                RetrofitClient.instance.getCuposEquipo().let { if (it.isSuccessful) it.body() else null }
            } catch (_: Exception) {
                null
            }
            if (cupos == null) {
                aviso(getString(R.string.equipo_error_cargar), Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val margen = (24 * resources.displayMetrics.density).toInt()
            val contenedor = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(margen, margen / 2, margen, 0)
            }
            contenedor.addView(TextView(requireContext()).apply {
                setText(R.string.equipo_cupos_ayuda)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            })
            val campos = listOf(ROL_ADMIN to R.string.equipo_cupo_asistentes, ROL_CONTADOR to R.string.equipo_cupo_contadores)
                .map { (rol, etiqueta) ->
                    val cupo = cupos.firstOrNull { it.rol == rol }
                    val et = TextInputEditText(requireContext()).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER
                        setText(cupo?.maximo?.toString().orEmpty())
                    }
                    contenedor.addView(TextInputLayout(requireContext()).apply {
                        hint = getString(etiqueta, cupo?.usados ?: 0L)
                        addView(et)
                        setPadding(0, margen / 2, 0, 0)
                    })
                    rol to et
                }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.equipo_cupos)
                .setView(contenedor)
                .setPositiveButton(R.string.btn_guardar) { _, _ ->
                    val nuevos = campos.map { (rol, et) -> CupoEquipo(rol, et.text?.toString()?.trim()?.toIntOrNull()) }
                    ejecutar(getString(R.string.equipo_cupos_guardados)) { RetrofitClient.instance.fijarCuposEquipo(nuevos) }
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        }
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
            val nombre: TextView = v.findViewById(android.R.id.text1)
            val detalle: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val c = getItem(position)
            val contexto = h.itemView.context
            h.nombre.text = c.nombreCompleto ?: c.usuario
            h.detalle.text = contexto.getString(R.string.equipo_fila_detalle,
                contexto.getString(etiquetaRol(c.rol, c.principal == true)), c.usuario)
            h.itemView.setOnClickListener { onTocar(c) }
        }
    }

    companion object {
        const val ROL_ADMIN = "ADMIN"
        const val ROL_CONTADOR = "CONTADOR"

        fun etiquetaRol(rol: String, principal: Boolean = false) = when {
            rol == ROL_CONTADOR -> R.string.rol_contador
            principal -> R.string.equipo_principal
            else -> R.string.rol_asistente
        }
    }
}
