package ru.madbrains.smartyard.ui.main.address.addressVerification.courier

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.databinding.FragmentCourierBinding
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment
import ru.madbrains.smartyard.ui.main.address.addressVerification.AddressVerificationFragment
import timber.log.Timber

class CourierFragment : Fragment() {
    private var _binding: FragmentCourierBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<CourierViewModel>()
    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()

    private var address = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObserve()

        arguments?.let {
            address = it.getString(ADDRESS_FIELD, "")
        }

        binding.btnOk.setOnClickListener {
            viewModel.createIssue(address)
        }
    }

    private fun setupObserve() {
        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                val fragmentCount = parentFragmentManager.backStackEntryCount
                for (i in 0 until  fragmentCount){
                parentFragmentManager.popBackStack()
                }
                mainActivityViewModel.bottomNavigateToIntercom()
                val intentBroadcast = Intent(AddressWebViewFragment.REFRESH_INTENT)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intentBroadcast)
                (activity as MainActivity?)?.reloadToAddress()
            }
        )
    }

    companion object {
        fun getInstance(address: String): CourierFragment {
            val courierFragment = CourierFragment()
            val bundle = Bundle().apply {
                putString(ADDRESS_FIELD, address)
            }
            return courierFragment.apply {
                arguments = bundle
            }
        }
        const val ADDRESS_FIELD = "address_field"
    }
}
