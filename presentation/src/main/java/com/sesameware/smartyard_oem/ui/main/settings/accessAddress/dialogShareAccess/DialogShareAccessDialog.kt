package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogShareAccess

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.LicensePlate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.DialogShareAccessBinding
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessType
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue

/**
 * @author Nail Shakurov
 * Created on 26/02/2020.
 */
class DialogShareAccessDialog(
    private val type: AccessType,
    private val addAccessByPhone: (ContactModel, AccessType) -> Unit,
    private val addAccessByLicensePlate: (LicensePlateValue) -> Unit
) : DialogFragment() {

    private var _binding: DialogShareAccessBinding? = null
    private val binding get() = _binding!!

    private val contactModel = ContactModel("", "")
    private var licensePlateString = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogShareAccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (type == AccessType.GATE_BY_LICENSE_PLATE) {
            setupForLicensePlate()
        } else {
            setupForPhone()
        }
    }

    private fun String.licensePlatePatternToHint(): String {
        val hint = StringBuilder()
        var digitCounter = 0
        var letterCounter = 0
        forEach {
            when (it) {
                '#' -> hint.append(digitCounter++)
                '*' -> hint.append(LicensePlate.ALLOWED_LETTERS[letterCounter++])
                else -> hint.append(it)
            }
        }
        return hint.toString()
    }

    private fun setupForLicensePlate() {
        binding.btnDone.setOnClickListener {
            addAccessByLicensePlate.invoke(LicensePlateValue(licensePlateString))
        }

        binding.tvCaption.text =
            getString(R.string.dialog_share_access_by_license_plate_caption)

        binding.ivAddContact.isVisible = false

        with(binding.prefixEditText) {
            setMask(null)
            setText("")

            val pattern = DataModule.licensePlatePattern
            hint = pattern.licensePlatePatternToHint()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(InputFilter.LengthFilter(pattern.length))

            addTextChangedListener(object : TextWatcher {
                private var isUpdating = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isUpdating) return
                    isUpdating = true

                    val validCyrillic = "АВЕКМНОРСТУХ"
                    val validLatin = "ABEKMHOPCTYX"

                    val rawInput = s?.toString()?.uppercase() ?: ""
                    val cleanRaw = StringBuilder()

                    for (c in rawInput) {
                        if (validLatin.contains(c)) {
                            cleanRaw.append(validCyrillic[validLatin.indexOf(c)])
                        } else if (validCyrillic.contains(c) || c.isDigit()) {
                            cleanRaw.append(c)
                        }
                    }

                    val builder = StringBuilder()
                    var rawIndex = 0
                    var patternIndex = 0

                    while (rawIndex < cleanRaw.length && patternIndex < pattern.length) {
                        val p = pattern[patternIndex]
                        val c = cleanRaw[rawIndex]

                        if (p == ' ') {
                            builder.append(' ')
                            patternIndex++
                        } else if (p == '*') {
                            if (validCyrillic.contains(c)) {
                                builder.append(c)
                                patternIndex++
                            }
                            rawIndex++
                        } else if (p == '#') {
                            if (c.isDigit()) {
                                builder.append(c)
                                patternIndex++
                            }
                            rawIndex++
                        } else {
                            builder.append(p)
                            patternIndex++
                        }
                    }

                    val formatted = builder.toString()
                    setText(formatted)
                    setSelection(formatted.length)

                    licensePlateString = formatted.replace(" ", "")
                    binding.btnDone.isEnabled = LicensePlate.allowedPatterns.any { it.matches(licensePlateString) }

                    isUpdating = false
                }
            })
        }
    }

    private fun setupForPhone() {
        binding.btnDone.setOnClickListener {
            addAccessByPhone.invoke(contactModel, type)
        }

        binding.tvCaption.text =
            getString(R.string.dialog_share_access_by_phone_caption)


        binding.ivAddContact.setOnClickListener {
            val contactPickerIntent = Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            )
            @Suppress("DEPRECATION")
            startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT)
        }

        setupPrefixSample()
    }

    private fun setupPrefixSample() {
        var hint = ""
        DataModule.phonePattern.forEach {
            if (it == '#') {
                hint += '0'
            }
        }
        binding.prefixEditText.hint = hint
        binding.prefixEditText.setMask(DataModule.phonePattern)
        binding.prefixEditText.addTextChangedListener {
            var rawPhone = ""
            val digits = "0123456789"
            binding.prefixEditText.getRawText().forEach {
                if (digits.contains(it)) {
                    rawPhone += it
                }
            }
            var phone = ""
            binding.prefixEditText.text.forEach {
                if (digits.contains(it)) {
                    phone += it
                }
            }
            contactModel.number = phone
            binding.btnDone.isEnabled = (rawPhone.length == hint.length)
        }
    }

    override fun onStart() {
        super.onStart()
        val back = ColorDrawable(Color.TRANSPARENT)
        val inset = InsetDrawable(back, 30)
        dialog?.window?.setBackgroundDrawable(inset)
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                RESULT_PICK_CONTACT -> {
                    val contactUri: Uri = data?.data!!
                    val cursor = requireContext().contentResolver.query(
                        contactUri, null,
                        null, null, null
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        val phoneIndex =
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex =
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val number = cursor.getString(phoneIndex)
                        val name = cursor.getString(nameIndex)
                        contactModel.name = name

                        //учитываем префикс
                        var rawPhone = ""
                        val digits = "0123456789"
                        number.forEach {
                            if (digits.contains(it)) {
                                rawPhone += it
                            }
                        }
                        if (rawPhone.length > binding.prefixEditText.hint.length) {
                            binding.prefixEditText.setText(rawPhone.substring(rawPhone.length - binding.prefixEditText.hint.length))
                        } else {
                            binding.prefixEditText.setText(rawPhone)
                        }

                        binding.btnDone.isEnabled = true
                    }
                    cursor?.close()
                }
            }
        }
    }

    companion object {
        private const val RESULT_PICK_CONTACT = 1
    }
}
