package ru.madbrains.smartyard.ui.main.address.guestAccessDialog

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import kotlinx.android.synthetic.main.dialog_guest_access.btnDismiss
import kotlinx.android.synthetic.main.dialog_guest_access.btnShare
import kotlinx.android.synthetic.main.dialog_guest_access.rvGuestAccess
import kotlinx.android.synthetic.main.dialog_guest_access.tvAddContact
import ru.madbrains.smartyard.R
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 2020-02-19.
 */
class GuestAccessDialogFragment : DialogFragment() {
    interface OnGuestAccessListener {
        fun onDismiss(dialog: GuestAccessDialogFragment)
        fun onShare()
    }

    var onGuestAccessListener: OnGuestAccessListener? = null

    var listGuestAccessModel = mutableListOf<GuestAccessModel>()

    lateinit var adapter: ListDelegationAdapter<List<GuestAccessModel>>

    private val RESULT_PICK_CONTACT = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_guest_access, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onGuestAccessListener = targetFragment as OnGuestAccessListener?
        } catch (e: ClassCastException) {
            Timber.d("onAttach: ClassCastException : " + e.message)
        }
    }

    private fun initRecycler() {
        rvGuestAccess.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter<List<GuestAccessModel>>(
            GuestAccessDelegates(activity!!) {
                listGuestAccessModel.removeAt(it)
                adapter.notifyDataSetChanged()
            }
        )
        listGuestAccessModel.add(GuestAccessModel("+7 917 619 48 95", "Юрий"))
        listGuestAccessModel.add(GuestAccessModel("+7 917 619 48 95", "Василий"))

        adapter.items = listGuestAccessModel
        rvGuestAccess.adapter = adapter
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()

        btnShare.setOnClickListener {
            onGuestAccessListener?.onShare()
        }
        btnDismiss.setOnClickListener {
            onGuestAccessListener?.onDismiss(this)
        }
        tvAddContact.setOnClickListener {
            val contactPickerIntent = Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            )
            startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                RESULT_PICK_CONTACT -> {
                    val contactUri: Uri = data?.data!!
                    val cursor = context!!.contentResolver.query(
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
                        listGuestAccessModel.add(GuestAccessModel(number, name))
                        adapter.notifyDataSetChanged()
                    }
                    cursor?.close()
                }
            }
        }
    }
}
