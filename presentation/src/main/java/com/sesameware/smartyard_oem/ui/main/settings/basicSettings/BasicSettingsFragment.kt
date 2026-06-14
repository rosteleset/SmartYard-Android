package com.sesameware.smartyard_oem.ui.main.settings.basicSettings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.NightMode
import com.sesameware.domain.model.response.CCTVViewTypeType
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.MessagingService
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentBasicSettingsBinding
import com.sesameware.smartyard_oem.ui.SoundChooser
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.firstCharacter
import com.sesameware.smartyard_oem.ui.main.settings.dialog.DialogChangeName
import com.sesameware.smartyard_oem.ui.main.settings.dialog.SelectThemeBottomSheetFragment
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity
import com.sesameware.smartyard_oem.ui.updateAllWidget
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class BasicSettingsFragment : Fragment() {
    private var _binding: FragmentBasicSettingsBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<BasicSettingsViewModel>()

    private var isRtl: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            REQUEST_NIGHT_MODE, this
        ) { _, bundle ->
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(NIGHT_MODE_VALUE, NightMode::class.java)
            } else {
                bundle.getParcelable(NIGHT_MODE_VALUE) as? NightMode
            } ?: return@setFragmentResultListener
            mViewModel.setNightMode(mode)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasicSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        binding.scrollRoot.applyBottomNavInsetsToPadding()

        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        binding.cvExit.setOnClickListener {
            showDialog()
        }
        binding.swShowNotify.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.setPushSetting(isChecked)
        }
        binding.sBalance.setOnCheckedChangeListener { _, isChecked ->
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

        binding.tvTitleDomophone.setOnClickListener {
            if (binding.expandableLayoutNotif.isExpanded) {
                binding.expandableLayoutNotif.collapse()
                binding.tvTitleDomophone.setArrowDown()
            } else {
                binding.expandableLayoutNotif.expand()
                binding.tvTitleDomophone.setArrowUp()
            }
        }
        binding.swShowOnMap.isChecked = mViewModel.mPreferenceStorage.showCamerasOnMap
        binding.swShowOnMap.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.saveShowOnMapPref(isChecked)
        }

        binding.tvCallRingtone.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showChannelSetup()
            }
        }
        binding.ivCallRingtone.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showChannelSetup()
            }
        }

        binding.tvTitleCameras.setOnClickListener {
            if (binding.expandableLayoutCameras.isExpanded) {
                binding.expandableLayoutCameras.collapse()
                binding.tvTitleCameras.setArrowDown()
            } else {
                binding.expandableLayoutCameras.expand()
                binding.tvTitleCameras.setArrowUp()
            }
        }

        binding.tvTitleSecurity.setOnClickListener {
            if (binding.expandableLayoutSecurity.isExpanded) {
                binding.expandableLayoutSecurity.collapse()
                binding.tvTitleSecurity.setArrowDown()
            } else {
                binding.expandableLayoutSecurity.expand()
                binding.tvTitleSecurity.setArrowUp()
            }
        }

        binding.ivNameEdit.setOnClickListener {
            val dialog = DialogChangeName()
            dialog.onSuccess = { mViewModel.refreshUserData() }
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

        binding.llCameras.isVisible = (DataModule.providerConfig.cctvView == CCTVViewTypeType.USER_DEFINED)

        // для Андроид версии 8.0 и выше отключаем настройку звука уведомлений,
        // так как для этого используются настройки категорий уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.gSound.isVisible = false
        }else {
            binding.gCallRingtone.isVisible = false
        }


        binding.tvTitleTheme.isVisible =
            requireContext().resources.getBoolean(R.bool.feature_night_mode_is_enabled)
        binding.tvTitleTheme.setOnClickListener {
            SelectThemeBottomSheetFragment().show(childFragmentManager, "DialogSelectThemeFragment")
        }

        mViewModel.userName.observe(
            viewLifecycleOwner
        ) {
            val text = if (DataModule.providerConfig.userHasLastName) {
                val patronymic = if (it.patronymic.isNotBlank()) "${it.patronymic}\n" else ""
                "${it.firstName}\n$patronymic${it.lastName}"
            } else {
                "${it.firstName} ${firstCharacter(it.patronymic)}"
            }
            binding.tvUserName.text = text
        }

        mViewModel.userPhone.observe(
            viewLifecycleOwner
        ) {
            if (it.isEmpty()) {
                binding.tvUserPhone.text = getString(R.string.phone_number_not_saved)
            } else {
                binding.tvUserPhone.text = it
            }
        }

        mViewModel.isPushSetting.observe(
            viewLifecycleOwner
        ) {
            binding.swShowNotify.isChecked = it
        }

        mViewModel.isPushMoneySetting.observe(
            viewLifecycleOwner
        ) {
            binding.sBalance.isChecked = it
        }

        binding.tvAppInfo.text = resources.getString(R.string.app_info,
            resources.getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.FLAVOR_market,
            "${Build.MANUFACTURER} ${Build.MODEL}")
    }

    private fun TextView.setArrowDown() {
        setArrow(this, R.drawable.ic_arrow_bottom)
    }

    private fun TextView.setArrowUp() {
        setArrow(this, R.drawable.ic_arrow_top)
    }

    private fun setArrow(tv: TextView, @DrawableRes arrowRes: Int) {
        val arrow = ContextCompat.getDrawable(requireContext(), arrowRes)
        tv.setCompoundDrawablesWithIntrinsicBounds(
            if (isRtl) arrow else null, null,
            if (!isRtl) arrow else null, null,
        )
        tv.invalidate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showChannelSetup() {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().applicationContext.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, MessagingService.CHANNEL_CALLS_ID)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {

        }
    }

    companion object {
        const val REQUEST_NIGHT_MODE = "RequestNightMode"
        const val NIGHT_MODE_VALUE = "NightModeValue"
    }
}
