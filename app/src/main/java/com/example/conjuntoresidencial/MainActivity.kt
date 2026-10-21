package com.example.conjuntoresidencial

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.example.conjuntoresidencial.util.SesionEventos
import com.example.conjuntoresidencial.util.SessionManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ajustarBarrasDelSistema()

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
        val contenido = findViewById<View>(R.id.nav_host_fragment)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            franjaEstado.layoutParams = franjaEstado.layoutParams.apply { height = barras.top }
            // Abajo: la barra de navegación o el teclado, el que ocupe más
            contenido.setPadding(barras.left, 0, barras.right, maxOf(barras.bottom, teclado.bottom))
            WindowInsetsCompat.CONSUMED
        }

        val modoOscuro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false          // iconos blancos sobre la franja morada
            isAppearanceLightNavigationBars = !modoOscuro // iconos oscuros sobre fondo claro
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                SessionManager(this).clearSession()
                findNavController(R.id.nav_host_fragment).navigate(R.id.loginSelectorFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Vuelve al selector de login y vacía la pila para que "atrás" no regrese a pantallas protegidas. */
    private fun irAlLogin() {
        val navController = findNavController(R.id.nav_host_fragment)
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
