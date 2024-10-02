package com.sesameware.smartyard_oem.ui.custom_web_view

import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.app.ActivityCompat.startActivityForResult
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.MainActivity.Companion.WEB_CHAT_CHOOSE_FILE
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
        if (isUserGesture) {
            //пытаемся получить URL
            val href = view?.handler?.obtainMessage()
            view?.requestFocusNodeHref(href)
            href?.data?.getString("url")?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fragment?.requireActivity()?.startActivity(intent) ?: bottomFragment?.requireActivity()?.startActivity(intent)
            }

            return true
        }

        return false
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        fileChooserParams?.createIntent()?.let {
            startActivityForResult(fragment?.requireActivity()!!,
                it, WEB_CHAT_CHOOSE_FILE, null)
        }
        (fragment?.requireActivity() as? MainActivity)?.filePathCallback = filePathCallback
        return true
    }
}
