package ru.madbrains.smartyard.ui.main.intercom

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.component.inject
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.CameraImageInteractor
import ru.madbrains.domain.model.ImageItem
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.domain.model.response.ItemOption
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentIntercomWebViewBinding
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment
import ru.madbrains.smartyard.ui.main.address.adapters.ParentListAdapter
import ru.madbrains.smartyard.ui.main.address.auth.AuthFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogFragment
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.Flat
import ru.madbrains.smartyard.ui.main.address.models.ParentModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.EventLogModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModelP
import ru.madbrains.smartyard.ui.main.pay.WebViewPayViewModel
import ru.madbrains.smartyard.ui.main.settings.accessAddress.AccessAddressFragment
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber
import com.jakewharton.disklrucache.DiskLruCache
import ru.madbrains.smartyard.App
import ru.madbrains.smartyard.DiskCache
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@Deprecated("WebView not supported")
class IntercomWebViewFragment : Fragment() {
    lateinit var binding: FragmentIntercomWebViewBinding
    lateinit var webView: WebView
    lateinit var recyclerView: RecyclerView

    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private var uriHls = ""
    private val mPreferenceStorage: PreferenceStorage by inject()
    private val mWebViewPayViewModel by sharedViewModel<WebViewPayViewModel>()
    private val mViewModel by sharedViewModel<AddressViewModel>()
    private val mEventLog by sharedViewModel<EventLogViewModel>()
    private val mExoPlayerViewModel by sharedViewModel<ExoPlayerViewModel>()

    private var startY = 0f
    private var startX = 0f
    private val SWIPE_THRESHOLD = 400 // Минимальное расстояние для определения свайпа

    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()

    private var url = ""
    private var currentUrl = ""

    //    private var mCustomView: View? = null
    private var DEFAULT_URL = ""
    private var adapter: ParentListAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    private val listBitmap = mutableListOf<ImageItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIntercomWebViewBinding.inflate(inflater, container, false)
        return binding.root
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
            // Если фрагмента нет в стеке, то добавляем его с bundle
            if (bundle != null) {
                fragment.arguments = bundle
            }
            transaction.add(id, fragment, fragment.javaClass.simpleName)

            // Добавляем транзакцию в стек возврата только если это фрагмент, вызванный из этого метода
            // Не добавляем в стек, если фрагмент возвращает на главный экран
            if (!shouldReturnToMainScreen(fragment)) {
//                transaction?.addToBackStack(AddressWebViewFragment().javaClass.simpleName)
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
        fragment is IntercomWebViewFragment

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        observer()
//        swipeRefresh()
//        createWebViewClient()
/////////////////////////////////
        mCCTVViewModel.getCameras(VideoCameraModelP(0, "")) {
//            startGlide()
        }
    }


    private fun startGlide() {
        mCCTVViewModel.cameraList.value?.forEach {
//            downloadImage(it)
        }
    }


//    private fun downloadImage(data: CCTVData) {
//        val cache = DiskCache.getInstance(requireContext())
//        Glide.with(requireContext())
//            .asBitmap()
//            .error(R.drawable.placeholder)
//            .load(data.preview)
//            .into(object : CustomTarget<Bitmap>() {
//                override fun onResourceReady(p0: Bitmap, p1: Transition<in Bitmap>?) {
//                    cache.put(data.id.toString(), p0)
//                }
//
//                override fun onLoadCleared(placeholder: Drawable?) {
//
//                }
//
//                override fun onLoadFailed(errorDrawable: Drawable?) {
//                    super.onLoadFailed(errorDrawable)
//                }
//            })
//    }


