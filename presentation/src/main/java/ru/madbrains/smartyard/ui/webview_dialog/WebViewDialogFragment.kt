package ru.madbrains.smartyard.ui.webview_dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Html
import android.view.*
import androidx.fragment.app.DialogFragment
import ru.madbrains.smartyard.databinding.FragmentWebViewDialogBinding

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

        binding.tvHelpDialogContent.text = Html.fromHtml(getString(resId))

        binding.ivWebViewDialogClose.setOnClickListener {
            this.dismiss()
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
