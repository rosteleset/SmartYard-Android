package com.sesameware.smartyard_oem.ui.custom_web_view

import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.sesameware.smartyard_oem.databinding.FragmentExtWebBottomBinding
import com.sesameware.smartyard_oem.ui.main.burger.ExtWebBottomFragment
import timber.log.Timber

class CustomWebChromeClient(
    private val fragment: CustomWebViewFragment? = null,
    private val bottomFragment: CustomWebBottomFragment? = null,
) : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Timber.d("debug_web error message: ${consoleMessage?.message()}")
        Timber.d("debug_web error line number: ${consoleMessage?.lineNumber()}")
        Timber.d("debug_web error source: ${consoleMessage?.sourceId()}")
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        Timber.d("debug_web: isDialog = $isDialog    isUserGesture = $isUserGesture    resultMsg = $resultMsg")
        if (isUserGesture) {
            //пытаемся получить URL
            val href = view?.handler?.obtainMessage()
            view?.requestFocusNodeHref(href)
            href?.data?.getString("url")?.let { url ->
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    fragment?.requireActivity()?.startActivity(this@apply)
                }
            }

            return true
        }

        return false
    }
}
