package com.example.conjuntoresidencial.ui.comun

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.conjuntoresidencial.BuildConfig
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ActivityChatVisitaBinding
import com.example.conjuntoresidencial.databinding.ItemMensajeBinding
import com.example.conjuntoresidencial.model.ChatVisitaDTO
import com.example.conjuntoresidencial.model.NuevoMensajeVisita
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * P10 bloque 7: chat temporal sobre una visita rechazada. Lo abren el residente (desde la alerta,
 * Inicio o el aviso) y la portería (desde la lista de visitas). Se actualiza cada 4 segundos
 * mientras está a la vista; pasados 30 minutos desde el rechazo queda solo para leer.
 */
class ChatVisitaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatVisitaBinding
    private var visitaId = -1L
    private var vistos = -1
    private var enviando = false
    private var abierto = true
    private val soyResidente = BuildConfig.TIPO_APP == "RESIDENTE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatVisitaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Íconos de la barra de estado oscuros sobre el fondo claro (claros en modo oscuro)
        val noche = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = !noche
        // Bordes del sistema y teclado: la fila para escribir siempre queda a la vista
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(top = barras.top, bottom = maxOf(barras.bottom, teclado.bottom))
            insets
        }
        visitaId = intent.getLongExtra(EXTRA_VISITA, -1)
        if (visitaId < 0) { finish(); return }
        binding.barra.setNavigationOnClickListener { finish() }
        binding.barra.title = getString(R.string.chat_visita_titulo)
        binding.etMensaje.doAfterTextChanged { binding.btnEnviar.isEnabled = abierto && !enviando && !it.isNullOrBlank() }
        binding.btnEnviar.setOnClickListener { enviar() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    cargar()
                    delay(INTERVALO_MS)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val otra = intent.getLongExtra(EXTRA_VISITA, -1)
        if (otra >= 0 && otra != visitaId) {
            visitaId = otra
            vistos = -1
            binding.listaMensajes.removeAllViews()
            lifecycleScope.launch { cargar() }
        }
    }

    private suspend fun cargar() {
        val id = visitaId
        try {
            val r = RetrofitClient.comun.chatVisita(id)
            val chat = r.body()
            if (id != visitaId) return
            if (r.isSuccessful && chat != null) pintar(chat)
            else if (vistos < 0) {
                binding.tvEstadoChat.text = RetrofitClient.mensajeDeError(r, getString(R.string.chat_visita_cerrado))
                cerrar()
            }
        } catch (_: Exception) {
            // Sin conexión: se intenta de nuevo en el siguiente ciclo
        }
    }

    private fun pintar(c: ChatVisitaDTO) {
        binding.barra.title = if (soyResidente) getString(R.string.chat_visita_titulo)
        else getString(R.string.chat_visita_titulo_porteria, c.torre, c.apto)
        binding.barra.subtitle = getString(if (soyResidente) R.string.chat_visita_sobre else R.string.chat_visita_sobre_porteria, c.visitante)
        abierto = c.abierto
        binding.tvEstadoChat.text = if (c.abierto) getString(R.string.chat_visita_cierra, c.cierraEn?.substringAfter('T')?.take(5).orEmpty())
        else getString(R.string.chat_visita_cerrado)
        if (!c.abierto) cerrar()
        binding.tvVacio.isVisible = c.mensajes.isEmpty()
        binding.tvVacio.setText(if (soyResidente) R.string.chat_visita_vacio else R.string.chat_visita_vacio_porteria)
        if (c.mensajes.size == vistos) return
        binding.listaMensajes.removeAllViews()
        c.mensajes.forEach { m ->
            val propio = m.deResidente == soyResidente
            val b = ItemMensajeBinding.inflate(layoutInflater, binding.listaMensajes, false)
            (b.burbuja.layoutParams as FrameLayout.LayoutParams).gravity = if (propio) Gravity.END else Gravity.START
            val fondo = if (propio) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHighest
            val texto = if (propio) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurface
            b.burbuja.setCardBackgroundColor(MaterialColors.getColor(b.burbuja, fondo))
            val color = MaterialColors.getColor(b.burbuja, texto)
            listOf(b.tvAutor, b.tvTexto, b.tvHora).forEach { it.setTextColor(color) }
            b.tvAutor.visibility = if (propio) View.GONE else View.VISIBLE
            b.tvAutor.text = m.autor
            b.tvTexto.text = m.texto
            b.tvHora.text = m.creadoEn.substringAfter('T').take(5)
            binding.listaMensajes.addView(b.root)
        }
        vistos = c.mensajes.size
        binding.desplazar.post { binding.desplazar.fullScroll(View.FOCUS_DOWN) }
    }

    private fun cerrar() {
        abierto = false
        binding.filaEscribir.isVisible = false
        binding.tvEstadoChat.setBackgroundColor(MaterialColors.getColor(binding.tvEstadoChat, com.google.android.material.R.attr.colorSurfaceContainerHighest))
    }

    private fun enviar() {
        val texto = binding.etMensaje.text?.toString()?.trim().orEmpty()
        if (texto.isEmpty() || enviando) return
        enviando = true
        binding.btnEnviar.isEnabled = false
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.comun.escribirChatVisita(visitaId, NuevoMensajeVisita(texto))
                if (r.isSuccessful) {
                    binding.etMensaje.setText("")
                    cargar()
                } else {
                    Snackbar.make(binding.raiz, RetrofitClient.mensajeDeError(r, getString(R.string.chat_visita_error)), Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.filaEscribir).show()
                    if (r.code() == 409) cargar()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.raiz, RetrofitClient.mensajeDeExcepcion(e), Snackbar.LENGTH_LONG).setAnchorView(binding.filaEscribir).show()
            } finally {
                enviando = false
                binding.btnEnviar.isEnabled = abierto && !binding.etMensaje.text.isNullOrBlank()
            }
        }
    }

    companion object {
        const val EXTRA_VISITA = "visitaId"
        private const val INTERVALO_MS = 4_000L

        fun intent(ctx: Context, visitaId: Long): Intent =
            Intent(ctx, ChatVisitaActivity::class.java).putExtra(EXTRA_VISITA, visitaId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun abrir(ctx: Context, visitaId: Long) = ctx.startActivity(intent(ctx, visitaId))
    }
}
