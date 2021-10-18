package ru.madbrains.smartyard.ui.common

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.form_appeal.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import androidx.core.widget.addTextChangedListener
import ru.madbrains.domain.utils.listenerEmpty

class AppealForm @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), KoinComponent {

    private val mViewModel: AppealFormViewModel by inject()

    init {
        LayoutInflater.from(context).inflate(R.layout.form_appeal, this, true)
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
        nameText?.addTextChangedListener { this.textChangeListener() }

        mViewModel.sentName.observe(
            viewLifecycleOwner,
            Observer {
                nameText?.setText(it.name)
                patronymicText?.setText(it.patronymic)
            }
        )
        mViewModel.localErrorsSink.observe(
            viewLifecycleOwner,
            EventObserver { error ->
                toggleError(true, error.status.messageId)
            }
        )
        btnDone.setText(btnText)
        btnDone?.setOnClickListener {
            toggleError(false)
            if (validate()) {
                mViewModel.sendName(
                    nameText.text.toString(),
                    patronymicText.text.toString()
                ) { success() }
            } else {
                toggleError(true, R.string.appeal_validation_error)
            }
        }
    }

    private fun validate(): Boolean {
        return nameText.text.isNotEmpty()
    }

    private fun toggleError(error: Boolean, @StringRes mesId: Int? = null) {
        if (error && mesId != null) {
            tvError.visibility = View.VISIBLE
            tvError.setText(mesId)
        } else {
            tvError.visibility = View.GONE
        }
    }
}
