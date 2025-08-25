package com.sesameware.smartyard_oem.ui.common

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import com.sesameware.data.DataModule
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FormAppealBinding
import com.sesameware.smartyard_oem.ui.regexInputFilter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppealForm @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), KoinComponent {
    private var _binding: FormAppealBinding? = null
    private val binding get() = _binding!!

    private val mViewModel: AppealFormViewModel by inject()

    init {
        _binding = FormAppealBinding.inflate(LayoutInflater.from(context) ,this, true)
    }

    private fun textChangeListener() {
        toggleError(false)
    }

    fun initialize(
        viewLifecycleOwner: LifecycleOwner,
        btnText: Int,
        arguments: Bundle? = null,
        success: listenerEmpty
    ) {
        mViewModel.loadName(arguments)
        binding.nameText.addTextChangedListener {
            this.textChangeListener()
        }
        if (DataModule.providerConfig.validationNamePattern.isNotEmpty()) {
            binding.nameText.regexInputFilter(DataModule.providerConfig.validationNamePattern)
        }
        if (DataModule.providerConfig.validationPatronymicPattern.isNotEmpty()) {
            binding.patronymicText.regexInputFilter(DataModule.providerConfig.validationPatronymicPattern)
        }

        mViewModel.sentName.observe(
            viewLifecycleOwner
        ) {
            binding.nameText.setText(it.name)
            binding.patronymicText.setText(it.patronymic)
        }
        mViewModel.localErrorsSink.observe(
            viewLifecycleOwner,
            EventObserver { error ->
                toggleError(true, error.status.messageId)
            }
        )
        binding.btnDone.setText(btnText)
        binding.btnDone.setOnClickListener {
            toggleError(false)
            val resId = validate()
            if (resId == -1) {
                mViewModel.sendName(
                    binding.nameText.text.toString(),
                    binding.patronymicText.text.toString()
                ) {
                    success()
                }
            } else {
                toggleError(true, resId)
            }
        }
    }

    private fun validate(): Int {
        if (DataModule.providerConfig.validationNamePattern.isNotEmpty()) {
            if (!Regex(DataModule.providerConfig.validationNamePattern).matches(binding.nameText.text)) {
                return R.string.appeal_validation_name_error
            }
        }
        if (DataModule.providerConfig.validationPatronymicPattern.isNotEmpty()) {
            if (!Regex(DataModule.providerConfig.validationPatronymicPattern).matches(binding.patronymicText.text)) {
                return R.string.appeal_validation_patronymic_error
            }
        }

        return if (binding.nameText.text.isNotEmpty()) -1 else R.string.appeal_validation_name_error
    }

    private fun toggleError(error: Boolean, @StringRes mesId: Int? = null) {
        if (error && mesId != null) {
            binding.tvError.visibility = VISIBLE
            binding.tvError.setText(mesId)
            if (mesId == com.sesameware.domain.R.string.common_do_authorization_on_another) {
                mViewModel.logout(context)
            }
        } else {
            binding.tvError.visibility = GONE
        }
    }
}
