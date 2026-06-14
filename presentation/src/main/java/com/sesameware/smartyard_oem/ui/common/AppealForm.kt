package com.sesameware.smartyard_oem.ui.common

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getString
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import com.sesameware.data.DataModule
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FormAppealBinding
import com.sesameware.smartyard_oem.ui.regexInputFilter
import com.sesameware.smartyard_oem.ui.takeIfNotBlank
import com.sesameware.smartyard_oem.ui.toRegexOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppealForm @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), KoinComponent {
    private var _binding: FormAppealBinding? = null
    private val binding get() = _binding!!

    private val nameRegex: Regex? by lazy {
        DataModule.providerConfig.validationNamePattern.toRegexOrNull()
    }

    private val patronymicRegex: Regex? by lazy {
        DataModule.providerConfig.validationPatronymicPattern.toRegexOrNull()
    }

    private val lastRegex: Regex? by lazy {
        DataModule.providerConfig.validationLastPattern.toRegexOrNull()
    }

    private val userHasLastName
        get() = DataModule.providerConfig.userHasLastName

    private val namePattern
        get() = DataModule.providerConfig.validationNamePattern

    private val patronymicPattern
        get() = DataModule.providerConfig.validationPatronymicPattern

    private val lastPattern
        get() = DataModule.providerConfig.validationLastPattern

    private val privacyPolicy
        get() = DataModule.providerConfig.privacyPolicy


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
        isRegistration: Boolean = false,
        success: listenerEmpty
    ) {
        binding.nameText.addTextChangedListener {
            this.textChangeListener()
        }
        binding.patronymicText.addTextChangedListener {
            this.textChangeListener()
        }
        binding.lastText.addTextChangedListener {
            this.textChangeListener()
        }
        binding.lastText.isVisible = userHasLastName

        namePattern.takeIfNotBlank()?.let { binding.nameText.regexInputFilter(it) }
        patronymicPattern.takeIfNotBlank()?.let { binding.patronymicText.regexInputFilter(it) }
        lastPattern.takeIfNotBlank()?.let { binding.lastText.regexInputFilter(it) }

        mViewModel.prefsUserName?.let { userName ->
            binding.nameText.setText(userName.firstName)
            binding.patronymicText.setText(userName.patronymic)
            if (userHasLastName) binding.lastText.setText(userName.lastName)
            setupPrivacyPolicy(isRegistration)
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
                    binding.patronymicText.text.toString(),
                    binding.lastText.text.toString()
                ) {
                    success()
                }
            } else {
                toggleError(true, resId)
            }
        }
    }

    private fun setupPrivacyPolicy(isRegistration: Boolean) {
        val privacyPolicyCaption = privacyPolicy ?:
            HtmlCompat.fromHtml(
                getString(this@AppealForm.context, R.string.privacy_policy),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        val privacyPolicyFeatureIsEnabled = privacyPolicyCaption.isNotBlank()
        if (privacyPolicyFeatureIsEnabled && isRegistration) {
            binding.privacyPolicy.isVisible = true
            with (binding.privacyPolicyCaption) {
                text = privacyPolicyCaption
                movementMethod = LinkMovementMethod.getInstance()
            }
            binding.privacyPolicyCheckBox.setOnCheckedChangeListener { _, isChecked ->
                binding.btnDone.isEnabled = isChecked
            }
            binding.btnDone.isEnabled = false
        } else {
            binding.privacyPolicy.isGone = true
            binding.privacyPolicy.isEnabled = true
        }
    }

    private fun validate(): Int {
        val name = binding.nameText.text
        val patronymic = binding.patronymicText.text
        val last = binding.lastText.text

        nameRegex?.let { if (it.matches(name)) return R.string.appeal_validation_name_error }
        patronymicRegex?.let {
            if (it.matches(patronymic)) return R.string.appeal_validation_patronymic_error
        }

        if (userHasLastName) {
            lastRegex?.let {
                if (it.matches(last)) return R.string.appeal_validation_last_error
            }

            return when {
                name.isBlank() -> R.string.appeal_empty_name_error
                last.isBlank() -> R.string.appeal_empty_last_error
                else -> -1
            }
        } else {
            return when {
                name.isBlank() -> R.string.appeal_empty_name_error
                else -> -1
            }
        }
    }

    private fun toggleError(error: Boolean, @StringRes mesId: Int? = null) {
        if (error && mesId != null) {
            binding.tvError.visibility = VISIBLE
            binding.tvError.setText(mesId)
            if (mesId == R.string.common_do_authorization_on_another) {
                mViewModel.logout(context)
            }
        } else {
            binding.tvError.visibility = GONE
        }
    }
}
