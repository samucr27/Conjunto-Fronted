package com.example.conjuntoresidencial.ui.comun

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.model.NotificacionDTO
import com.example.conjuntoresidencial.ui.admin.ReservasAdminFragment
import com.example.conjuntoresidencial.ui.admin.VehiculosAdminFragment
import com.example.conjuntoresidencial.ui.residentes.ResidentesFragment

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
    val esParaAdmin: Boolean get() =
        tipo == TIPO_RESERVA || tipo == TIPO_VEHICULO || tipo == TIPO_RESIDENTE || tipo == TIPO_COMPROBANTE ||
            tipo == TIPO_REGISTRO || tipo == TIPO_OBRA || tipo == TIPO_PERSONAL_OBRA || tipo == TIPO_SOLICITUD_CONTABLE

    fun abrir(nav: NavController) {
        when {
            // P5: mensaje de chat → esa conversación
            tipo == TIPO_CHAT -> nav.navigate(R.id.chatFragment,
                referenciaId?.let { bundleOf(com.example.conjuntoresidencial.ui.chat.ChatFragment.ARG_CONVERSACION to it) })
            // P5: obra por aprobar o revisada → su detalle; trabajador → la lista de obras
            tipo == TIPO_OBRA || tipo.startsWith("${TIPO_OBRA}_") -> nav.navigate(R.id.obraDetalleFragment,
                referenciaId?.let { bundleOf(com.example.conjuntoresidencial.ui.obras.ObraDetalleFragment.ARG_OBRA to it) })
            tipo == TIPO_PERSONAL_OBRA -> nav.navigate(R.id.controlObrasFragment)
            tipo.startsWith(TIPO_PERSONAL_OBRA) -> nav.navigate(R.id.misObrasFragment)
            // P5: lo que mandó el contador → la cartera de la administración
            tipo == TIPO_SOLICITUD_CONTABLE -> nav.navigate(R.id.carteraResumenFragment)
            // Nueva publicación → el feed de Inicio
            tipo == TIPO_PUBLICACION -> nav.navigate(R.id.inicioFragment)
            // Comprobante por revisar → Facturación; cuota nueva o revisada → Mi cartera
            tipo == TIPO_COMPROBANTE -> nav.navigate(R.id.cuotasFragment)
            tipo.startsWith(TIPO_CUOTA) -> nav.navigate(R.id.miCarteraFragment)
            // Arrendatario por aprobar → la lista de pendientes; su aprobación o rechazo → Mi hogar
            tipo == TIPO_RESIDENTE || tipo == TIPO_REGISTRO -> nav.navigate(R.id.residentesFragment, ResidentesFragment.pendientes())
            // Un arrendatario pidió registrarse: el propietario lo revisa en Mi hogar
            tipo == TIPO_SOLICITUD_ARRENDATARIO -> nav.navigate(R.id.arrendatarioFragment)
            tipo.startsWith(TIPO_RESIDENTE) -> nav.navigate(R.id.miHogarFragment)
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
        const val TIPO_RESIDENTE = "RESIDENTE"
        const val TIPO_CUOTA = "CUOTA"
        const val TIPO_REGISTRO = "REGISTRO"
        const val TIPO_PUBLICACION = "PUBLICACION"
        const val TIPO_SOLICITUD_ARRENDATARIO = "SOLICITUD_ARRENDATARIO"
        const val TIPO_COMPROBANTE = "COMPROBANTE"
        const val TIPO_CHAT = "CHAT"
        const val TIPO_OBRA = "OBRA"
        const val TIPO_PERSONAL_OBRA = "PERSONAL_OBRA"
        const val TIPO_SOLICITUD_CONTABLE = "SOLICITUD_CONTABLE"
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
