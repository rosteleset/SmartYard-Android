package com.sesameware.smartyard_oem.ui.common

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import androidx.core.widget.addTextChangedListener
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.databinding.FormAppealBinding

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
            if (validate()) {
                mViewModel.sendName(
                    binding.nameText.text.toString(),
                    binding.patronymicText.text.toString()
                ) {
                    success()
                }
            } else {
                toggleError(true, R.string.appeal_validation_error)
            }
        }
    }

    private fun validate(): Boolean {
        return binding.nameText.text.isNotEmpty()
    }

    private fun toggleError(error: Boolean, @StringRes mesId: Int? = null) {
        if (error && mesId != null) {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.setText(mesId)
        } else {
            binding.tvError.visibility = View.GONE
        }
    }
}
