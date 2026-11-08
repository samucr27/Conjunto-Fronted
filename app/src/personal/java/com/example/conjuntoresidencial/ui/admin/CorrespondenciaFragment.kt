package com.example.conjuntoresidencial.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogoCorrespondenciaBinding
import com.example.conjuntoresidencial.databinding.FragmentCorrespondenciaBinding
import com.example.conjuntoresidencial.databinding.ItemCorrespondenciaBinding
import com.example.conjuntoresidencial.ui.comun.FilaDeslizable
import com.example.conjuntoresidencial.ui.comun.QuitarConDeshacer
import com.example.conjuntoresidencial.model.CorrespondenciaDTO
import com.example.conjuntoresidencial.model.EntregaCorrespondencia
import com.example.conjuntoresidencial.model.NuevaCorrespondencia
import com.example.conjuntoresidencial.model.ReciboParaTodos
import com.example.conjuntoresidencial.model.PedirEntrega
import com.example.conjuntoresidencial.model.RespuestaVisita
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.example.conjuntoresidencial.ui.comun.TIPOS_CORRESPONDENCIA
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.FiltroTorreApto
import com.example.conjuntoresidencial.ui.comun.nombreTipoCorrespondencia
import com.example.conjuntoresidencial.ui.comun.pintarCorrespondencia
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Response
import com.example.conjuntoresidencial.ui.comun.FotosPorteria
import com.example.conjuntoresidencial.ui.comun.TomadorFoto

/**
 * P9 bloque 3: la correspondencia en la portería (y en la administración). Se registra lo que
 * llega para un apartamento, o un recibo público para todos; al apartamento le llega el aviso.
 * Cuando lo recogen, Entregar pide el nombre de quien recoge.
 */
class CorrespondenciaFragment : Fragment() {

    private var _binding: FragmentCorrespondenciaBinding? = null
    private val binding get() = _binding!!

    private var todas: List<CorrespondenciaDTO> = emptyList()

