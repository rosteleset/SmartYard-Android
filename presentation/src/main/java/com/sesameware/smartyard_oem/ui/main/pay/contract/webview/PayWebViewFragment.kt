package com.sesameware.smartyard_oem.ui.main.pay.contract.webview

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
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentPayWebViewBinding

class PayWebViewFragment : Fragment() {
    private var _binding: FragmentPayWebViewBinding? = null
    private val binding get() = _binding!!

    private val payWebViewViewModel by viewModel<PayWebViewViewModel>()

    private val args: PayWebViewFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPayWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.loadUrl(args.url)
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress < 100) {
                    binding.progressBar2.visibility = ProgressBar.VISIBLE
                }
                if (progress == 100) {
                    binding.progressBar2.visibility = ProgressBar.GONE
                }
            }
        }
        binding.ivClose.setOnClickListener {
            this@PayWebViewFragment.findNavController().popBackStack()
            val action =
                PayWebViewFragmentDirections.actionGlobalErrorBottomSheetDialogFragment(
                    getString(R.string.payments_error_3)
                )
            this.findNavController()
                .navigate(action)
        }
        binding.webView.webViewClient = object : WebViewClient() {
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
            .navigate(R.id.successBottomSheetDialogFragment)
    }
}
