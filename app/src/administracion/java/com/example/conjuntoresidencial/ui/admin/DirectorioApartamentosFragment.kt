package com.example.conjuntoresidencial.ui.admin

import com.example.conjuntoresidencial.ui.comun.repetir
import com.example.conjuntoresidencial.ui.comun.BarraBusqueda
import com.example.conjuntoresidencial.api.RetrofitClient
import com.example.conjuntoresidencial.api.instance
import com.example.conjuntoresidencial.model.ApartamentoRequest
import com.example.conjuntoresidencial.ui.comun.controlesFlotantes
import com.example.conjuntoresidencial.ui.comun.ejecutarAccion
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conjuntoresidencial.R
import com.example.conjuntoresidencial.databinding.FragmentDirectorioApartamentosBinding
import com.example.conjuntoresidencial.model.Apartamento
import com.example.conjuntoresidencial.ui.comun.mostrarRecurso
import com.example.conjuntoresidencial.ui.comun.accionesDeLista
import com.example.conjuntoresidencial.ui.comun.configurarEncabezado
import com.example.conjuntoresidencial.ui.comun.ponerInfo
import com.example.conjuntoresidencial.ui.comun.resumen
import com.example.conjuntoresidencial.util.map

/**
 * Directorio en dos pasos para encontrar rápido un apartamento: primero la lista de torres y,
 * al tocar una, esta misma pantalla con sus apartamentos (argumento [ARG_TORRE]).
 */
class DirectorioApartamentosFragment : Fragment() {
    private var _binding: FragmentDirectorioApartamentosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DirectorioViewModel by viewModels()

    /** Torre elegida; null = lista de torres. */
    private val torre: String? get() = arguments?.getString(ARG_TORRE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDirectorioApartamentosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val torre = torre
        val recargar = { viewModel.fetchApartamentos() }
        if (torre == null) {
            configurarEncabezado(
                binding.encabezado, R.string.directorio_titulo, R.string.directorio_subtitulo,
                menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe, recargar)
            )
        } else {
            configurarEncabezado(
                binding.encabezado, getString(R.string.directorio_torre_titulo, torre),
                R.string.directorio_torre_subtitulo,
                menu = R.menu.menu_lista, alElegir = accionesDeLista(binding.swipe, recargar)
            )
        }

        binding.rvApartamentos.layoutManager = LinearLayoutManager(requireContext())
        binding.estado.vincular(binding.rvApartamentos)
        // P5: en Torres, buscar o filtrar muestra los apartamentos de todas las torres que coinciden
        barra = BarraBusqueda(this, binding.busqueda, R.string.busqueda_apartamentos, listOf(
            R.string.busqueda_todos to null, R.string.busqueda_arrendados to "ARRENDADOS"
        )) { viewModel.aptos.repetir() }
        barra.vincular(binding.swipe, binding.rvApartamentos)
        binding.swipe.setOnRefreshListener { recargar() }

        if (torre == null) mostrarTorres() else mostrarApartamentos(torre)
        // Al volver del detalle o girar la pantalla se reutiliza lo ya cargado, salvo que algo cambiara
        if (viewModel.aptos.value == null || viewModel.version != version) {
            viewModel.version = version
            recargar()
        }

