package ru.madbrains.smartyard.ui.main.address.auth.offerta

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.databinding.FragmentAcceptOffertaBinding
import ru.madbrains.smartyard.ui.main.address.auth.AuthViewModel
import ru.madbrains.smartyard.ui.player.ExoPlayerTabsAdapter
import ru.madbrains.smartyard.ui.player.TimeLineAdapter
import timber.log.Timber


class AcceptOffertaFragment : Fragment() {
    lateinit var binding: FragmentAcceptOffertaBinding
    private val mAuthViewModel by sharedViewModel<AuthViewModel>()
    private var contractNumber: String? = null
    private var password: String? = null
    private var houseId: Int? = null
    private var flat: Int? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAcceptOffertaBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            houseId = it.getInt("houseId")
            flat = it.getInt("flat")

            contractNumber = it.getString("contractNumber")
            password = it.getString("password")
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeOffertaList()
        binding.btAcceptOfferta.setOnClickListener {
            if (contractNumber != null){
                acceptOfferta(contractNumber ?: "", password ?: "")
            }else{
                if (houseId != null){
                    acceptOffertaByAddress(houseId ?: -1, flat ?: -1)
                }
            }
        }
    }

    private fun observeOffertaList() {
        mAuthViewModel.offertaList.observe(viewLifecycleOwner) { offertaList ->
            if (offertaList.isNotEmpty()) {
                setupAdapter()
            }
        }
        mAuthViewModel.isOffertaApply.observe(viewLifecycleOwner){
            binding.btAcceptOfferta.isEnabled = it
        }
    }

    private fun setupAdapter() {
        mAuthViewModel.offertaList.value?.let {
            val adapter = AcceptOffertaAdapter(it, requireContext(), mAuthViewModel)
            binding.rvOffertaAccept.adapter = adapter
        }
    }

    private fun acceptOfferta(login: String, password: String){
        mAuthViewModel.acceptOfferta(login, password)
    }

    private fun acceptOffertaByAddress(houseId: Int, flat: Int){
        mAuthViewModel.acceptOffertaByAddress(houseId, flat)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthViewModel.setOffertaApply(false)
    }
}