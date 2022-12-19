package com.sesameware.smartyard_oem.ui.main.settings.dialog

import android.app.Dialog
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
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.DialogChangeNameBinding

/**
 * @author Nail Shakurov
 * Created on 2020-02-20.
 */
class DialogChangeName : DialogFragment() {
    private var _binding: DialogChangeNameBinding? = null
    private val binding get() = _binding!!

    var onSuccess: listenerEmpty? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChangeNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mForm.initialize(viewLifecycleOwner, R.string.save) {
            onSuccess?.invoke()
            this.dismiss()
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
}