        controlesFlotantes?.mostrarBotonFlotante(R.string.apartamento_agregar, R.drawable.ic_add) {
            pedirApartamento(null, torre) { nuevaTorre, apto, contrasena ->
                ejecutarAccion(getString(R.string.apartamento_creado, nuevaTorre, apto),
                    { RetrofitClient.instance.createApartamento(ApartamentoRequest(nuevaTorre, apto, contrasena)) }) {
                    huboCambios()
                    viewModel.version = version
                    recargar()
                }
            }
        }
    }

    private lateinit var barra: BarraBusqueda

    private fun coincide(a: Apartamento) =
        barra.coincide(a.torre, a.apto, "torre ${a.torre} apto ${a.apto}") && (barra.filtro == null || a.arrendado == true)

    private fun abrir(apto: Apartamento) {
        val bundle = Bundle().apply { putLong("aptoId", apto.id) }
        findNavController().navigate(R.id.action_directorioApartamentosFragment_to_detalleApartamentoFragment, bundle)
    }

    private fun mostrarTorres() {
        val encontrados = ApartamentoAdapter(soloNumero = false) { abrir(it) }
        val adaptador = TorreAdapter { elegida ->
            findNavController().navigate(
                R.id.action_directorioApartamentosFragment_to_torre,
                Bundle().apply { putString(ARG_TORRE, elegida.nombre) }
            )
        }
        binding.rvApartamentos.adapter = adaptador
        viewModel.aptos.observe(viewLifecycleOwner) { resource ->
            if (barra.activa || barra.filtro != null) {
                if (binding.rvApartamentos.adapter !== encontrados) binding.rvApartamentos.adapter = encontrados
                binding.estado.mostrarRecurso(
                    resource.map { aptos ->
                        aptos.filter { coincide(it) }.sortedWith(compareBy<Apartamento, String>(ORDEN_NATURAL) { it.torre }
                            .thenBy(ORDEN_NATURAL) { it.apto })
                    }, binding.swipe, R.drawable.ic_apartment,
                    getString(R.string.busqueda_sin_resultados), reintentar = { viewModel.fetchApartamentos() }
                ) { lista ->
                    encontrados.submitList(lista)
                    binding.encabezado.ponerInfo(resumen(R.plurals.resumen_apartamentos, lista.size))
                }
                return@observe
            }
            if (binding.rvApartamentos.adapter !== adaptador) binding.rvApartamentos.adapter = adaptador
            binding.estado.mostrarRecurso(
                resource.map { torresDe(it) }, binding.swipe, R.drawable.ic_apartment,
                getString(R.string.directorio_vacio), reintentar = { viewModel.fetchApartamentos() }
            ) { torres ->
                adaptador.submitList(torres)
                binding.encabezado.ponerInfo(resumen(
                    R.plurals.resumen_torres, torres.size,
                    R.plurals.resumen_apartamentos to torres.sumOf { it.apartamentos }
                ))
            }
        }
    }

    private fun mostrarApartamentos(torre: String) {
        val adaptador = ApartamentoAdapter(soloNumero = true) { abrir(it) }
        binding.rvApartamentos.adapter = adaptador
        viewModel.aptos.observe(viewLifecycleOwner) { resource ->
            val deLaTorre = resource.map { aptos ->
                aptos.filter { it.torre == torre && coincide(it) }.sortedWith(compareBy(ORDEN_NATURAL) { it.apto })
            }
            binding.estado.mostrarRecurso(
                deLaTorre, binding.swipe, R.drawable.ic_apartment,
                getString(if (barra.activa || barra.filtro != null) R.string.busqueda_sin_resultados else R.string.directorio_torre_vacia),
                reintentar = { viewModel.fetchApartamentos() }
            ) { lista ->
                adaptador.submitList(lista)
                binding.encabezado.ponerInfo(resumen(R.plurals.resumen_apartamentos, lista.size))
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_TORRE = "torre"

        // Sube al crear, editar o eliminar un apartamento: las listas ya abiertas se recargan
        private var version = 0
        fun huboCambios() { version++ }

        /** Números como números: "Torre 2" antes que "Torre 10"; si no son números, por texto. */
        val ORDEN_NATURAL = Comparator<String> { a, b ->
            val na = a.trim().toIntOrNull()
            val nb = b.trim().toIntOrNull()
            when {
                na != null && nb != null -> na.compareTo(nb)
                na != null -> -1
                nb != null -> 1
                else -> a.compareTo(b, ignoreCase = true)
            }
        }

        fun torresDe(aptos: List<Apartamento>): List<Torre> =
            aptos.groupBy { it.torre }
                .map { (nombre, deLaTorre) -> Torre(nombre, deLaTorre.size) }
                .sortedWith(compareBy(ORDEN_NATURAL) { it.nombre })
    }
}
