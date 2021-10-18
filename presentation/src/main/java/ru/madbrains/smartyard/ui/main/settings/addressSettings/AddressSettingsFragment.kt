package ru.madbrains.smartyard.ui.main.settings.addressSettings

import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_address_settings.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.TF
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.string
import ru.madbrains.smartyard.ui.SoundChooser
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.settings.SettingsViewModel
import ru.madbrains.smartyard.ui.main.settings.accessAddress.dialogDeleteReason.DialogDeleteReasonFragment
import ru.madbrains.smartyard.ui.main.settings.accessAddress.dialogDeleteReason.DialogDeleteReasonFragment.OnGuestDeleteListener
import ru.madbrains.smartyard.ui.showStandardAlert
import ru.madbrains.smartyard.ui.webview_dialog.WebViewDialogFragment
import timber.log.Timber

class AddressSettingsFragment : Fragment() {

    private lateinit var mSetting: AddressSettingsFragmentArgs
    private val viewModel by viewModel<AddressSettingsViewModel>()

    private var flatId: Int = 0
    private var clientId: String = ""
    private var isKey: Boolean = false
    private var contractOwner: Boolean = false

    private val mAddressVM by sharedViewModel<AddressViewModel>()
    private val mSettingsVM by sharedViewModel<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_address_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireNotNull(arguments).let {
            mSetting = AddressSettingsFragmentArgs.fromBundle(it)
        }
        cvDeleteAddress.setOnClickListener {
            if (contractOwner) {
                val dialog = DialogDeleteReasonFragment()
                dialog.setTargetFragment(this, 0)
                dialog.onDeleteReasonListener =
                    object : OnGuestDeleteListener {

                        override fun onDismiss(dialog: DialogDeleteReasonFragment) {
                            dialog.dismiss()
                        }

                        override fun onShare(reasonText: String, reasonList: String) {
                            viewModel.createIssue(mSetting.address, reasonText, reasonList)
                            dialog.dismiss()
                        }
                    }
                dialog.show(parentFragmentManager, "")
            } else {
                showDialogDelete()
            }
        }
        tvSoundChoose.setOnClickListener {
            SoundChooser.showSoundChooseIntent(
                this,
                RingtoneManager.TYPE_RINGTONE,
                mSetting.flatId,
                viewModel.preferenceStorage
            )
        }
        context?.let {
            val tone = SoundChooser.getChosenTone(
                it,
                RingtoneManager.TYPE_RINGTONE,
                mSetting.flatId,
                viewModel.preferenceStorage
            )
            tvSoundChoose.text = tone.getToneTitle(it)
        }
        tvTitleNotif.setOnClickListener {
            if (expandableLayoutNotif.isExpanded) {
                expandableLayoutNotif.collapse()
                tvTitleNotif.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_bottom,
                    0
                )
            } else {
                expandableLayoutNotif.expand()
                tvTitleNotif.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_top,
                    0
                )
            }
        }

        tvWhiteRabbit.setOnClickListener {
            WebViewDialogFragment(string.help_white_rabbit).show(requireActivity().supportFragmentManager, "HelpWhiteRabbit")
        }

        tvAddressName.text = mSetting.address
        flatId = mSetting.flatId
        clientId = mSetting.clientId
        isKey = mSetting.isKey
        contractOwner = mSetting.contractOwner

        // Значение домофона
        cvNotification.isVisible = isKey
        ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        viewModel.getIntercom(flatId)

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(
                    requireContext(),
                    resources.getString(string.issue_dialog_caption_0),
                    null,
                    false
                )
            }
        )

        viewModel.intercom.observe(
            viewLifecycleOwner,
            {
                Timber.d("__Q__ observer intercom $it")

                switchIntercom.isChecked = it.cMS
                switchVoip.isChecked = it.voIP
                
                val paperBill = it.paperBill
                if (paperBill == null) {
                    tvPaperBill.isVisible = false
                    switchPaperBill.isVisible = false
                    vPaperBill.isVisible = false
                } else {
                    tvPaperBill.isVisible = true
                    switchPaperBill.isChecked = paperBill
                    switchPaperBill.isVisible = true
                    vPaperBill.isVisible = true
                }

                val useEventLog = it.disablePlog
                if (useEventLog == null) {
                    tvUseEventLog.isVisible = false
                    switchUseEventLog.isVisible = false
                    vUseEventLog.isVisible = false
                } else {
                    tvUseEventLog.isVisible = true
                    switchUseEventLog.isChecked = !useEventLog
                    switchUseEventLog.isVisible = true
                    vUseEventLog.isVisible = true
                }

                val ownerEventLog = it.hiddenPlog
                if (ownerEventLog == null) {
                    tvOwnerEventLog.isVisible = false
                    switchOwnerEventLog.isVisible = false
                    vOwnerEventLog.isVisible = false
                } else {
                    tvOwnerEventLog.isVisible = true
                    switchOwnerEventLog.isChecked = ownerEventLog
                    switchOwnerEventLog.isVisible = true
                    vOwnerEventLog.isVisible = true
                }

                val useFRS = it.frsDisabled
                if (useFRS == null) {
                    tvUseFRS.isVisible = false
                    ivUseFRSBeta.isVisible = false
                    switchUseFRS.isVisible = false
                    vUseFRS.isVisible = false
                } else {
                    if (!contractOwner) {
                        //не владелец квартиры, запрещаем переключатель распознавания лиц и делаем настройку попупрозрачной
                        tvUseFRS.alpha = FRS_DISABLED_ALPHA
                        ivUseFRSBeta.alpha = FRS_DISABLED_ALPHA
                        switchUseFRS.alpha = FRS_DISABLED_ALPHA
                        switchUseFRS.isEnabled = false
                        switchUseFRS.setOnCheckedChangeListener(null)
                    }

                    tvUseFRS.isVisible = true
                    ivUseFRSBeta.isVisible = true
                    switchUseFRS.isChecked = !useFRS
                    switchUseFRS.isVisible = true
                    vUseFRS.isVisible = true
                }

                switchWhiteRabbit.isChecked = (it.whiteRabbit > 0)
            }
        )

        viewModel.deleteRoommate.observe(
            viewLifecycleOwner,
            Observer {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_addressSettingsFragment_to_settingsFragment)
            }
        )

        switchIntercom.setOnCheckedChangeListener { compoundButton, check ->
            if (!compoundButton.isPressed) {
                return@setOnCheckedChangeListener
            }
            viewModel.putIntercom(
                flatId,
                null,
                if (check) TF.TRUE else TF.FALSE,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        }

        switchVoip.setOnCheckedChangeListener { _, check ->
            viewModel.putIntercom(
                flatId,
                null,
                null,
                if (check) TF.TRUE else TF.FALSE,
                null,
                null,
                null,
                null,
                null,
                null
            )
        }

        switchPaperBill.setOnCheckedChangeListener { _, isChecked ->
            viewModel.putIntercom(
                flatId,
                null,
                null,
                null,
                null,
                null,
                if (isChecked) TF.TRUE else TF.FALSE,
                null,
                null,
                null
            )
        }

        switchUseEventLog.setOnCheckedChangeListener { _, isChecked ->
            viewModel.putIntercom(
                flatId,
                null,
                null,
                null,
                null,
                null,
                null,
                if (isChecked) TF.FALSE else TF.TRUE,
                null,
                null
            )
            mAddressVM.nextListNoCache = true
            mSettingsVM.nextListNoCache = true
        }

        switchOwnerEventLog.setOnCheckedChangeListener { _, isChecked ->
            viewModel.putIntercom(
                flatId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                if (isChecked) TF.TRUE else TF.FALSE,
                null
            )
        }

        switchUseFRS.setOnCheckedChangeListener { _, isChecked ->
            viewModel.putIntercom(
                flatId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                if (isChecked) TF.FALSE else TF.TRUE
            )
        }

        switchWhiteRabbit.setOnCheckedChangeListener { _, isChecked ->
            viewModel.putIntercom(
                flatId,
                null,
                null,
                null,
                null,
                if (isChecked) WHITE_RABBIT_ON else WHITE_RABBIT_OFF,
                null,
                null,
                null,
                null
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SoundChooser.getDataFromIntent(context, requestCode, resultCode, data) { tone ->
            context?.let {
                tvSoundChoose.text = tone.getToneTitle(it)
                viewModel.saveSoundToPref(tone, mSetting.flatId)
            }
        }
    }

    private fun showDialogDelete() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setMessage(resources.getString(R.string.setting_dialog_delete_title))
            .setPositiveButton(resources.getString(R.string.setting_dialog_delete_yes)) { _, _ ->
                viewModel.deleteRooomate(flatId, clientId)
            }
            .setNegativeButton(resources.getString(R.string.setting_dialog_delete_no)) { _, _ ->
                returnTransition
            }.show()
    }

    companion object {
        const val WHITE_RABBIT_ON = 5
        const val WHITE_RABBIT_OFF = 0
        const val FRS_DISABLED_ALPHA = 0.4f
    }
}
