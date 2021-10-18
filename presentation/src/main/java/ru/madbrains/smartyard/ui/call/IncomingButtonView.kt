package ru.madbrains.smartyard.ui.call

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_incoming_call_button.view.*
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.R

class IncomingButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), Checkable {
    init {
        LayoutInflater.from(context)
            .inflate(R.layout.item_incoming_call_button, this, true)
        setupAttrs(context, attrs)
    }

    private var isCheckable: Boolean = false
    private var mCheckListener: listenerGeneric<Boolean>? = null
    private var bgChecked: Int = 0
    private var bgNormal: Int = 0
    private var SCREEN_ORIENTATION_LANDSCAPE = false
    var mChecked: Boolean = false

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
            mButton.setBackgroundResource(bgNormal)
            if (isCheckable) {
                mButton.setOnClickListener { toggle() }
            }
            if (SCREEN_ORIENTATION_LANDSCAPE) {
                mButtonText.isVisible = false
            }
            if (text != -1) {
                mButtonText.setText(text)
            }
            typedArray.recycle()
        }
    }

    fun setText(resId: Int) {
        mButtonText.setText(resId)
    }

    fun setText(text: String) {
        mButtonText.text = text
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        mButton.isSelected = selected
    }

    override fun setOnClickListener(l: OnClickListener?) {
        mButton.setOnClickListener(l)
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
        mButton.isSelected = checked
    }

    fun setOnCheckedChangeListener(listener: listenerGeneric<Boolean>) {
        mCheckListener = listener
    }
}
