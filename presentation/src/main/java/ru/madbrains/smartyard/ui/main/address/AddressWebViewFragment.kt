package ru.madbrains.smartyard.ui.main.address

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.model.response.ItemOption
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAddressWebViewBinding
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.adapters.ParentListAdapter
import ru.madbrains.smartyard.ui.main.address.auth.AuthFragment
import ru.madbrains.smartyard.ui.main.address.auth.AuthViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVMapFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogFragment
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.Flat
import ru.madbrains.smartyard.ui.main.address.guestAccessDialog.GuestAccessDialogFragment
import ru.madbrains.smartyard.ui.main.address.models.ParentModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.EventLogModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModelP
import ru.madbrains.smartyard.ui.main.address.models.interfaces.Yard
import ru.madbrains.smartyard.ui.main.notification.NotificationFragment
import ru.madbrains.smartyard.ui.main.pay.WebViewPayViewModel
import ru.madbrains.smartyard.ui.main.settings.accessAddress.AccessAddressFragment
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber


const val TAG = "AddressWebViewFragment"

data class FlatUri(
    @SerializedName("flatId") val flatId: Int,
    @SerializedName("flatNumber") val flatNumber: Int,
    @SerializedName("frsEnabled") val frsEnabled: String
) {
    fun isFrsEnabled(): Boolean {
        return frsEnabled == "t"
    }
}


class AddressWebViewFragment : Fragment(), GuestAccessDialogFragment.OnGuestAccessListener {
    lateinit var binding: FragmentAddressWebViewBinding
    lateinit var webView: WebView

    private val mPreferenceStorage: PreferenceStorage by inject()
    private val mWebViewPayViewModel by sharedViewModel<WebViewPayViewModel>()
    private val mViewModel by sharedViewModel<AddressViewModel>()
    private val mEventLog by sharedViewModel<EventLogViewModel>()
    private val mAuthFragment by sharedViewModel<AuthViewModel>()
    private val handler = Handler(Looper.getMainLooper())

    private var adapter: ParentListAdapter? = null
    lateinit var recyclerView: RecyclerView

    private var startY = 0f
    private var startX = 0f
    private val SWIPE_THRESHOLD = 500 // Минимальное расстояние для определения свайпа

    private var currentUrl = ""
    private var DEFAULT_URL = ""
    private var isPayBarActive = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddressWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @JavascriptInterface
    fun setCityContract(message: String) {
        activity?.runOnUiThread {
            if (message.isNotEmpty()) {
                binding.tvTitleCity.text = message
            }
        }
    }

    @JavascriptInterface
    fun updatePageNoCache() {
        activity?.runOnUiThread {
            webView.clearCache(true)
            webView.clearHistory()
            loadUrl()
        }
    }

    @JavascriptInterface
    fun isPayBarActive(isActive: Boolean) {
        activity?.runOnUiThread {
            isPayBarActive = isActive
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun swipeRefresh() {
        binding.swipeRefreshLayoutWebView?.setOnRefreshListener {
            if (webView.scrollY == 0) {
                binding.swipeRefreshLayoutWebView?.isEnabled = !isPayBarActive
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    binding.swipeRefreshLayoutWebView?.isRefreshing = false
                    binding.swipeRefreshLayoutWebView?.isEnabled = false
                    loadUrl()
                }, 2000)
            }
        }

        binding.webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Запомните начальное положение касания
                    startY = event.y
                    startX = event.x
                    binding.swipeRefreshLayoutWebView?.isEnabled = !isPayBarActive
                }

                MotionEvent.ACTION_MOVE -> {
                    // Вычислите расстояние свайпа
                    val deltaY = event.y - startY
                    val deltaX = event.x - startX
                    //Горизонтальный свайп
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        binding.swipeRefreshLayoutWebView?.isEnabled = false
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.swipeRefreshLayoutWebView?.isEnabled = true
                    startY = 0f
                    startX = 0f
                    // Сброс состояния после завершения жеста
                }
            }
            false
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivNoti?.setOnClickListener {
            navigateToFragment(R.id.fl_address_web_view_fragment, NotificationFragment())
        }
        binding.ivAddContractWeb?.setOnClickListener {
            navigateToFragment(R.id.cl_fragment_wv, AuthFragment())
        }
        swipeRefresh()
        val statusBarSize = getStatusBarHeight()
        binding.clCityMain?.setPadding(0, statusBarSize, 0, 0)

