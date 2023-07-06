package com.sesameware.smartyard_oem.ui.reg.tel

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentNumberRegBinding
import com.sesameware.smartyard_oem.databinding.PinEntryBinding
import com.sesameware.smartyard_oem.ui.dpToPx

class NumberRegFragment : Fragment() {
    private var _binding: FragmentNumberRegBinding? = null
    private val binding get() = _binding!!

    private var mPhonePrefix: String = ""
    private var mPhoneNumber: String = ""
    private val mViewModel by viewModel<NumberRegViewModel>()

    private var pinSlots = mutableListOf<PinEntryBinding>()
    private var pinSlotSizes = mutableListOf<Int>()
    private var pinCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNumberRegBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvProviderNR.text = DataModule.providerName

        binding.ivExit.setOnClickListener {
            activity?.finish()
        }

        if (DataModule.BASE_URL.isEmpty()) {
            binding.tvBackToProviders.setOnClickListener {
                findNavController().popBackStack(R.id.providersFragment, false)
            }
        } else {
            binding.tvBackToProviders.visibility = View.INVISIBLE
        }

        mViewModel.localErrorsSink.observe(
            viewLifecycleOwner,
            EventObserver { error ->
                /*if (error.status == ErrorStatus.TOO_MANY_REQUESTS) {
                    mViewModel.goToNext(mPhonePrefix + mPhoneNumber, this)
                } else {
                    toggleError(true, error.status.messageId)
                }*/
                toggleError(true, error.status.messageId)
            }
        )

        createFromTemplate()
    }

    private fun createFromTemplate() {
        val q = Regex("""^\+?(\d+)\s*(.*)""").find(DataModule.phonePattern)
        if (q?.groups?.size != 3) {
            return
        }

        var pattern = ""
        q.groupValues[2].forEachIndexed { index, c ->
            pattern += if (c == '#') c else ' '
        }
        if (pattern.isEmpty()) {
            return
        }

        mPhonePrefix = q.groupValues[1]
        if (mPhonePrefix.isNotEmpty()) {
            binding.textView.text = "+" + mPhonePrefix
        } else {
            binding.textView.visibility = View.INVISIBLE
        }

        pattern.split(' ').forEach {
            if (it.isNotEmpty()) {
                val d = PinEntryBinding.inflate(LayoutInflater.from(requireContext()))
                d.peeSlot.setMaxLength(it.length)
                d.peeSlot.isFocusable = false
                d.peeSlot.isFocusableInTouchMode = false
                val lp = d.peeSlot.layoutParams as FrameLayout.LayoutParams
                lp.width = (it.length * dpToPx(25)).toInt()
                d.peeSlot.layoutParams = lp
                binding.llPhone.addView(d.root)
                pinSlots.add(d)
                pinSlotSizes.add(it.length)
                pinCount += it.length
            }
        }

        if (pinSlots.isNotEmpty()) {
            pinSlots[0].peeSlot.isFocusable = true
            pinSlots[0].peeSlot.isFocusableInTouchMode = true
            pinSlots[0].peeSlot.focus()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(pinSlots[0].peeSlot, InputMethodManager.SHOW_IMPLICIT)

            pinSlots.forEachIndexed { index, pinSlot ->
                pinSlot.peeSlot.addTextChangedListener {
                    checkToSmsReg()

                    if (index < pinSlots.size - 1) {
                        if (it?.length == pinSlotSizes[index]) {
                            pinSlots[index + 1].peeSlot.isFocusable = true
                            pinSlots[index + 1].peeSlot.isFocusableInTouchMode = true
                            pinSlots[index + 1].peeSlot.requestFocus()
                            pinSlots[index].peeSlot.isFocusable = false
                            pinSlots[index].peeSlot.isFocusableInTouchMode = false
                        }
                    }
                }

                if (index > 0) {
                    pinSlot.peeSlot.setOnKeyListener { _, keyCode, event ->
                        var r = false

                        //удаление символа в пустом слоте должно активировать предыдущий слот и удалить там номер
                        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && pinSlot.peeSlot.text?.isEmpty() == true) {
                            val text: String = pinSlots[index - 1].peeSlot.text.toString()
                            if (text.isNotEmpty()) {
                                pinSlots[index - 1].peeSlot.setText(text.substring(0, text.length - 1))
                                pinSlots[index - 1].peeSlot.setSelection(text.length - 1)
                            }
                            pinSlots[index - 1].peeSlot.isFocusable = true
                            pinSlots[index - 1].peeSlot.isFocusableInTouchMode = true
                            pinSlots[index - 1].peeSlot.requestFocus()
                            pinSlots[index].peeSlot.isFocusable = false
                            pinSlots[index].peeSlot.isFocusableInTouchMode = false
                            r = true
                        }

                        r
                    }
                }
            }
        }
    }

    private fun checkToSmsReg() {
        toggleError(false)
        mPhoneNumber = ""
        pinSlots.forEach {
            mPhoneNumber += it.peeSlot.text.toString()
        }
        if (mPhoneNumber.length == pinCount) {
            mViewModel.requestSmsCode(mPhonePrefix + mPhoneNumber, this)
        }
    }

    private fun toggleError(error: Boolean, @StringRes resId: Int? = null) {
        binding.tvError.isVisible = error
        if (error) {
            resId?.let { id ->
                binding.tvError.setText(id)
            }
        }
    }
}
