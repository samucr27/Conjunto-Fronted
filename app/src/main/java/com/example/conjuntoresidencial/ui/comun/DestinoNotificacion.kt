package com.example.conjuntoresidencial.ui.comun

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.ui.admin.ReservasAdminFragment
import com.example.conjuntoresidencial.ui.admin.VehiculosAdminFragment

/**
 * A dónde lleva una notificación, al tocarla en la pestaña Notificaciones o en el aviso del
 * celular. Administrador: una reserva → Reservas, con esa reserva resaltada; un vehículo → los
 * vehículos por aprobar. Residente: la respuesta sobre su vehículo o su reserva → Mis vehículos
 * o Mis reservas. Las notificaciones viejas (sin referencia) llevan a la lista general.
 */
data class DestinoNotificacion(
    val notificacionId: Long?,
    val tipo: String,
    val referenciaId: Long?,
    val apartamentoId: Long?
) {
    /** Las de los residentes empiezan por el tipo de lo que se revisó: VEHICULO_APROBADO... */
    val esParaAdmin: Boolean get() = tipo == TIPO_RESERVA || tipo == TIPO_VEHICULO

    fun abrir(nav: NavController) {
        when {
            tipo == TIPO_VEHICULO -> nav.navigate(
                R.id.vehiculosAdminFragment,
                referenciaId?.let { bundleOf(VehiculosAdminFragment.ARG_RESALTAR to it) }
            )
            tipo.startsWith(TIPO_VEHICULO) -> nav.navigate(R.id.misVehiculosFragment)
            tipo.startsWith(TIPO_RESERVA) && !esParaAdmin -> nav.navigate(R.id.misReservasFragment)
            else -> nav.navigate(
                R.id.reservasAdminFragment,
                referenciaId?.let { bundleOf(ReservasAdminFragment.ARG_RESALTAR to it) }
            )
        }
    }

    /** Datos que viajan en el aviso del celular hasta MainActivity. */
    fun ponerEn(intent: Intent): Intent = intent.apply {
        putExtra(EXTRA_TIPO, tipo)
        notificacionId?.let { putExtra(EXTRA_NOTIFICACION, it) }
        referenciaId?.let { putExtra(EXTRA_REFERENCIA, it) }
        apartamentoId?.let { putExtra(EXTRA_APARTAMENTO, it) }
    }

    companion object {
        const val TIPO_RESERVA = "RESERVA"
        const val TIPO_VEHICULO = "VEHICULO"
        private const val EXTRA_TIPO = "notificacion_tipo"
        private const val EXTRA_NOTIFICACION = "notificacion_id"
        private const val EXTRA_REFERENCIA = "notificacion_referencia"
        private const val EXTRA_APARTAMENTO = "notificacion_apartamento"

        fun de(n: NotificacionDTO) = DestinoNotificacion(n.id, n.tipo, n.referenciaId, n.apartamentoId)

        /** Lo que llega de Firebase (todos los valores son texto). */
        fun de(datos: Map<String, String>): DestinoNotificacion? {
            val tipo = datos["tipo"] ?: return null
            return DestinoNotificacion(
                datos["notificacionId"]?.toLongOrNull(), tipo,
                datos["referenciaId"]?.toLongOrNull(), datos["apartamentoId"]?.toLongOrNull()
            )
        }

        /** null si el intent no viene de tocar un aviso. */
        fun de(intent: Intent?): DestinoNotificacion? {
            val tipo = intent?.getStringExtra(EXTRA_TIPO) ?: return null
            fun largo(clave: String) = if (intent.hasExtra(clave)) intent.getLongExtra(clave, 0) else null
            return DestinoNotificacion(largo(EXTRA_NOTIFICACION), tipo, largo(EXTRA_REFERENCIA), largo(EXTRA_APARTAMENTO))
        }

        /** Para que el intent no vuelva a abrir la pantalla (al girar, al volver a la app). */
        fun limpiar(intent: Intent?) {
            listOf(EXTRA_TIPO, EXTRA_NOTIFICACION, EXTRA_REFERENCIA, EXTRA_APARTAMENTO).forEach { intent?.removeExtra(it) }
        }
    }
}
