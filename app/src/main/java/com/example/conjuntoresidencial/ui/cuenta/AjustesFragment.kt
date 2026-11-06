package com.example.conjuntoresidencial.ui.cuenta

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.DialogConfirmarContrasenaBinding
import com.example.conjuntoresidencial.databinding.FragmentAjustesBinding
import com.example.conjuntoresidencial.push.AvisosCelular
import com.example.conjuntoresidencial.push.PreferenciasAlarma
import com.example.conjuntoresidencial.seguridad.AccesoBiometrico
import com.example.conjuntoresidencial.ui.auth.credenciales
import com.example.conjuntoresidencial.ui.auth.iniciarSesion
import com.example.conjuntoresidencial.ui.comun.aviso
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.util.SessionManager
import com.example.conjuntoresidencial.util.Tema
import com.example.conjuntoresidencial.util.limpiarErrorAlEscribir
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Cuenta → Ajustes: tema, avisos en este celular y entrar con huella o rostro. */
class AjustesFragment : Fragment() {
    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    // Botón del grupo de tema para cada modo
    private val botonesTema = mapOf(
        Tema.Modo.SISTEMA to R.id.btnTemaSistema,
        Tema.Modo.CLARO to R.id.btnTemaClaro,
        Tema.Modo.OSCURO to R.id.btnTemaOscuro
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEncabezado(binding.encabezado, R.string.cuenta_ajustes, R.string.ajustes_descripcion)
        configurarTema()
        configurarAvisos()
        configurarVisitas()
        binding.filaHuella.alCambiar = { activar -> if (activar) activarHuella() else desactivarHuella() }
    }

    override fun onResume() {
        super.onResume()
        // Puede cambiar fuera de la app (huellas registradas en el celular)
        mostrarHuella()
        if (binding.seccionVisitas.visibility == View.VISIBLE) mostrarVisitas()
    }

