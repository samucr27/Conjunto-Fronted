package com.example.conjuntoresidencial

/** P7: la app de plataforma. Crea, edita y suspende conjuntos; no es de ninguno. */
object ConfigApp {
    /** Pantalla de inicio de sesión y a la que se vuelve al cerrarla. */
    val destinoLogin: Int = R.id.loginPlataformaFragment

    /** Roles que pueden usar esta app. */
    val rolesPermitidos: Set<String> = setOf("PLATAFORMA")

    /** P7 bloque 4: sin tocar la pantalla este tiempo, vuelve a [destinoBloqueo]. Solo la recepción. */
    val bloqueoInactividadMs: Long? = null
    val destinoBloqueo: Int? = null

    /** P8 bloque 2: la primera vez se elige el conjunto (la plataforma no es de ninguno). */
    const val usaConjunto = false

    /** P8 bloque 2: la primera vez se elige si se entra como propietario o arrendatario. */
    /** P9 bloque 2: después del conjunto se pide su código diario (solo la app de residentes). */
    const val pideCodigo = false

    const val pideRol = false

    /** P10: barra inferior con el panel, los conjuntos y la auditoría siempre a mano. */
    val barraPropia: Pair<Int, Set<Int>>? = R.menu.menu_barra_plataforma to
        setOf(R.id.panelPlataformaFragment, R.id.conjuntosPlataformaFragment, R.id.auditoriaPlataformaFragment)

    const val tipoApp = "PLATAFORMA"
}
