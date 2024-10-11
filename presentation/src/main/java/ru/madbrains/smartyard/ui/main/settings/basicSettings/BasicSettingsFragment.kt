package ru.madbrains.smartyard.ui.main.settings.basicSettings

import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.cancel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.drawable
import ru.madbrains.smartyard.databinding.FragmentBasicSettingsBinding
import ru.madbrains.smartyard.ui.SoundChooser
import ru.madbrains.smartyard.ui.firstCharacter
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment
import ru.madbrains.smartyard.ui.main.settings.dialog.DialogChangeName
import ru.madbrains.smartyard.ui.reg.RegistrationActivity
import ru.madbrains.smartyard.ui.requestSoundChouser
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber
import kotlin.coroutines.coroutineContext

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
//            this.findNavController().popBackStack()
            parentFragmentManager.popBackStack()
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
            if (requestSoundChouser(requireContext())){
                SoundChooser.showSoundChooseIntent(
                    this,
                    RingtoneManager.TYPE_NOTIFICATION,
                    null,
                    mViewModel.preferenceStorage
                )
            }
        }
        context?.let {
            try {
                val tone = SoundChooser.getChosenTone(
                    it,
                    RingtoneManager.TYPE_NOTIFICATION,
                    null,
                    mViewModel.preferenceStorage
                )
                binding.tvSoundChoose.text = tone.getToneTitle(it)
            }catch (_: Exception){
                binding.tvSoundChoose.text = "По умолчанию"
            }
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
                Timber.d("LOGOUTCOMOOM BassiSettingsFragment ACTIVITY FINISH $activity")
                activity?.finish()
//                NavHostFragment.findNavController(this)
//                    .navigate(BasicSettingsFragmentDirections.actionBasicSettingsFragmentToRegistrationActivity())
                val intent = Intent(activity, RegistrationActivity::class.java)
                startActivity(intent)
                updateAllWidget(requireContext())
            }
        )
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

    override fun onDestroy() {
        super.onDestroy()
    }
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
                mViewModel.clientLogout()
            }
            .setNegativeButton(resources.getString(R.string.setting_dialog_exit_no)) { _, _ ->
                return@setNegativeButton
            }.show()
    }
}