    private fun configurarTema() {
        // Con letra grande los tres botones no caben en una fila ("Sist…", "Osc…"): van en columna
        if (resources.configuration.fontScale >= 1.3f) {
            binding.grupoTema.orientation = LinearLayout.VERTICAL
            botonesTema.values.forEach { id ->
                binding.grupoTema.findViewById<View>(id).updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    weight = 0f
                }
            }
        }
        binding.grupoTema.check(botonesTema.getValue(Tema.actual(requireContext())))
        binding.grupoTema.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val modo = botonesTema.entries.first { it.value == id }.key
            // Cambiar el tema recrea la actividad; la sesión y la pantalla se conservan
            if (modo != Tema.actual(requireContext())) Tema.cambiar(requireContext(), modo)
        }
    }

    private fun configurarAvisos() {
        val contexto = requireContext()
        if (!AvisosCelular.disponible(contexto)) {
            // Sin Firebase configurado (docs/firebase.md) no llegan avisos
            binding.filaAvisos.isEnabled = false
            binding.filaAvisos.subtitulo = getString(R.string.ajustes_avisos_no_disponibles)
        }
        binding.filaAvisos.activado = AvisosCelular.disponible(contexto) && AvisosCelular.activados(contexto)
        binding.filaAvisos.alCambiar = { activar -> AvisosCelular.cambiarActivados(contexto, activar) }
        binding.filaAvisosSistema.setOnClickListener { abrirAjustesDeNotificaciones() }
    }

    // P10: el tono y si suena o vibra cuando llega una visita (solo residentes, que son quienes la reciben)
    private val elegirTono = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
        @Suppress("DEPRECATION")
        val uri = r.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        PreferenciasAlarma.ponerTono(requireContext(), uri)
        mostrarVisitas()
    }

    private fun configurarVisitas() {
        binding.seccionVisitas.visibility = if (com.example.conjuntoresidencial.BuildConfig.TIPO_APP == "RESIDENTE") View.VISIBLE else View.GONE
        if (binding.seccionVisitas.visibility != View.VISIBLE) return
        binding.filaVisitaModo.setOnClickListener {
            val modos = PreferenciasAlarma.Modo.values()
            val nombres = modos.map { getString(nombreModo(it)) }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ajustes_visitas_modo)
                .setSingleChoiceItems(nombres, modos.indexOf(PreferenciasAlarma.modo(requireContext()))) { d, i ->
                    PreferenciasAlarma.ponerModo(requireContext(), modos[i])
                    mostrarVisitas()
                    d.dismiss()
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
        }
        binding.filaVisitaTono.setOnClickListener {
            val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_RINGTONE)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.ajustes_visitas_tono))
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, PreferenciasAlarma.tono(requireContext()))
            try {
                elegirTono.launch(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                abrirAjustesDeNotificaciones()
            }
        }
        binding.filaPantallaCompleta.setOnClickListener {
            try {
                startActivity(PreferenciasAlarma.intentPantallaCompleta(requireContext()))
            } catch (_: android.content.ActivityNotFoundException) {
                abrirAjustesDeNotificaciones()
            }
        }
        mostrarVisitas()
    }

    private fun mostrarVisitas() {
        val c = requireContext()
        binding.filaVisitaModo.subtitulo = getString(nombreModo(PreferenciasAlarma.modo(c)))
        binding.filaVisitaTono.subtitulo = PreferenciasAlarma.nombreTono(c) ?: getString(R.string.ajustes_visitas_tono_defecto)
        binding.filaVisitaTono.isEnabled = PreferenciasAlarma.suena(c)
        binding.filaPantallaCompleta.subtitulo = getString(
            if (PreferenciasAlarma.puedePantallaCompleta(c)) R.string.ajustes_pantalla_completa_ok else R.string.ajustes_pantalla_completa_falta)
    }

    private fun nombreModo(m: PreferenciasAlarma.Modo) = when (m) {
        PreferenciasAlarma.Modo.SONIDO_Y_VIBRACION -> R.string.ajustes_modo_sonido_vibracion
        PreferenciasAlarma.Modo.SOLO_SONIDO -> R.string.ajustes_modo_sonido
        PreferenciasAlarma.Modo.SOLO_VIBRACION -> R.string.ajustes_modo_vibracion
    }

    private fun abrirAjustesDeNotificaciones() {
        val contexto = requireContext()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", contexto.packageName, null))
        }
        startActivity(intent)
    }

    private fun mostrarHuella() {
        val contexto = requireContext()
        val cuentaActual = SessionManager(contexto).credenciales("").cuenta(contexto)
        val guardada = AccesoBiometrico.cuentaGuardada(contexto)
        val disponible = AccesoBiometrico.modoDisponible(contexto) != null
        binding.filaHuella.activado = guardada == cuentaActual
        binding.filaHuella.isEnabled = disponible || guardada != null
        binding.filaHuella.subtitulo = when {
            !disponible && guardada == null -> getString(R.string.ajustes_huella_no_disponible)
            guardada != null && guardada != cuentaActual -> getString(R.string.ajustes_huella_otra_cuenta, guardada)
            else -> getString(R.string.ajustes_huella_sub)
        }
    }

    /** Pide la contraseña (se guarda cifrada), la comprueba con el servidor y luego la huella. */
    private fun activarHuella() {
        binding.filaHuella.activado = false
        val vista = DialogConfirmarContrasenaBinding.inflate(layoutInflater)
        vista.tilContrasenaConfirmar.limpiarErrorAlEscribir()
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ajustes_confirmar_titulo)
            .setMessage(R.string.ajustes_confirmar_mensaje)
            .setView(vista.root)
            .setPositiveButton(R.string.btn_confirmar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val contrasena = vista.etContrasenaConfirmar.text.toString()
            if (contrasena.isEmpty()) {
                vista.tilContrasenaConfirmar.error = getString(R.string.contrasena_falta_actual)
                return@setOnClickListener
            }
            val credenciales = SessionManager(requireContext()).credenciales(contrasena)
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val error = try {
                    val respuesta = iniciarSesion(credenciales)
                    // P7 bloque 5: ese login renueva la sesión de este celular: el token anterior deja de valer.
                    // Si pide código (celular aún sin verificar), la contraseña ya quedó confirmada
                    val nueva = respuesta.body()
                    if (respuesta.isSuccessful && nueva?.verificacionId == null && nueva?.token != null) {
                        SessionManager(requireContext()).guardar(nueva, credenciales.documento)
                    }
                    if (respuesta.isSuccessful) null
                    else RetrofitClient.mensajeDeError(respuesta, getString(R.string.contrasena_falta_actual))
                } catch (e: Exception) {
                    RetrofitClient.mensajeDeExcepcion(e)
                }
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                if (error != null) {
                    vista.tilContrasenaConfirmar.error = error
                    return@launch
                }
                dialogo.dismiss()
                AccesoBiometrico.activar(this@AjustesFragment, credenciales) { activada ->
                    if (_binding == null) return@activar
                    mostrarHuella()
                    if (activada) aviso(R.string.biometria_activada).show()
                }
            }
        }
    }

    private fun desactivarHuella() {
        AccesoBiometrico.desactivar(requireContext())
        mostrarHuella()
        aviso(R.string.biometria_desactivada).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
