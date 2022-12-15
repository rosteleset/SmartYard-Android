package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebViewBinding
import com.sesameware.smartyard_oem.ui.dpToPx
import timber.log.Timber

class CustomWebViewFragment : Fragment() {
    private var _binding: FragmentCustomWebViewBinding? = null
    val binding get() = _binding!!

    private var fragmentId: Int = 0
    private var popupId: Int = 0
    private var basePath: String? = null
    private var code: String? = null
    private var title = ""
    var hasBackButton = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            fragmentId = it.getInt(FRAGMENT_ID, fragmentId)
            popupId = it.getInt(POPUP_ID, popupId)
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
        binding.wvExt.webViewClient = CustomWebViewClient(fragmentId, popupId, this, null)
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
        binding.wvExt.clearCache(false)
        disableSomeEvents()

        if (savedInstanceState != null) {
            binding.wvExt.restoreState(savedInstanceState)
        } else {
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
            if (hasBackButton) {
                binding.ivEWVBack.visibility = View.VISIBLE
            } else {
                binding.ivEWVBack.visibility = View.INVISIBLE
                val lp = binding.flExtWebView.layoutParams as ConstraintLayout.LayoutParams
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                lp.topMargin = dpToPx(24).toInt()
                binding.flExtWebView.layoutParams = lp
                binding.flExtWebView.requestLayout()
            }
        }
    }

    override fun onDestroy() {
        CookieManager.getInstance().apply {
            Timber.d("debug_web cookie: ${getCookie(basePath)}")
            setAcceptCookie(true)
            acceptCookie()
            flush()
        }

        super.onDestroy()
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
        const val FRAGMENT_ID = "fragmentId"
        const val POPUP_ID = "popupId"
        const val BASE_PATH = "basePath"
        const val CODE = "code"
        const val TITLE = "title"
        const val HAS_BACK_BUTTON = "hasBackButton"
    }
}
