package com.example.conjuntoresidencial

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.conjuntoresidencial.ui.comun.NotificacionesViewModel
import com.example.conjuntoresidencial.util.SesionEventos
import com.example.conjuntoresidencial.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var barraInferior: MaterialCardView
    private lateinit var navInferior: BottomNavigationView
    private val notificaciones: NotificacionesViewModel by viewModels()

    // Menú que muestra ahora la barra inferior (null = oculta)
    @MenuRes private var menuActual: Int? = null
    private var margenInferiorSistema = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        barraInferior = findViewById(R.id.barraInferior)
        navInferior = findViewById(R.id.navInferior)
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
        val franjaEstado = findViewById<View>(R.id.vistaBarraEstado)
        val contenedor = findViewById<View>(R.id.contenedor)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            franjaEstado.layoutParams = franjaEstado.layoutParams.apply { height = barras.top }
            contenedor.setPadding(barras.left, 0, barras.right, 0)
            // Abajo: la barra de navegación o el teclado, el que ocupe más
            margenInferiorSistema = maxOf(barras.bottom, teclado.bottom)
            acomodarContenido()
            WindowInsetsCompat.CONSUMED
        }

        val modoOscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false          // iconos blancos sobre la franja morada
            isAppearanceLightNavigationBars = !modoOscuro // iconos oscuros sobre fondo claro
        }
    }

    /**
     * La barra flota sobre la barra de navegación del sistema; el contenido termina encima
     * de ella para que nunca tape el último elemento de una lista.
     */
    private fun acomodarContenido() {
        val separacion = resources.getDimensionPixelSize(R.dimen.separacion_barra_inferior)
        barraInferior.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
            bottomMargin = margenInferiorSistema + separacion
        }
        val ocupado = if (barraInferior.visibility == View.VISIBLE) barraInferior.height + 2 * separacion else 0
        findViewById<View>(R.id.nav_host_fragment).setPadding(0, 0, 0, margenInferiorSistema + ocupado)
    }

    /** Pestañas de cada rol. Los id del menú son los de las pantallas del nav_graph. */
    private fun configurarBarraInferior() {
        navController.addOnDestinationChangedListener { _, destino, _ ->
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

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            SessionManager(this).clearSession()
        }
    }
}
