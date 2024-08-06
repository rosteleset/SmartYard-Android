package com.sesameware.smartyard_oem.ui.reg.tel

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentNumberRegBinding
import com.sesameware.smartyard_oem.databinding.PinEntryBinding
import com.sesameware.smartyard_oem.ui.dpToPx
import org.koin.androidx.viewmodel.ext.android.viewModel


class NumberRegFragment : Fragment() {
    private var _binding: FragmentNumberRegBinding? = null
    private val binding get() = _binding!!

    private var mPhonePrefix: String = ""
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
                val lp = d.peeSlot.layoutParams as FrameLayout.LayoutParams
                lp.width = (it.length * dpToPx(25)).toInt()
                d.peeSlot.layoutParams = lp
                binding.llPhone.addView(d.root)
                pinSlots.add(d)
                pinSlotSizes.add(it.length)
                pinCount += it.length
            }
        }

        if (pinSlots.isEmpty()) return

        val et = binding.etHiddenPhoneNumber
        et.requestFocus()
        showSoftKeyboard(et)
        et.doAfterTextChanged(::modifyEtText)

        pinSlots[pinSlots.size - 1].peeSlot.doAfterTextChanged(::checkToSmsReg)
        pinSlots.forEach {
            it.peeSlot.setOnClickListener {
                et.requestFocus()
                showSoftKeyboard(et)
            }
        }
    }

    private fun modifyEtText(s: Editable?) {
        if (s == null) return

        // Cut ET text to pins count
        if (s.length > pinCount) {
            s.delete(pinCount, s.length)
        }

        updatePins(s.toString())
    }

    private fun showSoftKeyboard(et: EditText) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updatePins(text: String) {
        val etRemainingDigits = StringBuilder(text)
        for (i in 0 until pinSlots.size) {
            val peeDigits: Editable = pinSlots[i].peeSlot.text ?: break

            val bufferLen = minOf(etRemainingDigits.length, pinSlotSizes[i])
            if (bufferLen == 0) {
                peeDigits.clear()
            }

            val bufferDigits: String = etRemainingDigits.substring(0, bufferLen)
            etRemainingDigits.delete(0, bufferLen)

            val lenToReplace = minOf(bufferDigits.length, peeDigits.length)
            for (j in 0 until lenToReplace) {
                if (bufferDigits[j] != peeDigits[j]) {
                    peeDigits.replace(j, j + 1, bufferDigits[j].toString())
                }
            }

            val lenToAppendOrDelete = bufferDigits.length - peeDigits.length
            if (lenToAppendOrDelete > 0) {
                peeDigits.append(bufferDigits.substring(peeDigits.length, bufferDigits.length))
            } else if (lenToAppendOrDelete < 0) {
                peeDigits.delete(bufferDigits.length, peeDigits.length)
            }
        }
    }

    private fun checkToSmsReg(s: Editable?) {
        toggleError(false)
        val numbers = binding.etHiddenPhoneNumber.text
        if (numbers.length == pinCount) {
            mViewModel.requestSmsCode(mPhonePrefix + numbers, this)
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
