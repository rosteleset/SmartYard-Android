package com.sesameware.smartyard_oem.ui.main.settings.addressSettings

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
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.data.DataModule
import com.sesameware.domain.model.TF
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.R.string
import com.sesameware.smartyard_oem.databinding.FragmentAddressSettingsBinding
import com.sesameware.smartyard_oem.ui.SoundChooser
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
import com.sesameware.smartyard_oem.ui.main.settings.SettingsViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogDeleteReason.DialogDeleteReasonFragment
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogDeleteReason.DialogDeleteReasonFragment.OnGuestDeleteListener
import com.sesameware.smartyard_oem.ui.showStandardAlert
import com.sesameware.smartyard_oem.ui.webview_dialog.WebViewDialogFragment

class AddressSettingsFragment : Fragment() {
    private var _binding: FragmentAddressSettingsBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = FragmentAddressSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireNotNull(arguments).let {
            mSetting = AddressSettingsFragmentArgs.fromBundle(it)
        }
        binding.cvDeleteAddress.setOnClickListener {
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
        binding.tvSoundChoose.setOnClickListener {
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
            binding.tvSoundChoose.text = tone.getToneTitle(it)
        }
        binding.tvTitleNotif.setOnClickListener {
            if (binding.expandableLayoutNotif.isExpanded) {
                binding.expandableLayoutNotif.collapse()
                binding.tvTitleNotif.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_bottom,
                    0
                )
            } else {
                binding.expandableLayoutNotif.expand()
                binding.tvTitleNotif.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_top,
                    0
                )
            }
        }

        binding.tvWhiteRabbit.setOnClickListener {
            WebViewDialogFragment(string.help_white_rabbit).show(requireActivity().supportFragmentManager, "HelpWhiteRabbit")
        }

        binding.tvAddressName.text = mSetting.address
        flatId = mSetting.flatId
        clientId = mSetting.clientId
        isKey = mSetting.isKey
        contractOwner = mSetting.contractOwner

        // Значение домофона
        binding.cvNotification.isVisible = isKey
        binding.ivBack.setOnClickListener {
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
            viewLifecycleOwner
        ) {
            binding.switchIntercom.isChecked = it.cMS
            binding.switchVoip.isChecked = it.voIP

            val paperBill = it.paperBill
            if (paperBill == null) {
                binding.tvPaperBill.isVisible = false
                binding.switchPaperBill.isVisible = false
                binding.vPaperBill.isVisible = false
            } else {
                binding.tvPaperBill.isVisible = true
                binding.switchPaperBill.isChecked = paperBill
                binding.switchPaperBill.isVisible = true
                binding.vPaperBill.isVisible = true
            }

            val useEventLog = it.disablePlog
            if (useEventLog == null) {
                binding.tvUseEventLog.isVisible = false
                binding.switchUseEventLog.isVisible = false
                binding.vUseEventLog.isVisible = false
            } else {
                binding.tvUseEventLog.isVisible = true
                binding.switchUseEventLog.isChecked = !useEventLog
                binding.switchUseEventLog.isVisible = true
                binding.vUseEventLog.isVisible = true
            }

            val ownerEventLog = it.hiddenPlog
            if (ownerEventLog == null) {
                binding.tvOwnerEventLog.isVisible = false
                binding.switchOwnerEventLog.isVisible = false
                binding.vOwnerEventLog.isVisible = false
            } else {
                binding.tvOwnerEventLog.isVisible = true
                binding.switchOwnerEventLog.isChecked = ownerEventLog
                binding.switchOwnerEventLog.isVisible = true
                binding.vOwnerEventLog.isVisible = true
            }

            val useFRS = it.frsDisabled
            if (!DataModule.providerConfig.hasFRS || useFRS == null) {
                binding.tvUseFRS.isVisible = false
                binding.ivUseFRSBeta.isVisible = false
                binding.switchUseFRS.isVisible = false
                binding.vUseFRS.isVisible = false
            } else {
                if (!contractOwner) {
                    //не владелец квартиры, запрещаем переключатель распознавания лиц и делаем настройку попупрозрачной
                    binding.tvUseFRS.alpha = FRS_DISABLED_ALPHA
                    binding.ivUseFRSBeta.alpha = FRS_DISABLED_ALPHA
                    binding.switchUseFRS.alpha = FRS_DISABLED_ALPHA
                    binding.switchUseFRS.isEnabled = false
                    binding.switchUseFRS.setOnCheckedChangeListener(null)
                }

                binding.tvUseFRS.isVisible = true
                binding.ivUseFRSBeta.isVisible = true
                binding.switchUseFRS.isChecked = !useFRS
                binding.switchUseFRS.isVisible = true
                binding.vUseFRS.isVisible = true
            }

            binding.switchWhiteRabbit.isChecked = (it.whiteRabbit > 0)
        }

        viewModel.deleteRoommate.observe(
            viewLifecycleOwner,
            Observer {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_addressSettingsFragment_to_settingsFragment)
            }
        )

        binding.switchIntercom.setOnCheckedChangeListener { compoundButton, check ->
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

        binding.switchVoip.setOnCheckedChangeListener { _, check ->
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

        binding.switchPaperBill.setOnCheckedChangeListener { _, isChecked ->
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

        binding.switchUseEventLog.setOnCheckedChangeListener { _, isChecked ->
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

        binding.switchOwnerEventLog.setOnCheckedChangeListener { _, isChecked ->
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

        binding.switchUseFRS.setOnCheckedChangeListener { _, isChecked ->
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

        binding.switchWhiteRabbit.setOnCheckedChangeListener { _, isChecked ->
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

        binding.switchUseSpeaker.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveSpeakerFlag(flatId, isChecked)
        }

        binding.switchUseSpeaker.isChecked = (viewModel.preferenceStorage.addressOptions.getOption(flatId).isSpeaker == true)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SoundChooser.getDataFromIntent(context, requestCode, resultCode, data) { tone ->
            context?.let {
                binding.tvSoundChoose.text = tone.getToneTitle(it)
                viewModel.saveSoundToPref(tone, mSetting.flatId)
            }
        }
    }

    private fun showDialogDelete() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setMessage(resources.getString(R.string.setting_dialog_delete_title))
            .setPositiveButton(resources.getString(R.string.setting_dialog_delete_yes)) { _, _ ->
                viewModel.deleteRoommate(flatId, clientId)
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
