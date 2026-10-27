package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessAddressViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue

/**
 * Dialog for adding or editing a license plate number using LPRS API version 2.
 * One number at a time, with country selection and an optional validity period.
 * When an existing license plate is passed, the dialog works in edit mode: the original number is
 * removed first and the updated one is added only after a successful removal.
 *
 * The legacy dialog [com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogShareAccess.DialogShareAccessDialog]
 * remains unchanged and continues to be used for version 1.
 */
class DialogLicensePlateV2 : DialogFragment() {

    private val mViewModel: AccessAddressViewModel by sharedViewModel()

    private val flatId: Int
        get() = requireArguments().getInt(ARG_FLAT_ID)

    // The license plate being edited, or null when adding a new one.
    private val licensePlate: LicensePlateValue?
        get() = requireArguments().run {
            getString(ARG_NUMBER)?.let { number ->
                LicensePlateValue(
                    number = number,
                    countryCode = getString(ARG_COUNTRY_CODE) ?: "ru",
                    validFrom = getString(ARG_VALID_FROM),
                    validTo = getString(ARG_VALID_TO),
                    watcherId = if (containsKey(ARG_WATCHER_ID)) getInt(ARG_WATCHER_ID) else null
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DialogLicensePlateV2Content(
                    viewModel = mViewModel,
                    flatId = flatId,
                    licensePlate = licensePlate,
                    onDismiss = { dismiss() }
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.loadSupportedPlateFormats()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9f).toInt()
            setLayout(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    companion object {
        private const val ARG_FLAT_ID = "flatId"
        private const val ARG_NUMBER = "number"
        private const val ARG_COUNTRY_CODE = "countryCode"
        private const val ARG_VALID_FROM = "validFrom"
        private const val ARG_VALID_TO = "validTo"
        private const val ARG_WATCHER_ID = "watcherId"

        /**
         * @param licensePlate the license plate to edit, or null to add a new one.
         */
        fun newInstance(flatId: Int, licensePlate: LicensePlateValue? = null): DialogLicensePlateV2 =
            DialogLicensePlateV2().apply {
                arguments = Bundle().apply {
                    putInt(ARG_FLAT_ID, flatId)
                    licensePlate?.let {
                        putString(ARG_NUMBER, it.number)
                        putString(ARG_COUNTRY_CODE, it.countryCode)
                        putString(ARG_VALID_FROM, it.validFrom)
                        putString(ARG_VALID_TO, it.validTo)
                        it.watcherId?.let { id -> putInt(ARG_WATCHER_ID, id) }
                    }
                }
            }
    }
}
