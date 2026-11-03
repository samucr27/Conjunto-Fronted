package com.example.conjuntoresidencial

/** P7: la app de plataforma. Crea, edita y suspende conjuntos; no es de ninguno. */
object ConfigApp {
    /** Pantalla de inicio de sesión y a la que se vuelve al cerrarla. */
    val destinoLogin: Int = R.id.loginPlataformaFragment

    /** Roles que pueden usar esta app. */
    val rolesPermitidos: Set<String> = setOf("PLATAFORMA")

    const val tipoApp = "PLATAFORMA"
}
