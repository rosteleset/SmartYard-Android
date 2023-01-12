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
import android.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.databinding.DialogShareAccessBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel

/**
 * @author Nail Shakurov
 * Created on 26/02/2020.
 */
class DialogShareAccessDialog(private val mainActivity: MainActivity? = null) :
    DialogFragment() {

    private var _binding: DialogShareAccessBinding? = null
    private val binding get() = _binding!!

    interface OnDialogAccessListener {
        fun onDone(contactModel: ContactModel)
    }

    var onDialogServiceListener: OnDialogAccessListener? = null
    private var contactModel = ContactModel("", "")
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

        binding.btnDone.setOnClickListener {
            onDialogServiceListener?.onDone(contactModel)
        }

        binding.ivAddContact.setOnClickListener {
            val contactPickerIntent = Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            )
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
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    private val RESULT_PICK_CONTACT = 1

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
}
