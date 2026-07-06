package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.sesameware.domain.utils.doDelayed
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebViewBinding
import com.sesameware.smartyard_oem.ui.applyStatusBarInset
import com.sesameware.smartyard_oem.ui.getStatusBarHeight
import com.sesameware.smartyard_oem.ui.setInsetsListener
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class CustomWebViewFragment : Fragment() {
    private var _binding: FragmentCustomWebViewBinding? = null
    val binding get() = _binding!!

    private val viewModel: NfcViewModel by viewModels()

    var nfcManager: NfcManager? = null

    private var fragmentId: Int = 0
    private var popupId: Int = 0
    private var basePath: String? = null
    private var code: String? = null
    private var title = ""
    var hasBackButton = true
    private var canRefresh = true

    private var stateBundle: Bundle? = null

    private var windowInsets: WindowInsetsCompat? = null
    private lateinit var webViewClient: CustomWebViewClient

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

        webViewClient = CustomWebViewClient(fragmentId, popupId, this, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomWebViewBinding.inflate(inflater, container, false)
        binding.srlCustomWebView.applyStatusBarInset()
        binding.wvExt.setInsetsListener {
            windowInsets = it
            if (::webViewClient.isInitialized) {
                webViewClient.windowInsets = it
            }
        }
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.srlCustomWebView.clipToOutline = true
        binding.srlCustomWebView.setOnChildScrollUpCallback { _, _ ->
            binding.wvExt.scrollY > 0
        }
        binding.wvExt.clipToOutline = true
        binding.wvExt.settings.allowContentAccess = true
        binding.wvExt.settings.allowFileAccess = true
        binding.wvExt.settings.domStorageEnabled = true
        binding.wvExt.settings.databaseEnabled = true
        binding.wvExt.settings.javaScriptEnabled = true
        binding.wvExt.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.wvExt.settings.setSupportMultipleWindows(true)
        binding.wvExt.webChromeClient = CustomWebChromeClient(this, null)
        binding.wvExt.webViewClient = webViewClient
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(binding.wvExt.settings, true)
        }
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

            override fun scanNfc(timeout: Long) {
                Timber.d("debug_nfc call scanNfc from WebView $timeout")
                startNfcScan(timeout)
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
        if (binding.tvEWVTitle.text.isNotEmpty()) {
            binding.tvEWVTitle.visibility = View.VISIBLE
        }
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

        observeState()
    }

    private fun startNfcScan(timeout: Long) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        if (nfcAdapter != null) {
            if (nfcManager == null) {
                nfcManager = NfcManager(nfcAdapter)
            }
        } else {
            viewModel.notSupported()
            return
        }
        if (viewModel.state.value != NfcViewModel.State.Idle)
        {
            Timber.d("debug_nfc state is not idle")
            return
        }
        Timber.d("debug_nfc start scan")
        viewModel.startScan(timeout)
        nfcManager?.enableReader(requireActivity()) { tag ->
            val uid = tag.id.joinToString(":") {
                String.format("%02X", it)
            }
            Timber.d("debug_nfc success uid=$uid")

            viewModel.onTagScanned(uid)
            stopNfcScan()
        }
    }

    private fun stopNfcScan() {
        nfcManager?.disableReader(requireActivity())
        viewModel.stopScan()
        if (nfcManager != null) {
            Timber.d("debug_nfc stop scan")
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is NfcViewModel.State.Success -> {
                            val uid = state.uid
                            sendToWebView(uid)
                        }

                        is NfcViewModel.State.Timeout -> {
                            Timber.d("debug_nfc timeout")
                            stopNfcScan()
                            sendToWebView("timeout")
                        }

                        is NfcViewModel.State.Error -> {
                            Timber.d("debug_nfc error")
                            stopNfcScan()
                            sendToWebView("error")
                        }

                        is NfcViewModel.State.NotSupported -> {
                            Timber.d("debug_nfc not supported")
                            stopNfcScan()
                            sendToWebView("not supported")
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun sendToWebView(uid: String) {
        val data = JSONObject.quote(uid)
        Timber.d("debug_nfc send callback with value=$data")
        val js = """
            window.onNfcResult($data);
        """.trimIndent()

        binding.wvExt.post {
            binding.wvExt.evaluateJavascript(js, null)
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

    override fun onStop() {
        super.onStop()
        stopNfcScan()
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
