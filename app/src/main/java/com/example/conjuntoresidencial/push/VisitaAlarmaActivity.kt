package com.example.conjuntoresidencial.push

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.conjuntoresidencial.MainActivity
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.databinding.ActivityVisitaAlarmaBinding
import com.example.conjuntoresidencial.ui.comun.DestinoNotificacion
import kotlinx.coroutines.launch
import coil.load

/**
 * P8 bloque 3: la visita en portería a pantalla completa, encima de la pantalla de bloqueo.
 * Muestra quién llegó (con documento, placa y motivo si la portería los anotó) y cuánto falta;
 * Autorizar o Rechazar contesta y calla la alarma. Sin sesión en este celular ofrece abrir la
 * app para iniciarla. Se cierra sola al pasar [AlarmaVisita.DURACION_MS] (la portería espera 2
 * minutos; el resto lo puede contestar en Inicio o por llamada).
 */
class VisitaAlarmaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisitaAlarmaBinding
    private var visitaId = -1L
    // P12 bloque 3: la misma pantalla para autorizar la entrega de la correspondencia (visitaId es su id)
    private var entrega = false
    private var cuenta: CountDownTimer? = null
    private var pulso: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        encimaDelBloqueo()
        binding = ActivityVisitaAlarmaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mostrar(intent)

        binding.btnAutorizar.setOnClickListener { responder(AlarmaVisita.AUTORIZAR) }
        binding.btnRechazar.setOnClickListener { responder(AlarmaVisita.RECHAZAR) }
        binding.btnAbrirApp.setOnClickListener { abrirApp() }
        // P10 bloque 7: tras rechazar, el residente puede escribirle a la portería (ya no se llama)
        binding.btnChatPorteria.setOnClickListener {
            com.example.conjuntoresidencial.ui.comun.ChatVisitaActivity.abrir(this, visitaId, entrega)
            finish()
        }
        binding.imgVisitante.setOnClickListener {
            com.example.conjuntoresidencial.ui.comun.FotosPorteria.verGrande(this, fotoUrl, binding.tvTitulo.text)
        }
        com.example.conjuntoresidencial.util.ConjuntoElegido.actual(this)?.let {
            binding.tvConjunto.text = it.nombre
            binding.tvConjunto.isVisible = true
        }
        // "Atrás" solo cierra la pantalla: la alarma sigue en el aviso hasta responder o vencer
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })

        pulso = ObjectAnimator.ofPropertyValuesHolder(binding.pulso,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1.15f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1.15f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
        ).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mostrar(intent)
    }

    private fun encimaDelBloqueo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun mostrar(intent: Intent) {
        visitaId = intent.getLongExtra(AlarmaVisita.EXTRA_VISITA, -1)
        entrega = intent.getBooleanExtra(AlarmaVisita.EXTRA_ENTREGA, false)
        if (visitaId < 0) { finish(); return }
        binding.tvTitulo.text = intent.getStringExtra(AlarmaVisita.EXTRA_TITULO) ?: getString(R.string.alarma_titulo_generico)
        binding.tvDetalle.text = intent.getStringExtra(AlarmaVisita.EXTRA_MENSAJE).orEmpty()
        binding.tvEstado.isVisible = false
        botones(true)
        binding.btnChatPorteria.isVisible = false
        binding.grupoBotones.isVisible = true
        mostrarFoto(null)
        empezarCuenta(AlarmaVisita.DURACION_MS)
        cargarDetalle()
    }

    private var fotoUrl: String? = null

    /** P10 bloque 7: la cara del visitante en el centro del anillo; sin foto, el ícono de persona. */
    private fun mostrarFoto(url: String?) {
        fotoUrl = url
        val img = binding.imgVisitante
        binding.tvAmpliarFoto.isVisible = url != null
        img.isClickable = url != null
        if (url == null) {
            img.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            val p = (80 * resources.displayMetrics.density).toInt()
            img.setPadding(p, p, p, p)
            img.setImageResource(R.drawable.ic_people)
            return
        }
        img.imageTintList = null
        img.setPadding(0, 0, 0, 0)
        img.load(url) {
            crossfade(true)
            listener(onError = { _, _ -> if (fotoUrl == url) mostrarFoto(null) })
        }
    }

    /** Con sesión, trae la visita para mostrar el nombre, el documento, la placa y lo que falta. */
    private fun cargarDetalle() {
        val id = visitaId
        if (entrega) { cargarEntrega(id); return }
        lifecycleScope.launch {
            val v = try {
                RetrofitClient.comun.getVisitasPendientes().body()?.firstOrNull { it.id == id }
            } catch (_: Exception) {
                null
            } ?: return@launch
            if (id != visitaId) return@launch
            binding.tvTitulo.text = getString(R.string.porteria_residente_titulo, v.visitante)
            if (v.fotoUrl != null) mostrarFoto(v.fotoUrl)
            binding.tvDetalle.text = listOfNotNull(
                v.documento?.takeIf { it.isNotBlank() }?.let { getString(R.string.porteria_doc_linea, it) },
                v.placa?.takeIf { it.isNotBlank() }?.let { getString(R.string.alarma_placa, it) },
                v.motivo?.takeIf { it.isNotBlank() }
            ).joinToString("\n").ifBlank { getString(R.string.porteria_residente_mensaje) }
        }
    }

    /** P12 bloque 3: quién quiere recoger qué (con su foto y documento, si la portería los tomó). */
    private fun cargarEntrega(id: Long) {
        lifecycleScope.launch {
            val c = try {
                RetrofitClient.comun.solicitudesEntrega().body()?.firstOrNull { it.id == id }
            } catch (_: Exception) {
                null
            } ?: return@launch
            if (id != visitaId) return@launch
            val s = c.solicitud ?: return@launch
            binding.tvTitulo.text = getString(R.string.entrega_alarma_titulo, s.recoge.orEmpty())
            if (s.fotoUrl != null) mostrarFoto(s.fotoUrl)
            val que = getString(when (c.tipo) {
                "CARTA" -> R.string.corr_tipo_carta
                "DOCUMENTO" -> R.string.corr_tipo_documento
                "RECIBO" -> R.string.corr_tipo_recibo
                else -> R.string.corr_tipo_paquete
            }) + (c.remitente?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
            binding.tvDetalle.text = listOfNotNull(
                que,
                s.documento?.takeIf { it.isNotBlank() }?.let { getString(R.string.porteria_doc_linea, it) },
                c.nota?.takeIf { it.isNotBlank() }
            ).joinToString("\n")
        }
    }

    private fun empezarCuenta(ms: Long) {
        cuenta?.cancel()
        cuenta = object : CountDownTimer(ms, 1000) {
            override fun onTick(restante: Long) {
                val s = (restante / 1000).toInt()
                binding.tvTiempo.text = getString(R.string.alarma_tiempo, s / 60, s % 60)
                binding.anilloTiempo.setProgressCompat((restante * 1000 / ms).toInt(), true)
            }
            override fun onFinish() {
                AlarmaVisita.detener(this@VisitaAlarmaActivity, visitaId, entrega)
                finish()
            }
        }.start()
    }

    private fun responder(decision: String) {
        botones(false)
        AlarmaVisita.responder(this, visitaId, decision, entrega) { ok, texto ->
            if (isFinishing || isDestroyed) return@responder
            binding.tvEstado.text = texto
            binding.tvEstado.isVisible = true
            if (ok && decision == AlarmaVisita.RECHAZAR) {
                // P10 bloque 7: rechazada: se ofrece el chat con la portería en vez de cerrarse sola
                cuenta?.cancel()
                binding.tvEstado.text = getString(if (entrega) R.string.entrega_rechazada_chat else R.string.chat_visita_rechazada)
                binding.grupoBotones.isVisible = false
                binding.btnChatPorteria.isVisible = true
                binding.tvTiempo.isVisible = false
            } else if (ok) {
                cuenta?.cancel()
                binding.root.postDelayed({ finish() }, 1500)
            } else {
                botones(true)
            }
        }
    }

    private fun botones(activos: Boolean) {
        binding.btnAutorizar.isEnabled = activos
        binding.btnRechazar.isEnabled = activos
    }

    /** Abre la app en Inicio (tras el login), donde también se puede responder la visita. */
    private fun abrirApp() {
        val teclado = getSystemService(KeyguardManager::class.java)
        val abrir = {
            startActivity(DestinoNotificacion(null, if (entrega) DestinoNotificacion.TIPO_ENTREGA else DestinoNotificacion.TIPO_VISITA, visitaId, null)
                .ponerEn(Intent(this, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && teclado.isKeyguardLocked) {
            teclado.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = abrir()
            })
        } else {
            abrir()
        }
    }

    override fun onDestroy() {
        cuenta?.cancel()
        pulso?.cancel()
        super.onDestroy()
    }
}