    // P10 bloque 7: la foto del paquete del diálogo abierto
    private var foto: ByteArray? = null
    private var dialogoFoto: DialogoCorrespondenciaBinding? = null
    private val tomador = TomadorFoto(this) { datos ->
        foto = datos
        dialogoFoto?.let { mostrarFoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCorrespondenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.corr_titulo, R.string.corr_descripcion)
        binding.btnRegistrar.setOnClickListener { registrar() }
        binding.btnReciboTodos.setOnClickListener { reciboParaTodos() }
        binding.chipsEstado.setOnCheckedStateChangeListener { _, _ -> cargar() }
        filtro = FiltroTorreApto(binding.filtro) { _, _ -> pintar() }
        filtro.ponerPista(R.string.corr_buscar_apto)
        binding.swipe.setOnRefreshListener { cargar() }
        cargar()
        // P12 bloque 3: mientras algo espera la respuesta del apartamento, se mira cada 7 segundos
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(7_000)
                    if (!entregadas && todas.any { it.solicitud?.estado == "PENDIENTE" }) cargar()
                }
            }
        }
    }

    private val entregadas get() = binding.chipEntregadas.isChecked
    private lateinit var filtro: FiltroTorreApto
    // P11: lo entregado se quita de la lista deslizando de derecha a izquierda
    private val quitar = QuitarConDeshacer(this, { if (_binding != null) pintar() }) { RetrofitClient.comun.quitarCorrespondencia(it) }

    private fun cargar() {
        val estado = if (entregadas) "ENTREGADA" else "PENDIENTE"
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.correspondencia(estado, null, 7)
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    todas = r.body().orEmpty()
                    filtro.ponerTorres(todas.map { it.torre })
                    pintar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.corr_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
            _binding?.swipe?.isRefreshing = false
        }
    }

    /** P10: filtra lo cargado por torre y después por apartamento ("50" = 501, 502...), remitente o quién recogió. */
    private fun pintar() {
        val q = filtro.texto
        val torre = filtro.torre
        val lista = todas.filter { c ->
            !quitar.escondida(c.id) && (torre == null || c.torre == torre) && (q.isEmpty() || FiltroTorreApto.coincideApto(c.apto, q) ||
                FiltroTorreApto.normalizar("${c.torre}-${c.apto}").contains(q) ||
                FiltroTorreApto.normalizar(c.remitente.orEmpty()).contains(q) ||
                FiltroTorreApto.normalizar(c.entregadaA.orEmpty()).contains(q))
        }
        if (!entregadas) binding.chipPendientes.text = getString(R.string.corr_por_entregar_n, todas.size)
        binding.tvVacio.isVisible = lista.isEmpty()
        binding.tvVacio.setText(if (entregadas) R.string.corr_vacio_entregadas else R.string.corr_vacio_pendientes)
        binding.lista.removeAllViews()
        lista.forEach { c ->
            val fila = ItemCorrespondenciaBinding.inflate(layoutInflater, binding.lista, false)
            pintarCorrespondencia(fila, c, conApartamento = true, alEntregar = { entregar(it) })
            binding.lista.addView(if (c.estado == "ENTREGADA") FilaDeslizable.envolver(fila.root, binding.swipe) { quitar.quitar(c.id) }
                else fila.root)
        }
    }

    private fun registrar() {
        val d = DialogoCorrespondenciaBinding.inflate(layoutInflater)
        TIPOS_CORRESPONDENCIA.forEachIndexed { i, (codigo, nombre) ->
            d.chipsTipo.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                id = View.generateViewId()
                tag = codigo
                setText(nombre)
                isCheckable = true
                isChecked = i == 0
            })
        }
        foto = null
        fotoDeQuienRecoge = false
        dialogoFoto = d
        d.filaFoto.isVisible = true
        d.btnFoto.setOnClickListener { tomador.tomar() }
        d.imgFoto.setOnClickListener { foto?.let { FotosPorteria.verGrande(requireContext(), it, getString(R.string.foto_del_paquete)) } ?: tomador.tomar() }
        mostrarFoto(d)
        dialogo(R.string.corr_nueva_titulo, d, R.string.corr_guardar) { cerrar ->
            val torre = d.etTorre.text?.toString()?.trim().orEmpty()
            val apto = d.etApto.text?.toString()?.trim().orEmpty()
            val falta = torre.isEmpty() || apto.isEmpty()
            d.tilApto.error = if (falta) getString(R.string.corr_falta_apto) else null
            if (falta) return@dialogo
            val tipo = d.chipsTipo.findViewById<Chip>(d.chipsTipo.checkedChipId)?.tag as? String ?: "PAQUETE"
            val datos = foto
            viewLifecycleOwner.lifecycleScope.launch {
                // P10 bloque 7: primero la foto (si se tomó); si falla, no se registra para no perderla
                var fotoId: String? = null
                if (datos != null) {
                    val (id, error) = FotosPorteria.subir(datos, getString(R.string.foto_error))
                    if (_binding == null) return@launch
                    if (id == null) { aviso(error ?: getString(R.string.foto_error)).show(); return@launch }
                    fotoId = id
                }
                enviar({ RetrofitClient.comun.registrarCorrespondencia(
                    NuevaCorrespondencia(torre, apto, tipo, texto(d.etCampo1.text), texto(d.etCampo2.text), fotoId)) }) {
                    foto = null
                    dialogoFoto = null
                    cerrar()
                    aviso(getString(R.string.corr_registrada_ok, torre, apto)).show()
                }
            }
        }
    }

    private fun reciboParaTodos() {
        val d = DialogoCorrespondenciaBinding.inflate(layoutInflater)
        d.chipsTipo.isVisible = false
        d.filaApartamento.isVisible = false
        d.tvMensaje.isVisible = true
        d.tvMensaje.setText(R.string.corr_todos_mensaje)
        d.tilCampo1.setHint(R.string.corr_empresa)
        d.tilCampo2.setHint(R.string.corr_mes)
        dialogo(R.string.corr_todos_titulo, d, R.string.corr_avisar_todos) { cerrar ->
            val empresa = texto(d.etCampo1.text)
            d.tilCampo1.error = if (empresa == null) getString(R.string.corr_falta_empresa) else null
            if (empresa == null) return@dialogo
            enviar({ RetrofitClient.comun.reciboParaTodos(ReciboParaTodos(empresa, texto(d.etCampo2.text))) }) { lote ->
                cerrar()
                aviso(getString(R.string.corr_todos_ok, lote?.creadas ?: 0)).show()
            }
        }
    }

    /**
     * P12 bloque 3: entregar va en pasos, como una visita: se pide la autorización al apartamento
     * (quién recoge, documento y foto); mientras espera se puede registrar lo que dijo por
     * teléfono; ya autorizada, se entrega.
     */
    private fun entregar(c: CorrespondenciaDTO) {
        when (c.solicitud?.estado) {
            "PENDIENTE" -> respuestaPorLlamada(c)
            "AUTORIZADA" -> confirmarEntrega(c)
            else -> pedirAutorizacion(c)
        }
    }

    private fun pedirAutorizacion(c: CorrespondenciaDTO) {
        val d = DialogoCorrespondenciaBinding.inflate(layoutInflater)
        d.chipsTipo.isVisible = false
        d.filaApartamento.isVisible = false
        d.tvMensaje.isVisible = true
        d.tvMensaje.text = getString(R.string.corr_pedir_mensaje,
            listOfNotNull(getString(nombreTipoCorrespondencia(c.tipo)), c.remitente).joinToString(" · "), c.torre, c.apto)
        d.tilCampo1.setHint(R.string.corr_nombre_recoge)
        d.tilCampo2.setHint(R.string.corr_documento_recoge)
        foto = null
        fotoDeQuienRecoge = true
        dialogoFoto = d
        d.filaFoto.isVisible = true
        d.btnFoto.setOnClickListener { tomador.tomar() }
        d.imgFoto.setOnClickListener { foto?.let { FotosPorteria.verGrande(requireContext(), it, getString(R.string.foto_de_quien_recoge)) } ?: tomador.tomar() }
        mostrarFoto(d)
        dialogo(R.string.corr_pedir_titulo, d, R.string.corr_pedir) { cerrar ->
            val nombre = texto(d.etCampo1.text)
            d.tilCampo1.error = if (nombre == null) getString(R.string.corr_falta_nombre) else null
            if (nombre == null) return@dialogo
            val datos = foto
            viewLifecycleOwner.lifecycleScope.launch {
                var fotoId: String? = null
                if (datos != null) {
                    val (id, error) = FotosPorteria.subir(datos, getString(R.string.foto_error))
                    if (_binding == null) return@launch
                    if (id == null) { aviso(error ?: getString(R.string.foto_error)).show(); return@launch }
                    fotoId = id
                }
                enviar({ RetrofitClient.comun.pedirEntrega(c.id, PedirEntrega(nombre, texto(d.etCampo2.text), fotoId)) }) {
                    foto = null
                    dialogoFoto = null
                    cerrar()
                    aviso(R.string.corr_pedida_ok).show()
                }
            }
        }
    }

    /** Si el apartamento contestó por teléfono, la portería registra lo que dijo. */
    private fun respuestaPorLlamada(c: CorrespondenciaDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.corr_llamada_titulo)
            .setMessage(getString(R.string.corr_llamada_mensaje, c.solicitud?.recoge.orEmpty()))
            .setPositiveButton(R.string.corr_llamada_autorizo) { _, _ ->
                enviar({ RetrofitClient.comun.responderEntrega(c.id, RespuestaVisita("AUTORIZAR")) }) {}
            }
            .setNegativeButton(R.string.corr_llamada_rechazo) { _, _ ->
                enviar({ RetrofitClient.comun.responderEntrega(c.id, RespuestaVisita("RECHAZAR")) }) {}
            }
            .setNeutralButton(R.string.cancelar_corr, null)
            .show()
    }

    private fun confirmarEntrega(c: CorrespondenciaDTO) {
        val s = c.solicitud ?: return
        val canal = getString(if (s.canal == "LLAMADA") R.string.entrega_por_llamada else R.string.entrega_por_app)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.corr_entregar_confirmar, s.recoge.orEmpty()))
            .setMessage(getString(R.string.corr_entregar_confirmar_detalle, canal))
            .setPositiveButton(R.string.corr_entregar) { _, _ ->
                enviar({ RetrofitClient.comun.entregarCorrespondencia(c.id, EntregaCorrespondencia()) }) {
                    aviso(getString(R.string.corr_entregada_ok, s.recoge.orEmpty())).show()
                }
            }
            .setNegativeButton(R.string.cancelar_corr, null)
            .show()
    }

    // P12 bloque 3: si la foto del diálogo es de quien recoge (no del paquete)
    private var fotoDeQuienRecoge = false

    private fun mostrarFoto(d: DialogoCorrespondenciaBinding) {
        val datos = foto
        d.btnFoto.setText(if (datos != null) R.string.foto_otra else if (fotoDeQuienRecoge) R.string.foto_tomar_quien_recoge else R.string.foto_tomar_paquete)
        d.tvAyudaFoto.setText(if (datos != null) R.string.foto_lista else if (fotoDeQuienRecoge) R.string.foto_ayuda_quien_recoge else R.string.foto_ayuda_paquete)
        if (datos == null) {
            val p = (16 * resources.displayMetrics.density).toInt()
            d.imgFoto.setPadding(p, p, p, p)
            d.imgFoto.setImageResource(R.drawable.ic_camara)
        } else {
            d.imgFoto.setPadding(0, 0, 0, 0)
            d.imgFoto.imageTintList = null
            d.imgFoto.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(datos, 0, datos.size))
        }
    }

    /** Un diálogo que no se cierra solo al tocar el botón: [alAceptar] valida y lo cierra si sale bien. */
    private fun dialogo(titulo: Int, d: DialogoCorrespondenciaBinding, boton: Int, alAceptar: (cerrar: () -> Unit) -> Unit) {
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setView(d.root)
            .setPositiveButton(boton, null)
            .setNegativeButton(R.string.cancelar_corr, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            alAceptar { dialogo.dismiss() }
        }
    }

    private fun <T> enviar(llamada: suspend () -> Response<T>, alTerminar: (T?) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val r = llamada()
                if (_binding == null) return@launch
                if (r.isSuccessful) {
                    alTerminar(r.body())
                    cargar()
                } else {
                    aviso(RetrofitClient.mensajeDeError(r, getString(R.string.corr_error))).show()
                }
            } catch (e: Exception) {
                if (_binding != null) aviso(RetrofitClient.mensajeDeExcepcion(e)).show()
            }
        }
    }

    private fun texto(e: CharSequence?): String? = e?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
