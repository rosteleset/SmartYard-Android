package com.sesameware.smartyard_oem.ui.call

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemIncomingCallButtonBinding

class IncomingButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), Checkable {
    private var _binding: ItemIncomingCallButtonBinding? = null
    private val binding get() = _binding!!

    init {
        _binding = ItemIncomingCallButtonBinding.inflate(LayoutInflater.from(context), this, true)
        setupAttrs(context, attrs)
    }

    private var isCheckable: Boolean = false
    private var mCheckListener: listenerGeneric<Boolean>? = null
    private var bgChecked: Int = 0
    private var bgNormal: Int = 0
    private var SCREEN_ORIENTATION_LANDSCAPE = false
    private var mChecked: Boolean = false

    private fun setupAttrs(context: Context, attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.item_incoming_call_button_attributes,
                0, 0
            )
            bgNormal = typedArray.getResourceId(
                R.styleable.item_incoming_call_button_attributes_buttonBG,
                -1
            )
            isCheckable = typedArray.getBoolean(
                R.styleable.item_incoming_call_button_attributes_buttonCheckable,
                false
            )
            val text = typedArray.getResourceId(
                R.styleable.item_incoming_call_button_attributes_android_text,
                -1
            )
            SCREEN_ORIENTATION_LANDSCAPE = typedArray.getBoolean(
                R.styleable.item_incoming_call_button_attributes_orientationLandscap,
                false
            )
            binding.mButton.setBackgroundResource(bgNormal)
            if (isCheckable) {
                binding.mButton.setOnClickListener { toggle() }
            }
            if (SCREEN_ORIENTATION_LANDSCAPE) {
                binding.mButtonText.isVisible = false
            }
            if (text != -1) {
                binding.mButtonText.setText(text)
            }
            typedArray.recycle()
        }
    }

    fun setText(resId: Int) {
        binding.mButtonText.setText(resId)
    }

    fun setText(text: String) {
        binding.mButtonText.text = text
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        binding.mButton.isSelected = selected
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.mButton.setOnClickListener(l)
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun setChecked(checked: Boolean) {
        if (isCheckable) {
            mChecked = checked
            mCheckListener?.run { this(checked) }
            updateDrawable(checked)
        }
    }

    private fun updateDrawable(checked: Boolean) {
        binding.mButton.isSelected = checked
    }

    fun setOnCheckedChangeListener(listener: listenerGeneric<Boolean>) {
        mCheckListener = listener
    }
}
