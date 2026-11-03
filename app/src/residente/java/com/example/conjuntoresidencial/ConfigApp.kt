package com.example.conjuntoresidencial

/** Lo que distingue a la app de residentes de la de administración (P6). */
object ConfigApp {
    /** Pantalla de inicio de sesión y a la que se vuelve al cerrarla. */
    val destinoLogin: Int = R.id.loginApartamentoFragment

    /** Roles que pueden usar esta app. */
    val rolesPermitidos: Set<String> = setOf("PROPIETARIO", "ARRENDATARIO")

    /** P7 bloque 4: sin tocar la pantalla este tiempo, vuelve a [destinoBloqueo]. Solo la recepción. */
    val bloqueoInactividadMs: Long? = null
    val destinoBloqueo: Int? = null

    const val tipoApp = "RESIDENTE"
}
