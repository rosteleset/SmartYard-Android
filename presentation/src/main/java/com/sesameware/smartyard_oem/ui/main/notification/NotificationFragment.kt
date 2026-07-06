package com.sesameware.smartyard_oem.ui.main.notification

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentNotificationBinding
import com.sesameware.smartyard_oem.ui.applyStatusBarInset
import com.sesameware.smartyard_oem.ui.injectColorScheme
import com.sesameware.smartyard_oem.ui.injectInsets
import com.sesameware.smartyard_oem.ui.setInsetsListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<NotificationViewModel>()
    private var mLoaded: Boolean = false

    private var windowInsets: WindowInsetsCompat? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        binding.refreshLayout.applyStatusBarInset()
        binding.webViewNotification.setInsetsListener { windowInsets = it }
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.onStart()
        binding.refreshLayout.setOnChildScrollUpCallback { _, _ ->
            binding.webViewNotification.scrollY > 0
        }
        binding.webViewNotification.settings.allowContentAccess = true
        binding.webViewNotification.settings.allowFileAccess = true
        binding.webViewNotification.settings.domStorageEnabled = true
        binding.webViewNotification.settings.databaseEnabled = true
        binding.webViewNotification.settings.javaScriptEnabled = true
        binding.webViewNotification.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.injectColorScheme()
                windowInsets?.let { view?.injectInsets(it) }

                if (!mLoaded) {
                    mViewModel.finishedLoading()
                    mLoaded = true
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                if (url.contains("/cctv/download/", ignoreCase = true) ||
                    url.substringBefore("?").endsWith(".mp4", ignoreCase = true)) {
                        downloadVideo(url)
                        return true
                    }

                Intent(Intent.ACTION_VIEW, request.url).apply {
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
            }
        )
        mViewModel.progress.observe(
            viewLifecycleOwner
        ) { progress ->
            binding.refreshLayout.isRefreshing = progress
        }
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mViewModel.loadInbox()
            intentParse()
        }
    }

    private fun downloadVideo(url: String) {
        val downloadManager = requireActivity()
            .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val title = "${getText(R.string.video_fragment)}_${System.currentTimeMillis()}"
        val description = getText(R.string.downloading_fragment)

        val request = DownloadManager.Request(url.toUri()).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setTitle(title)
            setDescription(description)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$title.mp4")
        }
        downloadManager.enqueue(request)

        Toast.makeText(requireContext(),"$description $title.mp4",
            Toast.LENGTH_SHORT).show()
    }

    private fun intentParse() {
        cancelNotificationAll()
    }

    private fun cancelNotificationAll() {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
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
