package com.sesameware.smartyard_oem.ui.reg.tel

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import timber.log.Timber


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

        if (pinSlots.isNotEmpty()) {
            val et = binding.etHiddenPhoneNumber
            et.requestFocus()
            showSoftKeyboard(et)

            et.addTextChangedListener(object : TextWatcher {
                private var selfModified: Boolean = false

                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    Timber.d("___pre onTextChanged initial s = $s")
                    if (s.isNullOrEmpty()) return
                    if (selfModified) {
                        selfModified = false
                        return
                    }

                    val text = StringBuilder(s)


                    val etPrefix = text.substring(0, mPhonePrefix.length)
                    if (etPrefix.isNotEmpty() && etPrefix == mPhonePrefix) {
                        text.delete(0, mPhonePrefix.length)
                        Timber.d("___pre onTextChanged prefix removed text = $text")
                    }

                    if (text.length > pinCount) {
                        text.delete(pinCount, text.length)
                        Timber.d("___pre onTextChanged length cut text = $text")
                    }

                    val text0 = text.toString()
                    if (s.toString() != text0) {
                        selfModified = true
                        et.setText(text0)
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    Timber.d("___pre afterTextChanged")
                    updatePins(et.text.toString())
                }
            })

            pinSlots[pinSlots.size - 1].peeSlot.doAfterTextChanged {
                checkToSmsReg()
            }

            pinSlots.forEach { pinSlot ->
                pinSlot.peeSlot.setOnClickListener {
                    showSoftKeyboard(et)
                    if (et.text.isNotEmpty()) et.setSelection(et.text.length)
                }
                pinSlot.peeSlot.setOnLongClickListener {
                    showSoftKeyboard(et)
                    if (et.text.isNotEmpty()) et.selectAll()
                    true
                }
            }
        }
    }

    private fun showSoftKeyboard(et: EditText) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updatePins(text: String) {
        val remainingNumbers = StringBuilder(text)
        for (i in 0 until pinSlots.size) {
            val peeNumbers: Editable = pinSlots[i].peeSlot.text ?: break

            val lenToMove = minOf(remainingNumbers.length, pinSlotSizes[i])
            if (lenToMove == 0) {
                peeNumbers.clear()
            }

            val etNumbers: String = remainingNumbers.substring(0, lenToMove)
            remainingNumbers.delete(0, lenToMove)

            val lenToReplace = minOf(etNumbers.length, peeNumbers.length)
            for (j in 0 until lenToReplace) {
                if (etNumbers[j] != peeNumbers[j]) {
                    peeNumbers.replace(j, j + 1, etNumbers[j].toString())
                }
            }

            val lenDiff = etNumbers.length - peeNumbers.length
            if (lenDiff > 0) {
                peeNumbers.append(etNumbers.substring(peeNumbers.length, etNumbers.length))
            } else if (lenDiff < 0) {
                peeNumbers.delete(etNumbers.length, peeNumbers.length)
            }
        }
    }

    private fun checkToSmsReg() {
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
