package ru.madbrains.smartyard.ui.main.burger

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.*
import androidx.fragment.app.Fragment
import android.webkit.*
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.databinding.FragmentExtWebViewBinding
import timber.log.Timber
import java.io.ByteArrayInputStream

class ExtWebViewFragment : Fragment() {
    private var _binding: FragmentExtWebViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExtWebViewModel by sharedViewModel()

    private var basePath: String? = null
    private var code: String? = null
    private var hostName: String = ""

    fun goBack(): Boolean {
        return if (binding.wvExt.canGoBack()) {
            binding.wvExt.goBack()
            true
        } else {
            false
        }
    }

    private fun disableSomeEvents() {
        binding.wvExt.setOnLongClickListener {
            true
        }

        binding.wvExt.setOnDragListener { _, _ ->
            true
        }
    }

    private fun setTitle(title: String?) {
        if (title != null) {
            var newTitle = title

            //удаляем кавычки или апострофы в начале и конце строки
            if (newTitle.startsWith("\"") || newTitle.startsWith("'")) {
                newTitle = newTitle.drop(1)
            }
            if (newTitle.endsWith("\"") || newTitle.endsWith("'")) {
                newTitle = newTitle.dropLast(1)
            }

            binding.tvEWVTitle.text = if (binding.wvExt.canGoBack()) newTitle else "Меню"
        } else {
            binding.tvEWVTitle.text = ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            basePath = ExtWebViewFragmentArgs.fromBundle(it).basePath
            code = ExtWebViewFragmentArgs.fromBundle(it).code
            if (basePath != null) {
                hostName = Uri.parse(basePath)?.host ?: ""
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flExtWebView.clipToOutline = true
        binding.wvExt.clipToOutline = true

        binding.wvExt.settings.javaScriptEnabled = true
        binding.wvExt.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.wvExt.settings.setSupportMultipleWindows(true)
        binding.wvExt.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Timber.d("debug_web console: ${consoleMessage?.message()}")
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                //пытаемся получить URL
                val href = view?.handler?.obtainMessage()
                view?.requestFocusNodeHref(href)
                val url = href?.data?.getString("url");
                //Timber.d("debug_web onCreateWindow: isDialog = $isDialog   url = $url")
                if (url?.isNotEmpty() == true) {
                    val action = ExtWebViewFragmentDirections.actionExtWebViewFragmentToExtWebBottomFragment(url)
                    findNavController().navigate(action)
                }
                return false
            }

            //не всегда срабатывает, например, если перешли на предыдущую страницу то это событие не срабатывает;
            //поэтому заголовок документа также получаем после окончания загрузки страницы (событие onPageFinished)
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                Timber.d("debug_web title = $title")
                setTitle(title)
            }
        }

        binding.wvExt.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request?.url?.host == hostName) {
                    return false
                }
                Intent(Intent.ACTION_VIEW, request?.url).apply {
                    startActivity(this)
                }
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return if (request?.url?.path?.endsWith("lanta.js") == true) {
                    WebResourceResponse("text/javascript", "utf-8",
                        ByteArrayInputStream(ExtWebInterface.JS_INJECTION.toByteArray()))
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Timber.d("debug_web pageStarted = $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                //так как не всегда обновляется заголовок документа, то получем его принудительно через JS
                binding.wvExt.evaluateJavascript("document.title") {
                    setTitle(it)
                }
            }
        }

        binding.wvExt.addJavascriptInterface(ExtWebInterface(viewModel,
            object : ExtWebInterface.Callback {
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
            }),
            ExtWebInterface.WEB_INTERFACE_OBJECT)

        if (code != null) {
            binding.wvExt.loadDataWithBaseURL(basePath, code!!, "text/html", "utf-8", null)
        }

        binding.ivEWVBack.setOnClickListener {
            goBack()
        }

        binding.wvExt.clearCache(true)
        disableSomeEvents()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.onPostRefreshParent.observe(
            viewLifecycleOwner,
            EventObserver {
                val handler = Handler()
                handler.postDelayed({
                    //binding.wvExt.reload()
                    binding.wvExt.url?.let { url ->
                        val newUrl = url + if (!url.contains("forceRefresh", true)) {
                            "&forceRefresh=1"
                        } else {
                            ""
                        }
                        Timber.d("debug_web reload parent url = $newUrl")
                        binding.wvExt.clearCache(true)
                        binding.wvExt.evaluateJavascript("document.location.replace('$newUrl');") {}
                    }
                }, it * 1000L)
            }
        )
    }
}
