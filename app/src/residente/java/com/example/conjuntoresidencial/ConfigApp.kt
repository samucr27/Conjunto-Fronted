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

    /** P8 bloque 2: la primera vez se elige el conjunto (la plataforma no es de ninguno). */
    const val usaConjunto = true

    /** P8 bloque 2: la primera vez se elige si se entra como propietario o arrendatario. */
    /** P9/P11: después del conjunto se pide su código diario (residentes, administración y portería). */
    const val pideCodigo = true

    const val pideRol = true

    /** P10: barra inferior propia de la app (menú y pantallas que la muestran); null usa las de cada rol. */
    val barraPropia: Pair<Int, Set<Int>>? = null

    const val tipoApp = "RESIDENTE"
}
