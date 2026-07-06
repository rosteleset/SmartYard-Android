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
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebBottomBinding
import com.sesameware.smartyard_oem.ui.setInsetsListener
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class CustomWebBottomFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentCustomWebBottomBinding? = null
    val binding get() = _binding!!

    private val viewModel: NfcViewModel by viewModels()

    var nfcManager: NfcManager? = null

    private var fragmentId: Int = 0
    private var popupId: Int = 0
    private var url = ""

    private var stateBundle: Bundle? = null

    private var windowInsets: WindowInsetsCompat? = null
    private lateinit var webViewClient: CustomWebViewClient

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
        binding.wvExtBottom.setInsetsListener {
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
                startNfcScan(timeout)
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

        binding.wvExtBottom.post {
            binding.wvExtBottom.evaluateJavascript(js, null)
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

    override fun onStop() {
        super.onStop()
        stopNfcScan()
    }

    companion object {
        const val FRAGMENT_ID = "fragmentId"
        const val POPUP_ID = "popupId"
        const val URL = "url"
    }
}
