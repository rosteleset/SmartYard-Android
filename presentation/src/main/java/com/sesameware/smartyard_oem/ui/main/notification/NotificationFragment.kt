package com.sesameware.smartyard_oem.ui.main.notification

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.ProviderConfig
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.FirebaseMessagingService
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentNotificationBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import timber.log.Timber

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<NotificationViewModel>()
    private var mLoaded: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun resize(height: Float) {
            activity?.runOnUiThread {
                val viewGroup = binding.webViewNotification.layoutParams
                viewGroup?.height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    height,
                    resources.displayMetrics
                ).toInt()
                binding.webViewNotification.layoutParams = viewGroup
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel.onStart()
        binding.webViewNotification.settings.javaScriptEnabled = true
        binding.webViewNotification.addJavascriptInterface(WebAppInterface(), "AndroidFunction")
        binding.webViewNotification.webViewClient = object : WebViewClient() {
            private val URL = "javascript:AndroidFunction.resize(document.body.scrollHeight)"
            override fun onLoadResource(view: WebView?, url: String?) {
                if (url != null && url.endsWith(".mp4")) {
                    view?.stopLoading()
                    val downloadManager: DownloadManager =
                        requireActivity().getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    val uri = Uri.parse(url)
                    val title = "${getText(R.string.video_fragment)}_${System.currentTimeMillis()}"
                    val request =
                        DownloadManager.Request(uri)
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    request.setTitle(title)
                    request.setDescription(getText(R.string.downloading_fragment))
                    request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE
                    )
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "$title.mp4"
                    )
                    downloadManager.enqueue(request)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.loadUrl(URL)
                if (!mLoaded) {
                    mViewModel.finishedLoading()
                    mLoaded = true
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                Intent(Intent.ACTION_VIEW, request?.url).apply {
                    startActivity(this)
                }
                return true
            }
        }
        binding.refreshLayout.setOnRefreshListener {
            mViewModel.loadInbox()
        }
        mViewModel.loaded.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.webViewNotification.loadDataWithBaseURL(
                    it.basePath,
                    it.code,
                    "text/html", "UTF-8", null
                )
                if (DataModule.providerConfig.mainMenu?.contains(ProviderConfig.MAIN_MENU_NOTIFICATIONS) == true) {
                    (activity as MainActivity).removeBadge()
                }
            }
        )
        mViewModel.progress.observe(
            viewLifecycleOwner,
            Observer { progress ->
                binding.refreshLayout.isRefreshing = progress
            }
        )
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mViewModel.loadInbox()
            intent?.extras?.let {
                intentParse(it)
            }
        }
    }

    private fun intentParse(bundle: Bundle) {
        val messageId =
            bundle.getString(FirebaseMessagingService.NOTIFICATION_MESSAGE_ID, "")
        if (messageId.isNotEmpty()) {
            mViewModel.delivered(messageId)
        }
        cancelNotificationAll()
    }

    private fun cancelNotificationAll() {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm hidden: $hidden")
        if (hidden) {
            unregister()
        } else {
            refresh()
        }
        super.onHiddenChanged(hidden)
    }

    override fun onResume() {
        super.onResume()
        Timber.d("debug_dmm isVisible: $isVisible")
        if (isVisible) {
            refresh()
        }
    }

    override fun onStop() {
        unregister()
        super.onStop()
    }

    private fun refresh() {
        cancelNotificationAll()
        mViewModel.loadInbox()
        activity?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                IntentFilter(BROADCAST_ACTION_NOTIF)
            )
        }
    }
    private fun unregister() {
        activity?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }

    companion object {
        const val BROADCAST_ACTION_NOTIF = "broadcast_action_notif"
    }
}
