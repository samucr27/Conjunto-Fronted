package com.example.conjuntoresidencial.model

// P7 bloque 4: guardias de la recepción (GuardiaService del backend). El PIN nunca vuelve del servidor

data class GuardiaDTO(val id: Long, val nombre: String, val documento: String, val activo: Boolean, val creadoEn: String?)

/** [pin] null al editar: no cambia. [activo] null: no cambia. */
data class DatosGuardia(val nombre: String, val documento: String, val pin: String? = null, val activo: Boolean? = null)
