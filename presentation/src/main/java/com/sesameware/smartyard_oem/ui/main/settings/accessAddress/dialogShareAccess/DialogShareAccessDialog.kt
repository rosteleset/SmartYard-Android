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
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.redmadrobot.inputmask.MaskedTextChangedListener.Companion.installOn
import com.redmadrobot.inputmask.helper.AffinityCalculationStrategy
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
        val editText: EditText = binding.prefixEditText
        val affineFormats: MutableList<String> = ArrayList()
        val listener =
            installOn(
                editText,
                "+7 ([000]) [000]-[00]-[00]",
                affineFormats, AffinityCalculationStrategy.PREFIX,
                object : MaskedTextChangedListener.ValueListener {
                    override fun onTextChanged(maskFilled: Boolean, extractedValue: String, formattedValue: String) {
                        binding.btnDone.isEnabled = maskFilled
                        contactModel.number = "7$extractedValue"
                    }
                }
            )
        editText.hint = listener.placeholder()
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
                        binding.prefixEditText.setText(number)
                        binding.btnDone.isEnabled = true
                    }
                    cursor?.close()
                }
            }
        }
    }
}
