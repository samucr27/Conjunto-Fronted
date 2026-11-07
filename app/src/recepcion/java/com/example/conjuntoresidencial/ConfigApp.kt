package com.example.conjuntoresidencial

/**
 * P7 bloque 4: la app de la recepción. Entra la cuenta de portería del conjunto y cada guardia abre
 * su turno con su PIN; sin usarla unos minutos se bloquea y pide el PIN otra vez.
 */
object ConfigApp {
    /** Pantalla de inicio de sesión y a la que se vuelve al cerrarla. */
    val destinoLogin: Int = R.id.loginAdminFragment

    /** Roles que pueden usar esta app. */
    val rolesPermitidos: Set<String> = setOf("PORTERIA")

    /** Sin tocar la pantalla 3 minutos, vuelve a la pantalla de turno. */
    val bloqueoInactividadMs: Long? = 3 * 60_000L
    val destinoBloqueo: Int? = R.id.turnoFragment

    /** P8 bloque 2: la primera vez se elige el conjunto (la plataforma no es de ninguno). */
    const val usaConjunto = true

    /** P8 bloque 2: la primera vez se elige si se entra como propietario o arrendatario. */
    /** P9/P11: después del conjunto se pide su código diario (residentes, administración y portería). */
    const val pideCodigo = true

    const val pideRol = false

    /** P10: barra inferior propia de la app (menú y pantallas que la muestran); null usa las de cada rol. */
    val barraPropia: Pair<Int, Set<Int>>? = null

    const val tipoApp = "RECEPCION"
}
