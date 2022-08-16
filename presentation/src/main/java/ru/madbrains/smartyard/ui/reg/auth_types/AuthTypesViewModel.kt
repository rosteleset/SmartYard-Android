package ru.madbrains.smartyard.ui.reg.auth_types

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.reg.tel.NumberRegFragment

class AuthTypesViewModel : GenericViewModel() {
    fun goToNext(fragment: Fragment, methodId: String) {
        NavHostFragment.findNavController(fragment).navigate(
            R.id.action_authTypesFragment_to_numberRegFragment,
            bundleOf(NumberRegFragment.KEY_AUTH_METHOD_ID to methodId)
        )
    }
}
