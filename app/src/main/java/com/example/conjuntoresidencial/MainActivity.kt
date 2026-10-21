package com.example.conjuntoresidencial

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.example.conjuntoresidencial.util.SesionEventos
import com.example.conjuntoresidencial.util.SessionManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
