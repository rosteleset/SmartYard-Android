package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebViewBinding

class CustomWebViewFragment : Fragment() {
    private var _binding: FragmentCustomWebViewBinding? = null
    val binding get() = _binding!!

    private var basePath: String? = null
    private var code: String? = null
    private var title = ""
    var hasBackButton = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            basePath = it.getString(BASE_PATH)
            code = it.getString(CODE)
            title = it.getString(TITLE, title)
            hasBackButton = it.getBoolean(HAS_BACK_BUTTON, hasBackButton)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flExtWebView.clipToOutline = true
        binding.wvExt.clipToOutline = true
        binding.wvExt.settings.javaScriptEnabled = true
        binding.wvExt.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.wvExt.settings.setSupportMultipleWindows(true)
        binding.wvExt.webChromeClient = CustomWebChromeClient(this, null)
        binding.wvExt.webViewClient = CustomWebViewClient(this, null)
        binding.wvExt.addJavascriptInterface(CustomWebInterface(object : CustomWebInterface.Callback {
            override fun onPostLoadingStarted() {
                requireActivity().runOnUiThread {
                    binding.pbWebView.visibility = View.VISIBLE
                }
            }

            override fun onPostLoadingFinished() {
                requireActivity().runOnUiThread {
                    binding.pbWebView.visibility = View.INVISIBLE
                }
            }
        }), CustomWebInterface.WEB_INTERFACE_OBJECT)
        binding.wvExt.clearCache(true)
        disableSomeEvents()

        if (code.isNullOrEmpty()) {
            binding.wvExt.loadUrl(basePath ?: "")
        } else {
            binding.wvExt.loadDataWithBaseURL(basePath, code!!, "text/html", "utf-8", null)
        }

        binding.ivEWVBack.setOnClickListener {
            if (binding.tvEWVTitle.text.isNotEmpty()) {
                findNavController().popBackStack()
            }
        }

        binding.tvEWVTitle.text = title
        binding.ivEWVBack.visibility = if (hasBackButton) View.VISIBLE else View.INVISIBLE
    }

    private fun disableSomeEvents() {
        binding.wvExt.setOnLongClickListener {
            true
        }

        binding.wvExt.setOnDragListener { _, _ ->
            true
        }
    }

    companion object {
        const val BASE_PATH = "basePath"
        const val CODE = "code"
        const val TITLE = "title"
        const val HAS_BACK_BUTTON = "hasBackButton"

        @JvmStatic
        fun newInstance(basePath: String, code: String, title: String, hasBackButton: Boolean) =
            CustomWebViewFragment().apply {
                arguments = Bundle().apply {
                    putString(BASE_PATH, basePath)
                    putString(CODE, code)
                    putString(TITLE, title)
                    putBoolean(HAS_BACK_BUTTON, hasBackButton)
                }
            }
    }
}
