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
import kotlinx.android.synthetic.main.fragment_basic_settings.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.drawable
import ru.madbrains.smartyard.R.layout
import ru.madbrains.smartyard.ui.SoundChooser
import ru.madbrains.smartyard.ui.firstCharacter
import ru.madbrains.smartyard.ui.main.settings.dialog.DialogChangeName
import ru.madbrains.smartyard.ui.updateAllWidget

class BasicSettingsFragment : Fragment() {

    private val mViewModel by viewModel<BasicSettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layout.fragment_basic_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        cvExit.setOnClickListener {
            showDialog()
        }
        swShowNotify.setOnCheckedChangeListener { buttonView, isChecked ->
            mViewModel.setPushSetting(isChecked)
        }
        sBalanse.setOnCheckedChangeListener { buttonView, isChecked ->
            mViewModel.setPushMoneySetting(isChecked)
        }
        tvSoundChoose.setOnClickListener {
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
            tvSoundChoose.text = tone.getToneTitle(it)
        }

        tvTitleNotif.setOnClickListener {
            if (expandableLayoutNotif.isExpanded) {
                expandableLayoutNotif.collapse()
                ivNotif.setImageResource(drawable.ic_arrow_bottom)
            } else {
                expandableLayoutNotif.expand()
                ivNotif.setImageResource(drawable.ic_arrow_top)
            }
        }
        tvTitleSecurity.setOnClickListener {
            if (expandableLayoutSecurity.isExpanded) {
                expandableLayoutSecurity.collapse()
                ivSecurity.setImageResource(drawable.ic_arrow_bottom)
            } else {
                expandableLayoutSecurity.expand()
                ivSecurity.setImageResource(drawable.ic_arrow_top)
            }
        }

        ivNameEdit.setOnClickListener {
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
        mViewModel.sentName.observe(
            viewLifecycleOwner,
            {
                tvUserName.text = "${it.name} ${firstCharacter(it.patronymic)}"
            }
        )
        mViewModel.isPushSetting.observe(
            viewLifecycleOwner,
            Observer {
                swShowNotify.isChecked = it
            }
        )
        mViewModel.isPushMoneySetting.observe(
            viewLifecycleOwner,
            Observer {
                sBalanse.isChecked = it
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SoundChooser.getDataFromIntent(context, requestCode, resultCode, data) { tone ->
            context?.let {
                tvSoundChoose.text = tone.getToneTitle(it)
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
