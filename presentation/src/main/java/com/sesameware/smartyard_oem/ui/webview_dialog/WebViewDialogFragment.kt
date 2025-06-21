package com.sesameware.smartyard_oem.ui.webview_dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.fragment.app.DialogFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentWebViewDialogBinding

class WebViewDialogFragment(private val resId: Int) : DialogFragment() {
    private var _binding: FragmentWebViewDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebViewDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val brandColorWebHex = getWebColorHex(R.color.brand)
        val html = getString(resId).replace("#007AFF", brandColorWebHex)
        @Suppress("DEPRECATION")
        binding.tvHelpDialogContent.text = Html.fromHtml(html)

        binding.ivWebViewDialogClose.setOnClickListener {
            this.dismiss()
        }
    }

    @SuppressLint("ResourceType")
    private fun getWebColorHex(@ColorRes color: Int): String {
        val hexString = getString(color)
        return if (hexString.length == 7) {
            hexString
        } else if (hexString.length == 9) {
            hexString[0] + hexString.substring(3..8)
        } else {
            throw RuntimeException("WRONG COLOR RESOURCE: $hexString")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
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
