package com.example.conjuntoresidencial.ui.propietario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.ItemVehiculoBinding
import com.example.conjuntoresidencial.model.CupoVehiculoDTO
import com.example.conjuntoresidencial.model.VehiculoDTO
import com.example.conjuntoresidencial.ui.comun.Estados
import com.example.conjuntoresidencial.ui.comun.mostrarEstado

/** Filas de Mis vehículos (P5): el encabezado de cada categoría, sus vehículos o "No asignado". */
sealed class FilaVehiculo {
    data class Categoria(val tipo: String, val usados: Int, val maximo: Int) : FilaVehiculo()
    data class Vehiculo(val vehiculo: VehiculoDTO) : FilaVehiculo()
    data class SinAsignar(val tipo: String) : FilaVehiculo()

    companion object {
        /** Todas las categorías (las del cupo), con lo que tiene el apartamento en cada una. */
        fun de(cupos: List<CupoVehiculoDTO>, vehiculos: List<VehiculoDTO>): List<FilaVehiculo> {
            val tipos = (cupos.map { it.tipo } + vehiculos.map { it.tipo }).distinct()
            return tipos.flatMap { tipo ->
                val deTipo = vehiculos.filter { it.tipo == tipo }
                val maximo = cupos.firstOrNull { it.tipo == tipo }?.maximo ?: 0
                // Las categorías no permitidas sin vehículos no se muestran
                if (maximo == 0 && deTipo.isEmpty()) return@flatMap emptyList()
                listOf(Categoria(tipo, deTipo.count { it.estado != "RECHAZADO" }, maximo)) +
                    (if (deTipo.isEmpty()) listOf(SinAsignar(tipo)) else deTipo.map { Vehiculo(it) })
            }
        }
    }
}

class VehiculosPorTipoAdapter(
    private val onEliminar: (VehiculoDTO) -> Unit,
    private val onEditar: (VehiculoDTO) -> Unit,
    // El arrendatario (o el propietario, si el conjunto lo deja a la administración) solo consulta
    private val puedeGestionar: Boolean = true
) : ListAdapter<FilaVehiculo, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<FilaVehiculo>() {
    override fun areItemsTheSame(a: FilaVehiculo, b: FilaVehiculo) = when {
        a is FilaVehiculo.Vehiculo && b is FilaVehiculo.Vehiculo -> a.vehiculo.id == b.vehiculo.id
        a is FilaVehiculo.Categoria && b is FilaVehiculo.Categoria -> a.tipo == b.tipo
        a is FilaVehiculo.SinAsignar && b is FilaVehiculo.SinAsignar -> a.tipo == b.tipo
        else -> false
    }
    override fun areContentsTheSame(a: FilaVehiculo, b: FilaVehiculo) = a == b
}) {

    private class Texto(val vista: TextView) : RecyclerView.ViewHolder(vista)
    private class Tarjeta(val binding: ItemVehiculoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FilaVehiculo.Categoria -> 0
        is FilaVehiculo.SinAsignar -> 1
        is FilaVehiculo.Vehiculo -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflador = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> Texto(inflador.inflate(R.layout.item_categoria_vehiculo, parent, false) as TextView)
            1 -> Texto(inflador.inflate(R.layout.item_sin_asignar, parent, false) as TextView)
            else -> Tarjeta(ItemVehiculoBinding.inflate(inflador, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val ctx = holder.itemView.context
        when (val fila = getItem(position)) {
            // "Carro · 1 de 1"
            is FilaVehiculo.Categoria -> (holder as Texto).vista.text =
                ctx.getString(R.string.vehiculos_categoria, fila.tipo, fila.usados, fila.maximo)
            is FilaVehiculo.SinAsignar -> (holder as Texto).vista.setText(R.string.vehiculos_no_asignado)
            is FilaVehiculo.Vehiculo -> mostrar((holder as Tarjeta).binding, fila.vehiculo)
        }
    }

    private fun mostrar(b: ItemVehiculoBinding, v: VehiculoDTO) {
        val ctx = b.root.context
        // "Mazda · Carro" y "ABC123 · Rojo · Parqueadero C-01"; sin marca o color se omiten
        b.tvMarcaVehiculo.text = listOf(v.marca, v.tipo).filterNot { it.isNullOrBlank() }.joinToString(" · ")
        b.tvPlacaVehiculo.text = listOfNotNull(v.placa, v.color,
            v.parqueadero?.let { ctx.getString(R.string.vehiculo_parqueadero, it) }).filterNot { it.isBlank() }.joinToString(" · ")
        b.tvEstadoVehiculo.mostrarEstado(Estados.vehiculo(v.estado))
        // Rechazado: el motivo; por aprobar: que espera la aprobación y el parqueadero
        val nota = when (v.estado) {
            "RECHAZADO" -> v.motivoRechazo?.let { ctx.getString(R.string.vehiculo_motivo_rechazo, it) }
            "PENDIENTE" -> ctx.getString(R.string.vehiculo_pendiente_parqueadero)
            else -> if (v.parqueadero == null) ctx.getString(R.string.vehiculo_sin_parqueadero) else null
        }
        b.tvNotaVehiculo.text = nota
        b.tvNotaVehiculo.visibility = if (nota == null) View.GONE else View.VISIBLE
        b.btnEliminarVehiculo.contentDescription = ctx.getString(R.string.desc_eliminar_vehiculo, v.placa ?: v.tipo)
        b.btnEliminarVehiculo.visibility = if (puedeGestionar) View.VISIBLE else View.GONE
        b.btnEliminarVehiculo.setOnClickListener { onEliminar(v) }
        if (puedeGestionar) b.root.setOnClickListener { onEditar(v) } else b.root.isClickable = false
    }
}
