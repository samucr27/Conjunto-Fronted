package com.example.conjuntoresidencial

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.conjuntoresidencial.ui.comun.ControlesFlotantes
import com.example.conjuntoresidencial.ui.comun.DifuminadoProgresivo
import com.example.conjuntoresidencial.ui.comun.NotificacionesViewModel
import com.example.conjuntoresidencial.util.SesionEventos
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class MainActivity : AppCompatActivity(), ControlesFlotantes {

    private lateinit var navController: NavController
    private lateinit var barraInferior: BlurView
    private lateinit var navInferior: BottomNavigationView
    private lateinit var franjaSuperior: BlurView
    private lateinit var botonesSuperiores: View
    private lateinit var btnVolver: MaterialButton
    private lateinit var btnMas: MaterialButton
    private val notificaciones: NotificacionesViewModel by viewModels()

    // Menú que muestra ahora la barra inferior (null = oculta)
    @MenuRes private var menuActual: Int? = null
    private var margenInferiorSistema = 0
    // Difuminado progresivo de los bordes (Android 13+); null en versiones anteriores
    private var difuminado: DifuminadoProgresivo? = null
    private var tecladoAbierto = false
    // Botón fijo abajo de la pantalla actual (Registrar obra, Agregar...): el difuminado termina encima
    private var vistaReservada: View? = null
    private val alCambiarReservada = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> actualizarZonasDifuminadas() }
    private var modoOscuro = false
    override var margenSuperior = 0
        private set
    private val _espacioInferior = MutableLiveData(0)
    override val espacioInferior: LiveData<Int> get() = _espacioInferior

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de inicio del sistema: solo el fondo morado (el logo lo anima la app)
        val inicio = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) mostrarLogoAnimado(inicio)
        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        barraInferior = findViewById(R.id.barraInferior)
        navInferior = findViewById(R.id.navInferior)
        franjaSuperior = findViewById(R.id.franjaSuperior)
        botonesSuperiores = findViewById(R.id.botonesSuperiores)
        btnVolver = findViewById(R.id.btnVolver)
        btnMas = findViewById(R.id.btnMas)
        configurarDesenfoque()
        ajustarBarrasDelSistema()
        configurarBarraInferior()

        SesionEventos.expirada.observe(this) { evento ->
            evento.obtenerSiNoManejado()?.let {
                Toast.makeText(this, R.string.sesion_expirada, Toast.LENGTH_LONG).show()
                irAlLogin()
            }
        }
        SesionEventos.sinPermiso.observe(this) { evento ->
            evento.obtenerSiNoManejado()?.let {
                Toast.makeText(this, R.string.sin_permiso, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Desde Android 15 (targetSdk 35) la app se dibuja debajo de la barra de estado y
     * la de navegación. Sin esto los encabezados quedaban tapados y los botones de
     * arriba (Salir, notificaciones) no se podían tocar. Se activa también en
     * versiones anteriores para que la app se vea igual en todas.
     */
    private fun ajustarBarrasDelSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val raiz = findViewById<View>(R.id.raiz)
        val contenedor = findViewById<View>(R.id.contenedor)
        val altoBotones = resources.getDimensionPixelSize(R.dimen.alto_botones_superiores)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            // Arriba el contenido llega hasta el borde (detrás de la barra de estado): la franja
            // difuminada cubre la barra de estado y la fila de botones
            margenSuperior = barras.top
            franjaSuperior.updateLayoutParams<android.widget.FrameLayout.LayoutParams> { height = barras.top + altoBotones }
            botonesSuperiores.updateLayoutParams<android.widget.FrameLayout.LayoutParams> { topMargin = barras.top }
            contenedor.setPadding(barras.left, 0, barras.right, 0)
            // Abajo: la barra de navegación o el teclado, el que ocupe más
            margenInferiorSistema = maxOf(barras.bottom, teclado.bottom)
            tecladoAbierto = teclado.bottom > barras.bottom
            acomodarContenido()
            WindowInsetsCompat.CONSUMED
        }

        modoOscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false          // iconos blancos sobre el morado del encabezado
            isAppearanceLightNavigationBars = !modoOscuro // iconos oscuros sobre fondo claro
        }
    }

    /**
     * La barra flota sobre la barra de navegación del sistema y el contenido pasa por debajo
     * de ella (se ve desenfocado). Para que el último elemento de una lista se pueda leer,
     * cada pantalla deja al final un espacio del alto de la barra ([espacioInferior]).
     */
    private fun acomodarContenido() {
        val separacion = resources.getDimensionPixelSize(R.dimen.separacion_barra_inferior)
        barraInferior.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
            bottomMargin = margenInferiorSistema + separacion
        }
        findViewById<View>(R.id.nav_host_fragment).setPadding(0, 0, 0, margenInferiorSistema)
        val espacio = if (barraInferior.visibility == View.VISIBLE) barraInferior.height + 2 * separacion else 0
        if (_espacioInferior.value != espacio) _espacioInferior.value = espacio
        actualizarZonasDifuminadas()
    }

    /**
     * Zonas del difuminado progresivo. Arriba: la barra de estado y, si se muestran, la fila de
     * botones (el título grande empieza justo debajo y en reposo se ve nítido). Abajo: solo en
     * las pestañas, desde encima de la barra inferior hasta el borde; en las demás pantallas no
     * hay nada flotante abajo (y el botón fijo de "Registrar obra" se vería borroso).
     */
    private fun actualizarZonasDifuminadas() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val efecto = difuminado ?: return
        val contenedor = findViewById<View>(R.id.contenedor)
        val altoBotones = resources.getDimensionPixelSize(R.dimen.alto_botones_superiores)
        val arriba = margenSuperior + if (botonesSuperiores.visibility == View.VISIBLE) altoBotones else 0
        val espacioBarra = _espacioInferior.value ?: 0
        val reservada = vistaReservada?.takeIf { it.isAttachedToWindow && it.visibility == View.VISIBLE && it.height > 0 }
        val (abajo, fin) = when {
            // Con el teclado abierto no se difumina abajo: ahí está el campo en el que se escribe
            tecladoAbierto -> 0 to 0
            // Pestañas: el contenido pasa bajo la barra translúcida hasta el borde
            espacioBarra > 0 -> espacioBarra + margenInferiorSistema to contenedor.height
            // Botón fijo abajo: el difuminado termina justo encima de él
            reservada != null -> {
                val ubicacion = IntArray(2).also(reservada::getLocationInWindow)
                val inicioContenedor = IntArray(2).also(contenedor::getLocationInWindow)
                zonaAbajo to (ubicacion[1] - inicioContenedor[1] - zonaSeparacion)
            }
            // Resto: hasta donde termina el contenido (encima de la barra de navegación del sistema)
            else -> zonaAbajo to (contenedor.height - margenInferiorSistema)
        }
        efecto.ponerZonas(arriba.toFloat(), abajo.toFloat(), fin.toFloat())
    }

    private val zonaAbajo get() = (resources.displayMetrics.density * ZONA_ABAJO_DP).toInt()
    private val zonaSeparacion get() = (resources.displayMetrics.density * 8).toInt()

    override fun reservarAbajo(vista: View?) {
        vistaReservada?.removeOnLayoutChangeListener(alCambiarReservada)
        vistaReservada = vista
        vista?.addOnLayoutChangeListener(alCambiarReservada)
        // Al crearse la pantalla aún no tiene posición: se recalcula tras el primer dibujo
        (vista ?: botonesSuperiores).post { actualizarZonasDifuminadas() }
    }

    /** Pestañas de cada rol. Los id del menú son los de las pantallas del nav_graph. */
    private fun configurarBarraInferior() {
        navController.addOnDestinationChangedListener { _, destino, _ ->
            // Cada pantalla vuelve a pedir sus botones de arriba (configurarEncabezado)
            ocultarControles()
            reservarAbajo(null)
            val menu = when (destino.id) {
                R.id.adminDashboardFragment, R.id.notificacionesFragment -> R.menu.menu_barra_admin
                R.id.propietarioDashboardFragment, R.id.misReservasFragment, R.id.miCarteraFragment ->
                    R.menu.menu_barra_residente
                R.id.cuentaFragment ->
                    if (SessionManager(this).esAdmin()) R.menu.menu_barra_admin else R.menu.menu_barra_residente
                else -> null
            }
            mostrarBarra(menu)
            if (menu != null && navInferior.selectedItemId != destino.id) navInferior.selectedItemId = destino.id
            if (menu == R.menu.menu_barra_admin) notificaciones.actualizarConteo()
        }

        navInferior.setOnItemSelectedListener { item ->
            val actual = navController.currentDestination?.id
            if (item.itemId != actual) irAPestana(item.itemId)
            true
        }

        // Insignia de no leídas en la pestaña Notificaciones (solo existe en la barra del administrador)
        notificaciones.noLeidas.observe(this) { ponerInsignia() }
    }

    private fun ponerInsignia() {
        if (menuActual != R.menu.menu_barra_admin) return
        val noLeidas = notificaciones.noLeidas.value ?: 0
        navInferior.getOrCreateBadge(R.id.notificacionesFragment).apply {
            number = noLeidas
            isVisible = noLeidas > 0
            // TalkBack: "Notificaciones, 2 notificaciones sin leer" (por defecto vendría en inglés)
            setContentDescriptionQuantityStringsResource(R.plurals.resumen_notificaciones)
        }
    }

    private fun mostrarBarra(@MenuRes menu: Int?) {
        if (menu != menuActual) {
            menuActual = menu
            if (menu != null) {
                navInferior.menu.clear()
                navInferior.inflateMenu(menu)
                ponerInsignia()
            }
        }
        barraInferior.visibility = if (menu == null) View.GONE else View.VISIBLE
        // La altura de la barra se conoce después de medirla
        barraInferior.post { acomodarContenido() }
    }

    /**
     * Cambia de pestaña dejando debajo solo Inicio (el primer elemento del menú):
     * "atrás" desde cualquier pestaña vuelve a Inicio, y desde Inicio pregunta si cerrar sesión.
     */
    private fun irAPestana(destino: Int) {
        val inicio = navInferior.menu.getItem(0).itemId
        if (destino == inicio) {
            navController.popBackStack(inicio, false)
            return
        }
        navController.navigate(
            destino, null,
            NavOptions.Builder().setPopUpTo(inicio, false).setLaunchSingleTop(true).build()
        )
    }

    /**
     * Carga inicial: sobre el mismo morado de la pantalla de inicio el logo se dibuja trazo a
     * trazo (drawable/logo_animado) y después crece y se desvanece dejando ver la app. Lo hace
     * la app y no el sistema para que se vea igual en todas las versiones de Android.
     * Si el usuario desactivó las animaciones, no se muestra.
     */
    private fun mostrarLogoAnimado(inicio: SplashScreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()) return
        val capa = findViewById<View>(R.id.capaInicio)
        capa.visibility = View.VISIBLE
        // Empieza cuando se va la pantalla del sistema (si no, se animaría oculta debajo de ella);
        // por si el sistema no la mostró, arranca igual al poco del primer dibujo
        var empezado = false
        val empezar = {
            if (!empezado) {
                empezado = true
                animarLogo(capa)
            }
        }
        inicio.setOnExitAnimationListener { pantalla ->
            pantalla.remove()
            empezar()
        }
        capa.postDelayed(empezar, ESPERA_MAXIMA_INICIO_MS)
    }

    private fun animarLogo(capa: View) {
        val logo = findViewById<ImageView>(R.id.imgLogoInicio)
        (logo.drawable as? Animatable)?.start()
        logo.animate()
            .scaleX(1.25f).scaleY(1.25f).alpha(0f)
            .setStartDelay(DURACION_LOGO_MS)
            .setDuration(SALIDA_LOGO_MS)
            .start()
        capa.animate()
            .alpha(0f)
            .setStartDelay(DURACION_LOGO_MS + SALIDA_LOGO_MS / 3)
            .setDuration(SALIDA_LOGO_MS)
            .withEndAction { capa.visibility = View.GONE }
            .start()
    }

    // ---- Botones flotantes y franja superior (ControlesFlotantes) ----

    /**
     * Desenfoque en vivo de lo que pasa bajo la franja superior (Android 12+, en el hilo de
     * dibujo del sistema). En versiones anteriores no hay desenfoque eficiente: degradado morado.
     */
    private fun configurarDesenfoque() {
        val contenedor = findViewById<BlurTarget>(R.id.contenedor)
        when {
            // Android 13+: el contenido mismo se difumina poco a poco hacia los bordes.
            // No hace falta franja; la barra es una cápsula translúcida sin desenfoque propio.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                difuminado = DifuminadoProgresivo(contenedor, resources.displayMetrics.density * RADIO_PROGRESIVO_DP)
                franjaSuperior.visibility = View.GONE
                barraInferior.setBackgroundResource(R.drawable.fondo_barra_inferior_translucida)
            }
            // Android 12: desenfoque simple de una franja fija y de la barra (BlurView)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                franjaSuperior.setupWith(contenedor)
                    .setBlurRadius(RADIO_DESENFOQUE)
                    .setOverlayColor(ContextCompat.getColor(this, R.color.velo_desenfoque))
                barraInferior.setupWith(contenedor)
                    .setBlurRadius(RADIO_DESENFOQUE)
                    .setOverlayColor(ContextCompat.getColor(this, R.color.velo_barra))
            }
            // Android 7-11: sin desenfoque en vivo, degradado del color de fondo
            else -> {
                franjaSuperior.setBackgroundResource(R.drawable.franja_degradada)
                barraInferior.setBackgroundResource(R.drawable.fondo_barra_inferior_respaldo)
            }
        }
        // El desenfoque se recorta con la forma de cápsula del fondo
        barraInferior.outlineProvider = ViewOutlineProvider.BACKGROUND
        barraInferior.clipToOutline = true
    }

    override fun mostrarControles(atras: Boolean, alVolver: () -> Unit, @MenuRes menu: Int?, alElegir: (Int) -> Boolean) {
        botonesSuperiores.visibility = View.VISIBLE
        actualizarZonasDifuminadas()
        btnVolver.visibility = if (atras) View.VISIBLE else View.GONE
        btnVolver.setOnClickListener { alVolver() }
        btnMas.visibility = if (menu == null) View.GONE else View.VISIBLE
        btnMas.setOnClickListener { boton ->
            if (menu == null) return@setOnClickListener
            PopupMenu(this, boton).apply {
                inflate(menu)
                setOnMenuItemClickListener { alElegir(it.itemId) }
            }.show()
        }
    }

    override fun ponerPlegado(fraccion: Float) {
        franjaSuperior.alpha = ((fraccion - 0.75f) / 0.25f).coerceIn(0f, 1f)
        // Cuando el encabezado morado sale, arriba pasa el contenido: en modo claro los iconos
        // de la barra de estado (hora, batería) pasan a oscuros para seguir leyéndose
        ponerIconosOscuros(!modoOscuro && fraccion > 0.85f)
    }

    private fun ocultarControles() {
        botonesSuperiores.visibility = View.GONE
        btnVolver.setOnClickListener(null)
        btnMas.setOnClickListener(null)
        franjaSuperior.alpha = 0f
        ponerIconosOscuros(false)
        actualizarZonasDifuminadas()
    }

    private fun ponerIconosOscuros(oscuros: Boolean) {
        val control = WindowCompat.getInsetsController(window, window.decorView)
        if (control.isAppearanceLightStatusBars != oscuros) control.isAppearanceLightStatusBars = oscuros
    }

    /** Vuelve al selector de login y vacía la pila para que "atrás" no regrese a pantallas protegidas. */
    private fun irAlLogin() {
        val actual = navController.currentDestination?.id
        val pantallasLogin = setOf(R.id.loginSelectorFragment, R.id.loginAdminFragment, R.id.loginApartamentoFragment)
        if (actual in pantallasLogin) return
        navController.navigate(
            R.id.loginSelectorFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        )
    }

    companion object {
        private const val RADIO_DESENFOQUE = 24f
        // Radio del desenfoque justo en el borde (dp); hacia el centro baja hasta 0
        private const val RADIO_PROGRESIVO_DP = 22f
        // Alto de la zona difuminada de abajo en pantallas sin barra inferior (dp)
        private const val ZONA_ABAJO_DP = 96
        // Lo que tarda en dibujarse el logo animado (drawable/logo_animado) y su salida
        private const val DURACION_LOGO_MS = 1100L
        private const val SALIDA_LOGO_MS = 350L
        private const val ESPERA_MAXIMA_INICIO_MS = 4000L
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            SessionManager(this).clearSession()
        }
    }
}
