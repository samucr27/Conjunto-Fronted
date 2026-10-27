package com.example.conjuntoresidencial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.conjuntoresidencial.R

/**
 * Destino inicial del grafo y de cerrar sesión. Desde P5 cada app tiene una sola entrada, así que
 * no hay nada que elegir: lleva directo al inicio de sesión de esta app (la huella está ahí).
 */
class LoginSelectorFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        View(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findNavController().navigate(
            if (resources.getBoolean(R.bool.app_admin)) R.id.action_loginSelectorFragment_to_loginAdminFragment
            else R.id.action_loginSelectorFragment_to_loginApartamentoFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.loginSelectorFragment, true).build()
        )
    }
}
