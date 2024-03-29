package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.domain.utils.doDelayed
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebViewBinding
import com.sesameware.smartyard_oem.ui.getStatusBarHeight

class CustomWebViewFragment : Fragment() {
    private var _binding: FragmentCustomWebViewBinding? = null
    val binding get() = _binding!!

    private var fragmentId: Int = 0
    private var popupId: Int = 0
    private var basePath: String? = null
    private var code: String? = null
    private var title = ""
    var hasBackButton = true
    private var canRefresh = true

    private var stateBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            fragmentId = it.getInt(FRAGMENT_ID, fragmentId)
            popupId = it.getInt(POPUP_ID, popupId)
            basePath = it.getString(BASE_PATH)
            code = it.getString(CODE)
            title = it.getString(TITLE, title)
            hasBackButton = it.getBoolean(HAS_BACK_BUTTON, hasBackButton)
            canRefresh = it.getBoolean(CAN_REFRESH, canRefresh)
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

        binding.srlCustomWebView.clipToOutline = true
        binding.wvExt.clipToOutline = true
        binding.wvExt.settings.allowContentAccess = true
        binding.wvExt.settings.allowFileAccess = true
        binding.wvExt.settings.domStorageEnabled = true
        binding.wvExt.settings.databaseEnabled = true
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

            override fun onPostRefreshParent(timeout: Int) {
                refreshPage(timeout)
            }

            override fun isAppInstalled(url: String): Boolean {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                return appIntent.resolveActivity(requireActivity().packageManager) != null
            }
        }), CustomWebInterface.WEB_INTERFACE_OBJECT)
        binding.wvExt.clearCache(true)
        if (canRefresh) {
            binding.srlCustomWebView.isEnabled = true
            binding.srlCustomWebView.setOnRefreshListener {
                binding.srlCustomWebView.isRefreshing = false
                binding.wvExt.reload()
            }
        } else {
            binding.srlCustomWebView.setOnRefreshListener(null)
            binding.srlCustomWebView.isEnabled = false
        }

        disableSomeEvents()

        if (stateBundle != null) {
            binding.wvExt.restoreState(stateBundle!!)
        } else {
            if (code.isNullOrEmpty()) {
                binding.wvExt.loadUrl(basePath ?: "")
            } else {
                binding.wvExt.loadDataWithBaseURL(basePath, code!!, "text/html", "utf-8", null)
            }
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
            val lp = binding.srlCustomWebView.layoutParams as ConstraintLayout.LayoutParams
            lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            lp.topMargin = getStatusBarHeight()
            binding.srlCustomWebView.layoutParams = lp
            binding.srlCustomWebView.requestLayout()
        }
    }

    override fun onPause() {
        super.onPause()

        saveState()
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            acceptCookie()
            flush()
        }
    }

    private fun saveState() {
        if (stateBundle == null) {
            stateBundle = Bundle()
        }

        binding.wvExt.saveState(stateBundle!!)
    }

    private fun disableSomeEvents() {
        binding.wvExt.setOnLongClickListener {
            true
        }

        binding.wvExt.setOnDragListener { _, _ ->
            true
        }
    }

    fun refreshPage(timeout: Int) {
        doDelayed(
            {
                requireActivity().runOnUiThread {
                    binding.wvExt.reload()
                }
            }, timeout * 1000L)
    }

    companion object {
        const val FRAGMENT_ID = "fragmentId"
        const val POPUP_ID = "popupId"
        const val BASE_PATH = "basePath"
        const val CODE = "code"
        const val TITLE = "title"
        const val HAS_BACK_BUTTON = "hasBackButton"
        const val CAN_REFRESH = "canRefresh"
    }
}
