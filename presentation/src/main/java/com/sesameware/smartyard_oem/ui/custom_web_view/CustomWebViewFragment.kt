package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import androidx.annotation.UiThread
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.sesameware.domain.utils.doDelayed
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebViewBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.applyStatusBarInset
import com.sesameware.smartyard_oem.ui.getStatusBarHeight
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class CustomWebViewFragment : Fragment() {
    private var _binding: FragmentCustomWebViewBinding? = null
    val binding get() = _binding!!

    private val viewModel: NfcViewModel by viewModels()

    var nfcManager: NfcManager? = null

    val args: CustomWebViewFragmentArgs by navArgs()
    private val fragmentId get() = args.fragmentId
    private val popupId get() = args.popupId
    private val basePath get() = args.basePath
    private val code by lazy { WebViewCodeCache.get(args.code) }
    private val title get() = args.title ?: ""
    val hasBackButton get() = args.hasBackButton
    private val statusBarCupertinoStyle get() = args.statusBarStyle
    private val statusBarBackgroundColor get() = args.statusBarColor
    private val canRefresh get() = args.canRefresh

    private var stateBundle: Bundle? = null

    private lateinit var webViewClient: CustomWebViewClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webViewClient = CustomWebViewClient(fragmentId, popupId, this, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomWebViewBinding.inflate(inflater, container, false)
        binding.srlCustomWebView.applyStatusBarInset()
        binding.srlCustomWebView.applyBottomNavInsetsToPadding()
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
        binding.wvExt.addJavascriptInterface(UiWebInterface(), "AndroidUI")
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
        binding.tvEWVTitle.isVisible = title.isNotEmpty()

        binding.ivEWVBack.isVisible = hasBackButton

        binding.fakeStatusBarBackground.isGone = hasBackButton
        if (!hasBackButton) {
            val fakeStatusBarLp = binding.fakeStatusBarBackground.layoutParams
            fakeStatusBarLp.height = getStatusBarHeight()
            binding.fakeStatusBarBackground.layoutParams
            binding.fakeStatusBarBackground.requestLayout()
            statusBarBackgroundColor?.let { sbColor ->
                if (sbColor.isValidHexColor()) {
                    val color = sbColor.toAndroidHexColor().toColorInt()
                    binding.fakeStatusBarBackground.background = color.toDrawable()
                }
            }
        }

        val isLightAppearance = statusBarCupertinoStyle?.let {
            if (it in listOf("light", "dark")) {
                it != "light"
            } else null
        } ?: statusBarBackgroundColor?.let {
                if (it.isValidHexColor()) {
                    val androidColorHex = it.toAndroidHexColor()
                    isStatusBarLight(androidColorHex)
                } else null
            }

        if (isLightAppearance != null) {
            setStatusBarAppearance(isLightAppearance)
        } else {
            (activity as? CommonActivity)?.fragmentHasHeader = hasBackButton
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

    @UiThread
    fun setStatusBarAppearance(isLightAppearance: Boolean) {
        val window = activity?.window ?: return
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = isLightAppearance
    }

    private inner class UiWebInterface {
        @JavascriptInterface
        fun setStatusBarColor(colorHex: String?): String? {
            val errMsg = when {
                colorHex == null -> "The 'colorHex' parameter must not be null"
                colorHex.isEmpty() -> "The 'colorHex' parameter must not be empty"
                !colorHex.isValidHexColor() -> "Invalid colorHex '$colorHex'. Expected hex color, formatted #RRGGBB or #RRGGBBAA"
                else -> null
            }

            if (errMsg != null) {
                return "{\"error\": \"IllegalArgument\", \"message\": \"$errMsg\"}"
            }

            colorHex?.let { sbColor ->
                if (sbColor.isValidHexColor()) {
                    val color = sbColor.toAndroidHexColor().toColorInt()
                    binding.fakeStatusBarBackground.background = ColorDrawable(color)
                }
            }

            return null
        }

        @JavascriptInterface
        fun setStatusBarStyle(style: String?): String? {
            val errMsg = when {
                style == null -> "The 'style' parameter must not be null"
                style.isEmpty() -> "The 'style' parameter must not be empty"
                style !in listOf("light", "dark") -> "Invalid style '${style}'. Expected 'light' or 'dark'"
                else -> null
            }

            if (errMsg != null) {
                return "{\"error\": \"IllegalArgument\", \"message\": \"$errMsg\"}"
            }

            setStatusBarAppearance(style == "dark")
            return null
        }

        @JavascriptInterface
        fun navigateBack() {
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread

                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    companion object {
        const val FRAGMENT_ID = "fragmentId"
        const val POPUP_ID = "popupId"
        const val BASE_PATH = "basePath"
        const val CODE = "code"
        const val TITLE = "title"
        const val HAS_BACK_BUTTON = "hasBackButton"
        const val CAN_REFRESH = "canRefresh"

        private fun String?.isValidHexColor(): Boolean {
            this ?: return false
            val regex = "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$".toRegex()
            return matches(regex)
        }

        private fun String.toAndroidHexColor(): String {
            return if (startsWith("#") && length == 9) {
                val rgb = substring(1, 7)
                val alpha = substring(7, 9)
                "#$alpha$rgb"
            } else {
                this
            }
        }

        // The alpha channel does not affect the luminance
        private fun isStatusBarLight(androidColorHex: String): Boolean {
            val parsedColor = androidColorHex.toColorInt()
            return ColorUtils.calculateLuminance(parsedColor) > 0.5
        }
    }
}
