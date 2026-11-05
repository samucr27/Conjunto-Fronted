package com.example.conjuntoresidencial.model

// P7 bloque 4: el turno de la recepción (GuardiaService del backend)

data class GuardiaEnTurno(val id: Long, val nombre: String)

data class AbrirTurno(val guardiaId: Long, val pin: String)

// P8 bloque 5: el tablero de la portería (PorteriaTableroService del backend)

data class ResumenPorteria(
    val visitasHoy: Int, val esperandoRespuesta: Int, val dentro: Int, val vehiculosDentro: Int,
    val parqueaderosLibres: Int, val parqueaderosTotal: Int, val minutaHoy: Int, val guardia: String?,
    // P9 bloque 3 (0 con un servidor anterior)
    val correspondenciaPendiente: Int = 0
)

data class PaginaVisitas(val visitas: List<VisitaDTO>, val hayMas: Boolean)

data class Habitante(val nombre: String, val tipo: String?, val telefono: String?)

data class ApartamentoDirectorio(
    val id: Long, val torre: String, val apto: String, val ocupacion: String?, val citofono1: String?,
    val citofono2: String?, val residentes: List<Habitante>, val placas: List<String>
)

data class AnotacionMinuta(val id: Long, val categoria: String, val texto: String, val creadaEn: String, val guardia: String?)

data class NuevaAnotacion(val categoria: String, val texto: String)
