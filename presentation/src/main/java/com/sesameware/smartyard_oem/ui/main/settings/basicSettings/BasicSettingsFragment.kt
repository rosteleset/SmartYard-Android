package com.sesameware.smartyard_oem.ui.main.settings.basicSettings

import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.R.drawable
import com.sesameware.smartyard_oem.databinding.FragmentBasicSettingsBinding
import com.sesameware.smartyard_oem.ui.SoundChooser
import com.sesameware.smartyard_oem.ui.firstCharacter
import com.sesameware.smartyard_oem.ui.main.settings.dialog.DialogChangeName
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity
import com.sesameware.smartyard_oem.ui.updateAllWidget
import org.koin.androidx.viewmodel.ext.android.viewModel
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
        binding.swShowNotify.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.setPushSetting(isChecked)
        }
        binding.sBalanse.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.setPushMoneySetting(isChecked)
        }
        binding.tvSoundChoose.setOnClickListener {
            SoundChooser.showSoundChooseIntent(
                this,
                RingtoneManager.TYPE_NOTIFICATION,
                null,
                mViewModel.mPreferenceStorage
            )
        }
        context?.let {
            val tone = SoundChooser.getChosenTone(
                it,
                RingtoneManager.TYPE_NOTIFICATION,
                null,
                mViewModel.mPreferenceStorage
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
            viewLifecycleOwner
        ) {
            activity?.finish()
            val intent = Intent(requireContext(), RegistrationActivity::class.java)
            startActivity(intent)
            updateAllWidget(requireContext())
        }

        binding.cvNotifications.isVisible = true

        // для Андроид версии 8.0 и выше отключаем настройку звука уведомлений,
        // так как для этого используются настройки категорий уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.soundTitle.isVisible = false
            binding.pdSound.isVisible = false
            binding.tvSoundChoose.isVisible = false
        }

        mViewModel.sentName.observe(
            viewLifecycleOwner
        ) {
            binding.tvUserName.text = "${it.name} ${firstCharacter(it.patronymic)}"
        }
        mViewModel.isPushSetting.observe(
            viewLifecycleOwner
        ) {
            binding.swShowNotify.isChecked = it
        }
        mViewModel.isPushMoneySetting.observe(
            viewLifecycleOwner
        ) {
            binding.sBalanse.isChecked = it
        }

        binding.tvAppInfo.text = resources.getString(R.string.app_info,
            resources.getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.FLAVOR_market,
            "${Build.MANUFACTURER} ${Build.MODEL}")
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
                mViewModel.logout(requireContext())
            }
            .setNegativeButton(resources.getString(R.string.setting_dialog_exit_no)) { _, _ ->
                return@setNegativeButton
            }.show()
    }
}