    @SuppressLint("ClickableViewAccessibility")
    private fun swipeRefresh() {
        binding.swipeRefreshLayoutWebView.setOnRefreshListener {
            if (webView.scrollY == 0) {
                binding.swipeRefreshLayoutWebView.isEnabled = true
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    binding.swipeRefreshLayoutWebView.isRefreshing = false
                    binding.swipeRefreshLayoutWebView.isEnabled = false
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
                }

                MotionEvent.ACTION_MOVE -> {
                    // Вычислите расстояние свайпа
                    val deltaY = event.y - startY
                    val deltaX = event.x - startX
                    //Горизонтальный свайп
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        binding.swipeRefreshLayoutWebView.isEnabled = false
                    }
                    // Если расстояние свайпа достигло определенного значения, включите SwipeRefreshLayout
                    if (deltaY >= SWIPE_THRESHOLD && webView.scrollY == 0) {
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.swipeRefreshLayoutWebView.isEnabled = true
                    // Сброс состояния после завершения жеста
                }
            }
            false
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebViewClient() {
        WebView.enableSlowWholeDocumentDraw()
//        WebView.setWebContentsDebuggingEnabled(true) //TODO Debug chrome://inspect
        if (!this::webView.isInitialized) {
            webView = binding.webView
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            webView.addJavascriptInterface(this, "Android")
            ///////////////
//            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        }

        val bearerToken = mPreferenceStorage.authToken ?: ""
        val javascript = "bearerToken = function() { return \"$bearerToken\"; };"
        var mCustomView: View? = null
        var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
        var mFullscreenContainer: FrameLayout? = null
        var mOriginalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        var mOriginalSystemUiVisibility = 0

        webView.webChromeClient = object : WebChromeClient() {
            override fun getDefaultVideoPoster(): Bitmap? {
                val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                canvas.drawARGB(250, 243, 244, 250)
                return bitmap
            }

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
                    handler.removeCallbacksAndMessages(null)
                    webView.postVisualStateCallback(0, visualStateCallback)
                    webView.evaluateJavascript("javascript:$javascript", null)

                    if (urlClient != null && DEFAULT_URL != urlClient) {
                        if (currentUrl != urlClient && urlClient.length != DEFAULT_URL.length + 1) {
                            currentUrl = urlClient
                            try {
                                webViewUrlPager()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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
//                Timber.d("IntercomWebView onReceivedHttpError ${errorResponse?.statusCode}, request ${request?.requestHeaders}")
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
            when (uriType) {
                EVENTS -> {
                    val clientId = uri.getQueryParameter("clientId")
                    val houseId = uri.getQueryParameter("houseId")?.toInt()
                    val address = uri.getQueryParameter("address")
                    val flatsUri = uri.getQueryParameter("flats")

                    val gson = Gson()
                    val flatUriArray = gson.fromJson(flatsUri, Array<Flat>::class.java)
                    val flatsList = flatUriArray?.toList()
                    var flatId = 0
                    var flatNumber = ""
                    var frsEnabled = false
                    var flats = listOf<Flat>()
                    val listFlats = mutableListOf<Flat>()
                    flatsList?.forEach {
                        flatId = it.flatId
                        flatNumber = it.flatNumber
                        frsEnabled = !it.frsEnabled
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
                        listFlats.add(
                            Flat(
                                flatId = it.flatId,
                                flatNumber = it.flatNumber,
                                frsEnabled = it.frsEnabled
                            )
                        )
                        if (flatId == it.flatId) {
                            if (flatsList != null) {
                                mEventLog.flatsAll = flatsList
                            }
                            mEventLog.address = address ?: ""
                            mEventLog.flatsAll = listFlats
                            mEventLog.filterFlat = null
                            mEventLog.lastLoadedDayFilterIndex.value = -1
                            mEventLog.currentEventItem = null
                            mEventLog.getAllFaces()
                            navigateToFragment(R.id.cl_fragment_wv_intercom, EventLogFragment())
                        }
                    }
                }

                CAM_MAP -> {
                    val houseId = uri.getQueryParameter("houseId")
                        .let { if (it.isNullOrEmpty()) 0 else it.toInt() }
                    val address = uri.getQueryParameter("address").toString()
                    mCCTVViewModel.getCameras(VideoCameraModelP(houseId, address)) {
//                    navigateToFragment(R.id.cl_fragment_wv_intercom, CCTVMapFragment())
                        mCCTVViewModel.chooseCamera(0)
                        navigateToFragment(R.id.cl_fragment_wv_intercom, CCTVDetailFragment())
                    }
                }

                FULL_SCREEN_PLAYER -> {
                    uriHls = uri.getQueryParameter("url").toString() + "/index.m3u8"
                    val bundle = Bundle()

                    val domophoneId = uri.getQueryParameter("domophoneId")
                    val doorId = uri.getQueryParameter("doorId")
                    val title = uri.getQueryParameter("title")
                    val token = uri.getQueryParameter("token")
                    val name = uri.getQueryParameter("name")
                    val camId = uri.getQueryParameter("camId")?.toInt() ?: -1

//                    mExoPlayerViewModel.getCameras(
//                        VideoCameraModelP(0, ""), camId
//                    ) {
//                        navigateToFragment(R.id.cl_fragment_wv_intercom, ExoPlayerFragment())
//                    }
                    //TODO START EXOPLAYER

                    mCCTVViewModel.getCameras(VideoCameraModelP(0, "")) {
                        mCCTVViewModel.chooseCameraById(camId)
                        navigateToFragment(R.id.cl_fragment_wv_intercom, CCTVDetailFragment())
                    } //TODO START_CAMERA_WITH_ARCHIVE

//                uriHls = uri.getQueryParameter("url").toString() + "/index.m3u8?token=$token"
//                if (!domophoneId.isNullOrEmpty()) {
//                    bundle.putLong("domophoneId", domophoneId.toLong())
//                    bundle.putInt("doorId", doorId!!.toInt())
//                    bundle.putString("title", title)
//                    bundle.putString("name", name)
//                }
//                bundle.putString("uriHls", uriHls)
//                navigateToFragment(R.id.cl_fragment_wv_intercom, ExoPlayerIntercomWebView(), bundle)
                    //TODO FULL SCREEN MODE
                }

                ADD_CONTRACT -> {
                    navigateToFragment(R.id.cl_fragment_wv_intercom, AuthFragment())
                }

                SHARE_OPEN_DOOR -> {
                    val title = uri.getQueryParameter("title")
                    val text = uri.getQueryParameter("text")
                    val urlShare = uri.getQueryParameter("url")

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "$title $text $urlShare")
                        type = "text/plain"
                    }
                    // Запуск активности Share
                    startActivity(Intent.createChooser(sendIntent, null))
                }

                SETTINGS -> {
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
                    if (!bundleSettings.isEmpty) {
                        navigateToFragment(
                            R.id.cl_fragment_wv_intercom,
                            AccessAddressFragment(),
                            bundleSettings
                        )
                    }
                }
            }
        }
    }

    private fun observer() {
        val observer = Observer<List<ItemOption>> {
            Timber.d("IntercomWebView OBSERVER ${mWebViewPayViewModel.options.value}")
            if (mWebViewPayViewModel.options.value?.size!! == 1) {
                url = mWebViewPayViewModel.options.value!![0].intercomScreenUrl.toString()
                if (DEFAULT_URL.isEmpty()) {
                    DEFAULT_URL =
                        mWebViewPayViewModel.options.value!![0].intercomScreenUrl.toString()
                }
                loadUrl()
            }
        }
        mWebViewPayViewModel.options.observe(viewLifecycleOwner, observer)
    }


    @SuppressLint("NotifyDataSetChanged")
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

    private fun setCountEvents(json: String) {
        webView.evaluateJavascript(json, null);
    }


    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == AddressWebViewFragment.REFRESH_INTENT) {
                    loadUrl()
                }
                if (intent.action == SET_COUNT_EVENTS) {
                    val json = intent.getStringExtra("yard").toString()
                    setCountEvents(json)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(AddressWebViewFragment.REFRESH_INTENT)
        intentFilter.addAction(SET_COUNT_EVENTS)
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                intentFilter
            )
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }


    private fun loadUrl() {
        if (DEFAULT_URL.isNotEmpty()) {
            binding.webView.loadUrl(DEFAULT_URL)
            webViewUrlPager()
        }
    }

    companion object {
        const val SET_COUNT_EVENTS = "SET_COUNT_EVENTS"
        private const val EVENTS = "events"
        private const val CAM_MAP = "map"
        private const val FULL_SCREEN_PLAYER = "fullscreen"
        private const val ADD_CONTRACT = "addContract"
        private const val SHARE_OPEN_DOOR = "share"
        private const val SETTINGS = "conf"
    }
}