package ru.madbrains.smartyard.ui.main.settings.accessAddress.dialogDeleteReason

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import kotlinx.android.synthetic.main.dialog_delete_reason.btnDelete
import kotlinx.android.synthetic.main.dialog_delete_reason.btnDismiss
import kotlinx.android.synthetic.main.dialog_delete_reason.etReason
import kotlinx.android.synthetic.main.dialog_delete_reason.rvGuestAccess
import ru.madbrains.smartyard.R
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 26/02/2020.
 */
class DialogDeleteReasonFragment : DialogFragment() {
    interface OnGuestDeleteListener {
        fun onDismiss(dialog: DialogDeleteReasonFragment)
        fun onShare(reasonText: String, reasonList: String)
    }

    var onDeleteReasonListener: OnGuestDeleteListener? = null

    private var listReasonAccessModel = mutableListOf<ReasonModel>()

    private lateinit var adapter: ListDelegationAdapter<List<ReasonModel>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_delete_reason, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onDeleteReasonListener = targetFragment as OnGuestDeleteListener?
        } catch (e: ClassCastException) {
            Timber.d("onAttach: ClassCastException : " + e.message)
        }
    }

    private fun initRecycler() {
        rvGuestAccess.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter<List<ReasonModel>>(
            ReasonDelegateAdapter()
        )
        // listReasonAccessModel.add(ReasonModel("Не хочу управлять этим адресом из приложения"))
        listReasonAccessModel.add(ReasonModel("Хочу расторгнуть договор"))
        listReasonAccessModel.add(ReasonModel("Другое"))

        adapter.items = listReasonAccessModel
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

        btnDelete.setOnClickListener {
            onDeleteReasonListener?.onShare(
                etReason.text.toString(),
                listReasonAccessModel.filter { it.check }.map { it.name }
                    .joinToString { it -> "\'${it}\'" }
            )
        }
        btnDismiss.setOnClickListener {
            onDeleteReasonListener?.onDismiss(this)
        }
    }
}
