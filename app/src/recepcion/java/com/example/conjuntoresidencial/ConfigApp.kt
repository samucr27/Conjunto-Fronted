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
    /** P9 bloque 2: después del conjunto se pide su código diario (solo la app de residentes). */
    const val pideCodigo = false

    const val pideRol = false

    const val tipoApp = "RECEPCION"
}
