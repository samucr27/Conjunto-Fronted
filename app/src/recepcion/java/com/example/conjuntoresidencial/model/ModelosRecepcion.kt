package com.example.conjuntoresidencial.model

// P7 bloque 4: el turno de la recepción (GuardiaService del backend)

data class GuardiaEnTurno(val id: Long, val nombre: String)

data class AbrirTurno(val guardiaId: Long, val pin: String)
