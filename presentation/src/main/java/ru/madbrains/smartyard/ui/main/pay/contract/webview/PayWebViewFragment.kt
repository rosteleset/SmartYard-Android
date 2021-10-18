package ru.madbrains.smartyard.ui.main.pay.contract.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_pay_web_view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.R

class PayWebViewFragment : Fragment() {

    private val payWebViewViewModel by viewModel<PayWebViewViewModel>()

    private val args: PayWebViewFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pay_web_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(args.url)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress < 100) {
                    progressBar2?.visibility = ProgressBar.VISIBLE
                }
                if (progress == 100) {
                    progressBar2?.visibility = ProgressBar.GONE
                }
            }
        }
        ivClose.setOnClickListener {
            this@PayWebViewFragment.findNavController().popBackStack()
            val action =
                PayWebViewFragmentDirections.actionGlobalErrorButtomSheetDialogFragment(
                    getString(R.string.payments_error_3)
                )
            this.findNavController()
                .navigate(action)
        }
        webView.webViewClient = object : WebViewClient() {
            /**
             * Notify the host application that a page has finished loading.
             * @param view The WebView that is initiating the callback.
             * @param url The url of the page.
             */
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("finish.html") == true) {
                    finishPay()
                }
            }
        }
    }

    fun finishPay() {
        this@PayWebViewFragment.findNavController().popBackStack()
        this@PayWebViewFragment.findNavController()
            .navigate(R.id.successButtomSheetDialogFragment)
    }
}
