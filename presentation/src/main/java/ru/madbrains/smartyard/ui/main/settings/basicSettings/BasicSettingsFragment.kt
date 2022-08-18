package ru.madbrains.smartyard.ui.main.settings.basicSettings

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
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.data.DataModule
import ru.madbrains.domain.model.response.ProviderConfig
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.drawable
import ru.madbrains.smartyard.databinding.FragmentBasicSettingsBinding
import ru.madbrains.smartyard.ui.SoundChooser
import ru.madbrains.smartyard.ui.firstCharacter
import ru.madbrains.smartyard.ui.main.settings.dialog.DialogChangeName
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber

class BasicSettingsFragment : Fragment() {
    private var _binding: FragmentBasicSettingsBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<BasicSettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasicSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        binding.cvExit.setOnClickListener {
            showDialog()
        }
        binding.swShowNotify.setOnCheckedChangeListener { buttonView, isChecked ->
            mViewModel.setPushSetting(isChecked)
        }
        binding.sBalanse.setOnCheckedChangeListener { buttonView, isChecked ->
            mViewModel.setPushMoneySetting(isChecked)
        }
        binding.tvSoundChoose.setOnClickListener {
            SoundChooser.showSoundChooseIntent(
                this,
                RingtoneManager.TYPE_NOTIFICATION,
                null,
                mViewModel.preferenceStorage
            )
        }
        context?.let {
            val tone = SoundChooser.getChosenTone(
                it,
                RingtoneManager.TYPE_NOTIFICATION,
                null,
                mViewModel.preferenceStorage
            )
            binding.tvSoundChoose.text = tone.getToneTitle(it)
        }

        binding.tvTitleNotif.setOnClickListener {
            if (binding.expandableLayoutNotif.isExpanded) {
                binding.expandableLayoutNotif.collapse()
                binding.ivNotif.setImageResource(drawable.ic_arrow_bottom)
            } else {
                binding.expandableLayoutNotif.expand()
                binding.ivNotif.setImageResource(drawable.ic_arrow_top)
            }
        }
        binding.tvTitleSecurity.setOnClickListener {
            if (binding.expandableLayoutSecurity.isExpanded) {
                binding.expandableLayoutSecurity.collapse()
                binding.ivSecurity.setImageResource(drawable.ic_arrow_bottom)
            } else {
                binding.expandableLayoutSecurity.expand()
                binding.ivSecurity.setImageResource(drawable.ic_arrow_top)
            }
        }

        binding.ivNameEdit.setOnClickListener {
            val dialog = DialogChangeName()
            dialog.onSuccess = { mViewModel.refreshSendName() }
            dialog.show(parentFragmentManager, "")
        }
        mViewModel.logout.observe(
            viewLifecycleOwner,
            Observer {
                NavHostFragment.findNavController(this)
                    .navigate(BasicSettingsFragmentDirections.actionBasicSettingsFragmentToRegistrationActivity())
                activity?.finish()
                updateAllWidget(requireContext())
            }
        )

        if (DataModule.providerConfig.mainMenu?.contains(ProviderConfig.MAIN_MENU_NOTIFICATIONS) == false) {
            binding.cvNotifications.isVisible = false
        }

        mViewModel.sentName.observe(
            viewLifecycleOwner
        ) {
            binding.tvUserName.text = "${it.name} ${firstCharacter(it.patronymic)}"
        }
        mViewModel.isPushSetting.observe(
            viewLifecycleOwner,
            Observer {
                binding.swShowNotify.isChecked = it
            }
        )
        mViewModel.isPushMoneySetting.observe(
            viewLifecycleOwner,
            Observer {
                binding.sBalanse.isChecked = it
            }
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("debug_sound $resultCode")
        SoundChooser.getDataFromIntent(context, requestCode, resultCode, data) { tone ->
            context?.let {
                binding.tvSoundChoose.text = tone.getToneTitle(it)
                mViewModel.saveSoundToPref(tone)
            }
        }
    }

    private fun showDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setTitle(R.string.setting_dialog_exit_caption)
            .setMessage(R.string.setting_dialog_exit_caption_msg)
            .setPositiveButton(resources.getString(R.string.setting_dialog_exit_yes)) { _, _ ->
                mViewModel.logout()
            }
            .setNegativeButton(resources.getString(R.string.setting_dialog_exit_no)) { _, _ ->
                return@setNegativeButton
            }.show()
    }
}