//        binding.tvTitleCity.setPadding(0,statusBarSize, 0 , 0)
//        binding.ivMarker.setPadding(0,statusBarSize, 0 , 0)
        observer()
        createWebViewClient()
    }


    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private fun navigateToFragment(id: Int, fragment: Fragment, bundle: Bundle? = null) {
        val transaction = parentFragmentManager.beginTransaction()
        // Проверка, что фрагмент с таким классом уже находится в стеке
        val existingFragment =
            parentFragmentManager.findFragmentByTag(fragment.javaClass.simpleName)
        if (existingFragment != null) {
            // Если фрагмент уже есть в стеке, то просто открываем его
            transaction.show(existingFragment)
        } else {
            if (bundle != null) {
                fragment.arguments = bundle
            }
            transaction.add(id, fragment, fragment.javaClass.simpleName)
            // Добавляем транзакцию в стек возврата только если это фрагмент, вызванный из этого метода
            // Не добавляем в стек, если фрагмент возвращает на главный экран
            if (!shouldReturnToMainScreen(fragment)) {
                transaction.addToBackStack("root")
            }
        }
        // Скрываем текущий фрагмент
        val currentFragment = parentFragmentManager.findFragmentById(id)
        if (currentFragment != null) {
            transaction.hide(currentFragment)
        }
        transaction.commit()
    }

    private fun shouldReturnToMainScreen(fragment: Fragment): Boolean =
        fragment is AddressWebViewFragment


    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebViewClient() {
//        WebView.setWebContentsDebuggingEnabled(true) //TODO Debug chrome://inspect
        if (!this::webView.isInitialized) {
            webView = binding.webView
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
//            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            webView.addJavascriptInterface(this, "Android")
            //Cache
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        }

        val bearerToken = mPreferenceStorage.authToken ?: ""
        val javascript = "bearerToken = function() { return \"$bearerToken\"; };"
        var mCustomView: View? = null
        var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
        var mFullscreenContainer: FrameLayout? = null
        var mOriginalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        var mOriginalSystemUiVisibility = 0

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                Timber.d("fullscreen onShowCustomView")
                if (mCustomView != null) {
                    onHideCustomView()
                    return
                }
                mCustomView = view
                mOriginalSystemUiVisibility = requireActivity().window.decorView.systemUiVisibility
                mOriginalOrientation = requireActivity().requestedOrientation
                mCustomViewCallback = callback

                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutParams.gravity = Gravity.CENTER
                mCustomView?.layoutParams = layoutParams
                (requireActivity().window.decorView as FrameLayout)
                    .addView(
                        mCustomView, FrameLayout.LayoutParams(-1, -1)
                    )
                requireActivity().window.decorView.systemUiVisibility =
                    3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                Timber.d("fullscreen onHideCustomView")
                (requireActivity().window.decorView as FrameLayout).removeView(mCustomView)
                mCustomView = null
                requireActivity().window.decorView.systemUiVisibility =
                    mOriginalSystemUiVisibility
                requireActivity().requestedOrientation = mOriginalOrientation
                mCustomViewCallback!!.onCustomViewHidden()
                mCustomViewCallback = null
            }
        }

        val visualStateCallback = object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                webView.visibility = View.VISIBLE
                binding.webViewProgressBar5.visibility = View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            var isError = false

            override fun onPageFinished(view: WebView?, urlClient: String?) {
                super.onPageFinished(view, urlClient)
                if (!isError) {
                    webView.postVisualStateCallback(0, visualStateCallback)
                    webView.evaluateJavascript("javascript:$javascript", null)

                    if (urlClient != null && DEFAULT_URL != urlClient) {
                        if (currentUrl != urlClient && urlClient.length != DEFAULT_URL.length + 1) {
                            currentUrl = urlClient
                            webViewUrlPager()
                        }
                    } else {
                        currentUrl = DEFAULT_URL
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
//                Timber.d("IntercomWebView onReceivedHttpError ${errorResponse}, URL ${request?.url} request ${request?.requestHeaders}")
//
//                handler.postDelayed({
//                    loadUrl()
//                    isError = false
//                }, 5000)
//                isError = true

                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
//                Timber.d("IntercomWebView onReceivedError ${error?.errorCode}, URL ${request?.url} request ${request?.requestHeaders}")
//                handler.postDelayed({
//                    loadUrl()
//                    isError = false
//                }, 5000)
//                isError = true
                super.onReceivedError(view, request, error)
            }
        }
    }

    private fun webViewUrlPager() {
        if (currentUrl.isNotEmpty()) {
            Timber.d("webViewUrlPager currentUrl ${currentUrl}")
            val url = currentUrl
            val result = url.replaceFirst("#", "")
            val uri = Uri.parse(result)
            val uriType = uri.getQueryParameter("type").toString().replace(" ", "")

            Timber.d("URI_TYPE $uriType\nwebViewCurrentUrl ${webView.url}\ncurrentUrl ${uri}")

            if (uriType == SETTINGS) {
                val bundleSettings = Bundle()
                val flatId = uri.getQueryParameter("flatId")
                val contractOwner = uri.getQueryParameter("contractOwner") == "t"
                val hasGates = uri.getQueryParameter("hasGates") == "t"
                val clientId = uri.getQueryParameter("clientId")
                val address = uri.getQueryParameter("address")

                bundleSettings.putString("address", address.toString())
                bundleSettings.putString("clientId", clientId.toString())
                bundleSettings.putInt("flatId", flatId!!.toInt())
                bundleSettings.putBoolean("hasGates", hasGates)
                bundleSettings.putBoolean("contractOwner", contractOwner)
                Timber.d("ASDQQQWEEERRR $flatId, $contractOwner $hasGates $clientId $address")
                if (!bundleSettings.isEmpty) {
                    navigateToFragment(R.id.cl_fragment_wv, AccessAddressFragment(), bundleSettings)
                }
            }
            if (uriType == EVENTS) {
                val clientId = uri.getQueryParameter("clientId")
                val houseId = uri.getQueryParameter("houseId")?.toInt()
                val address = uri.getQueryParameter("address")
                val flatsUri = uri.getQueryParameter("flats")

                val gson = Gson()
                val flatUriList = gson.fromJson(flatsUri, Array<FlatUri>::class.java)
                val flatsList = flatUriList?.toList()
                var flatId = 0
                var flatNumber = ""
                var frsEnabled = false
                var flats = listOf<Flat>()

                flatsList?.forEach {
                    flatId = it.flatId
                    flatNumber = it.flatNumber.toString()
                    frsEnabled = it.isFrsEnabled()
                }
                for (i in 0 until mViewModel.dataList.value!!.size) {
                    if (mViewModel.dataList.value!![i] is ParentModel) {
                        val parentsModel = mViewModel.dataList.value!![i] as ParentModel
                        if (houseId == parentsModel.houseId) {
                            parentsModel.children.forEach {
                                if (it is EventLogModel) {
                                    flats = it.flats
                                }
                            }
                        }
                    }
                }
                flats.forEach {
                    if (flatId == it.flatId) {
                        mEventLog.address = address.toString()
                        mEventLog.flatsAll = listOf(Flat(flatId, "", true))
//                        mEventLog.flatsAll =
//                            listOf(Flat(flatId, flatNumber, frsEnabled))
                        mEventLog.filterFlat = null
                        mEventLog.currentEventDayFilter = null
                        mEventLog.lastLoadedDayFilterIndex.value = -1
                        mEventLog.currentEventItem = null
                        mEventLog.getAllFaces()
                        navigateToFragment(R.id.cl_fragment_wv, EventLogFragment())
                    }
                }
            }
            if (uriType == ADD_CONTRACT) {
                navigateToFragment(R.id.cl_fragment_wv, AuthFragment())
            }
        }
    }

    private fun observer() {
        val observer = Observer<List<ItemOption>> {
            Timber.d("webViewUrlPager observer list $it")
            if (it.isNotEmpty()) {
                if (it[0].centraScreenUrl == null) {
                    Thread.sleep(500)
                    mWebViewPayViewModel.getOptions()
                } else {
                    if (DEFAULT_URL.isEmpty()) {
                        DEFAULT_URL = it[0].centraScreenUrl.toString()
                    }
                }
                loadUrl()
            }
        }
        mWebViewPayViewModel.options.observe(viewLifecycleOwner, observer)
    }

    private fun loadUrl() {
        if (DEFAULT_URL.isNotEmpty()) {
            Timber.d("webViewUrlPager LOADLADASDURLRL DEFAULT_URL:$DEFAULT_URL")
            webView.loadUrl(DEFAULT_URL)
            webViewUrlPager()
        }
    }

    override fun onDismiss(dialog: GuestAccessDialogFragment) {
        dialog.dismiss()
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel.dataList.observe(
            viewLifecycleOwner
        ) {
            adapter?.items = it
            adapter?.notifyDataSetChanged()
            updateAllWidget(requireContext())
        }
    }


    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == REFRESH_INTENT) {
                    loadUrl()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(REFRESH_INTENT)
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                intentFilter
            )
        }
    }


    override fun onShare() {}

    companion object {
        private const val SETTINGS = "conf"
        private const val EVENTS = "events"
        private const val ADD_CONTRACT = "addContract"
        const val REFRESH_INTENT = "REFRESH_INTENT"
    }
}
