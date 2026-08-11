package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebBottomBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class CustomWebBottomFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentCustomWebBottomBinding? = null
    val binding get() = _binding!!

    private val viewModel: NfcViewModel by viewModels()

    private var nfcManager: NfcManager? = null

    private var fragmentId: Int = 0
    private var popupId: Int = 0
    private var url = ""

    private var stateBundle: Bundle? = null

    private lateinit var webViewClient: CustomWebViewClient
    private var nfcSessionActive = false
    private var nfcSessionTimeout = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)

        arguments?.let {
            fragmentId = it.getInt(CustomWebViewFragment.FRAGMENT_ID, fragmentId)
            popupId = it.getInt(CustomWebViewFragment.POPUP_ID, popupId)
            url = it.getString(URL, url)
        }

        webViewClient = CustomWebViewClient(fragmentId, popupId, null, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomWebBottomBinding.inflate(inflater, container, false)
        binding.wvContainer.applyBottomNavInsetsToPadding()
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.wvExtBottom.settings.javaScriptEnabled = true
        binding.wvExtBottom.settings.allowContentAccess = true
        binding.wvExtBottom.settings.allowFileAccess = true
        binding.wvExtBottom.settings.domStorageEnabled = true
        binding.wvExtBottom.settings.databaseEnabled = true
        binding.wvExtBottom.webChromeClient = CustomWebChromeClient(null, this)
        binding.wvExtBottom.webViewClient = webViewClient
        binding.wvExtBottom.addJavascriptInterface(CustomWebInterface(object : CustomWebInterface.Callback {
            override fun onPostLoadingStarted() {
                requireActivity().runOnUiThread {
                    binding.pbWebViewBottom.visibility = View.VISIBLE
                }
            }

            override fun onPostLoadingFinished() {
                requireActivity().runOnUiThread {
                    binding.pbWebViewBottom.visibility = View.INVISIBLE
                }
            }

            override fun onPostRefreshParent(timeout: Int) {
                requireActivity().runOnUiThread {
                    requireActivity()
                        .supportFragmentManager
                        .primaryNavigationFragment
                        ?.childFragmentManager
                        ?.fragments
                        ?.forEach {
                            (it as? CustomWebViewFragment)?.let {f ->
                                f.refreshPage(timeout)
                                return@forEach
                            }
                        }
                }
            }

            override fun isAppInstalled(url: String): Boolean {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                return appIntent.resolveActivity(requireActivity().packageManager) != null
            }

            override fun scanNfc(timeout: Long) {
                Timber.d("debug_nfc call scanNfc from WebView $timeout")
                activity?.runOnUiThread {
                    startNfcScan(timeout)
                }
            }

            override fun stopNfc() {
                Timber.d("debug_nfc call stopNfc from WebView")
                activity?.runOnUiThread {
                    stopNfcScan()
                }
            }
        }), CustomWebInterface.WEB_INTERFACE_OBJECT)

        //костыль для подгона высоты
        /*dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Timber.d("debug_web newState = $newState")
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        changeLayout()
                    } else if (newState != BottomSheetBehavior.STATE_SETTLING && newState != BottomSheetBehavior.STATE_DRAGGING) {
                        setDefaultLayout()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    //ничего не делаем
                }
            })
            it.setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }*/

        if (stateBundle != null) {
            binding.wvExtBottom.restoreState(stateBundle!!)
        } else {
            binding.wvExtBottom.loadUrl(url)
        }

        binding.wvExtBottom.clearCache(true)
        disableSomeEvents()

        observeState()
    }

    private fun startNfcScan(timeout: Long) {
        nfcSessionActive = true
        nfcSessionTimeout = timeout
        startNfcReader(timeout)
    }

    private fun startNfcReader(timeout: Long) {
        stopNfcReader()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        if (nfcAdapter != null) {
            val manager = NfcManager(nfcAdapter)
            nfcManager = manager
            Timber.d("debug_nfc enable reader")
            manager.enableReader(requireActivity()) { tag ->
                val uid = tag.id.joinToString(":") {
                    String.format("%02X", it)
                }
                viewModel.onTagScanned(uid)
                sendToWebView(uid)
            }
        } else {
            viewModel.notSupported()
            return
        }
        viewModel.startScan(timeout)
    }

    private fun stopNfcReader() {
        viewModel.stopScan()
        Timber.d("debug_nfc disable reader")
        activity?.let { nfcManager?.disableReader(it) }
        nfcManager = null
    }

    private fun stopNfcScan() {
        nfcSessionActive = false
        stopNfcReader()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is NfcViewModel.State.Timeout -> {
                            Timber.d("debug_nfc timeout")
                            stopNfcScan()
                            sendToWebView("timeout")
                        }

                        is NfcViewModel.State.NotSupported -> {
                            Timber.d("debug_nfc not supported")
                            stopNfcScan()
                            sendToWebView("not supported")
                        }

                        NfcViewModel.State.Idle,
                        NfcViewModel.State.Scanning -> Unit
                    }
                }
            }
        }
    }

    private fun sendToWebView(uid: String) {
        val webView = _binding?.wvExtBottom ?: return
        val data = JSONObject.quote(uid)
        Timber.d("debug_nfc send callback with value=$data")
        val js = """
            window.onNfcResult($data);
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    override fun onPause() {
        Timber.d("debug_web onPause CustomWebBottomFragment")
        binding.wvExtBottom.onPause()
        super.onPause()
        stopNfcReader()

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

        binding.wvExtBottom.saveState(stateBundle!!)
    }

    //костыль: пересчитываем высоту WebView для правильной работы скроллинга
    /*fun changeLayout() {
        Timber.d("debug_web changeLayout")
        val position = IntArray(2)
        binding.wvExtBottom.getLocationOnScreen(position)
        val newHeight = requireContext().resources.displayMetrics.heightPixels - position[1]
        Timber.d("debug_web changeLayout newHeight = $newHeight")
        if (newHeight > 0) {
            val lp = binding.wvExtBottom.layoutParams
            lp.height = newHeight
            binding.wvExtBottom.layoutParams = lp
            binding.root.requestLayout()
        } else {
            setDefaultLayout()
        }
    }

    private fun setDefaultLayout() {
        val lp = binding.wvExtBottom.layoutParams
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.wvExtBottom.layoutParams = lp
        binding.root.requestLayout()
    }*/

    private fun disableSomeEvents() {
        binding.wvExtBottom.setOnLongClickListener {
            true
        }

        binding.wvExtBottom.setOnDragListener { _, _ ->
            true
        }
    }

    override fun onResume() {
        super.onResume()
        binding.wvExtBottom.onResume()
        if (nfcSessionActive && nfcManager == null) {
            Timber.d("debug_nfc resume reader")
            startNfcReader(nfcSessionTimeout)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        Timber.d("debug_web onDismiss CustomWebBottomFragment")
        stopNfcScan()
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        Timber.d("debug_web onDestroyView CustomWebBottomFragment")
        stopNfcScan()
        _binding?.wvExtBottom?.let { webView ->
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.webChromeClient = WebChromeClient()
            webView.removeAllViews()
            webView.destroy()
        }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val FRAGMENT_ID = "fragmentId"
        const val POPUP_ID = "popupId"
        const val URL = "url"
    }
}
