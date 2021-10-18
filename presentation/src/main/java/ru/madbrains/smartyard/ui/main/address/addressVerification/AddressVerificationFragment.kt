package ru.madbrains.smartyard.ui.main.address.addressVerification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_address_verification.*
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.addressVerification.courier.CourierFragment
import ru.madbrains.smartyard.ui.main.address.addressVerification.office.OfficeFragment
import ru.madbrains.smartyard.ui.main.address.noNetwork.NoNetworkFragmentArgs

class AddressVerificationFragment : Fragment() {

    private var address = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            address = NoNetworkFragmentArgs.fromBundle(it).address
        }
        imageView7?.setOnClickListener {
            this.findNavController().popBackStack()
        }
        activity?.supportFragmentManager?.let { // todo: use getChildFragmentManager instead?
            val adapter = TabAdapter(it)
            adapter.addFragment(
                CourierFragment.getInstance(address),
                resources.getString(R.string.address_verification_tab_title_1)
            )
            adapter.addFragment(
                OfficeFragment.getInstance(address),
                resources.getString(R.string.address_verification_tab_title_2)
            )
            vpAddressVerification.adapter = adapter
            tlAddressVerification.setupWithViewPager(vpAddressVerification)
        }
    }
}
