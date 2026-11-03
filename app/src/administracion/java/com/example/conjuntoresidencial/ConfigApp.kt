package com.example.conjuntoresidencial

/** Lo que distingue a la app de administración de la de residentes (P6). */
object ConfigApp {
    /** Pantalla de inicio de sesión y a la que se vuelve al cerrarla. */
    val destinoLogin: Int = R.id.loginAdminFragment

    /** Roles que pueden usar esta app. */
    val rolesPermitidos: Set<String> = setOf("ADMIN", "CONTADOR", "PORTERIA")

    /** P7 bloque 4: sin tocar la pantalla este tiempo, vuelve a [destinoBloqueo]. Solo la recepción. */
    val bloqueoInactividadMs: Long? = null
    val destinoBloqueo: Int? = null

    const val tipoApp = "ADMIN"
}
