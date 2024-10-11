package ru.madbrains.smartyard.ui.main.pay

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.component.KoinComponent
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.model.response.ItemOption
import ru.madbrains.smartyard.databinding.FragmentWebviewPayBinding


class WebViewPayFragment : Fragment(), KoinComponent {
    lateinit var binding: FragmentWebviewPayBinding
    lateinit var webView: WebView

    private val mPreferenceStorage: PreferenceStorage by inject()
    private val mWebViewPayViewModel by sharedViewModel<WebViewPayViewModel>()

    private var url = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observer()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWebviewPayBinding.inflate(inflater, container, false)

        return  binding.root
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun observer() {
        val messageObserver = Observer<List<ItemOption>>{
            webView = binding.webView
            url = mWebViewPayViewModel.options.value!![0].paymentsUrl.toString()
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            loadUrl()

//        WebView.setWebContentsDebuggingEnabled(true) //Debug chrome://inspect
            val bearerToken = mPreferenceStorage.authToken ?: ""
            val javascript = "bearerToken = function() { return \"$bearerToken\"; };"

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Выполнение JavaScript после полной загрузки страницы
                    webView.visibility = View.VISIBLE
                    binding.webViewProgressBar.visibility = View.GONE
                    webView.evaluateJavascript("javascript:$javascript", null)
                }
            }
        }
        mWebViewPayViewModel.options.observe(this, messageObserver)
    }


    private fun loadUrl(){
        webView.loadUrl(url)
    }

}