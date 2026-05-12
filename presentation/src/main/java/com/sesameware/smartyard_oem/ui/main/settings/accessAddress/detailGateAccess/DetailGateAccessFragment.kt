package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessAddressViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessType
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose.DetailGateAccessScreen
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogShareAccess.DialogShareAccessDialog
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue
import com.sesameware.smartyard_oem.ui.showStandardAlert
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DetailGateAccessFragment : Fragment() {

    private val args: DetailGateAccessFragmentArgs by navArgs()
    private val mViewModel: AccessAddressViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                DetailGateAccessScreen(
                    viewModel = mViewModel,
                    flatId = args.flatId,
                    address = args.address,
                    clientId = args.clientId,
                    initialTab = args.initialTab,
                    initialPosition = args.initialPosition,
                    hasGates = args.hasGates,
                    onBackClick = { findNavController().popBackStack() },
                    onAddLicensePlateClick = {
                        createDialogShareAccessDialog(AccessType.GATE_BY_LICENSE_PLATE)
                    },
                    onAddPhoneClick = {
                        createDialogShareAccessDialog(AccessType.GATE_BY_PHONE)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.dialogToSuccessSms.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(
                    requireContext(),
                    resources.getString(R.string.dialog_sms),
                    null,
                    true
                )
            }
        )
    }

    private fun createDialogShareAccessDialog(type: AccessType) {
        DialogShareAccessDialog(type, ::onAddAccessByPhone, ::onAddAccessByLicensePlate)
            .show(parentFragmentManager, "")
    }

    private fun onAddAccessByPhone(contact: ContactModel, type: AccessType) {
        mViewModel.addRoommate(args.flatId, contact.number, type.roommateType)
    }

    private fun onAddAccessByLicensePlate(plate: LicensePlateValue) {
        mViewModel.addLicensePlate(args.flatId, plate)
    }
}

