package com.sesameware.smartyard_oem.ui.main.burger

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebBottomBinding
import com.sesameware.smartyard_oem.databinding.FragmentExtWebBottomBinding
import com.sesameware.smartyard_oem.ui.custom_web_view.CustomWebChromeClient
import com.sesameware.smartyard_oem.ui.custom_web_view.CustomWebInterface
import com.sesameware.smartyard_oem.ui.custom_web_view.CustomWebViewClient
import timber.log.Timber

class ExtWebBottomFragment : BottomSheetDialogFragment() {
    //private var _binding: FragmentExtWebBottomBinding? = null
    private var _binding: FragmentCustomWebBottomBinding? = null
    private val binding get() = _binding!!
    private var url: String? = null
    private var hostName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        
        arguments?.let {
            //url = ExtWebBottomFragmentArgs.fromBundle(it).url
            hostName = Uri.parse(url)?.host ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //_binding = FragmentExtWebBottomBinding.inflate(inflater, container, false)
        _binding = FragmentCustomWebBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setDefaultLayout() {
        val lp = binding.wvExtBottom.layoutParams
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.wvExtBottom.layoutParams = lp
        binding.root.requestLayout()
    }

    //пересчитываем высоту WebView для правильной работы скроллинга
    private fun changeLayout() {
        val position = IntArray(2)
        binding.wvExtBottom.getLocationOnScreen(position)
        val newHeight = requireContext().resources.displayMetrics.heightPixels - position[1]
        //Timber.d("debug_web changeLayout newHeight = $newHeight")
        if (newHeight > 0) {
            val lp = binding.wvExtBottom.layoutParams
            lp.height = newHeight
            binding.wvExtBottom.layoutParams = lp
            binding.root.requestLayout()
        } else {
            setDefaultLayout()
        }
    }

    private fun disableSomeEvents() {
        binding.wvExtBottom.setOnLongClickListener {
            true
        }

        binding.wvExtBottom.setOnDragListener { _, _ ->
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*binding.wvExtBottom.settings.javaScriptEnabled = true
        binding.wvExtBottom.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Timber.d("debug_web console: ${consoleMessage?.message()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }

        binding.wvExtBottom.webViewClient = object : WebViewClient() {
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

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                Timber.d("debug_web pageStarted")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                Timber.d("debug_web pageFinished")
                //"костыль" с отсроченным вызовом пересчёта высоты WebView для правильной работы скроллинга
                Handler().postDelayed({
                    changeLayout()
                }, 800)
            }
        }

        binding.wvExtBottom.addJavascriptInterface(ExtWebInterface(viewModel,
            object : ExtWebInterface.Callback {
                override fun onPostLoadingStarted() {
                    requireActivity().runOnUiThread {
                        binding.pbWebViewBottom.visibility = View.VISIBLE
                    }
                }

                override fun onPostLoadingFinished() {
                    requireActivity().runOnUiThread {
                        binding.pbWebViewBottom.visibility = View.INVISIBLE
                    }
                }
            }),
            ExtWebInterface.WEB_INTERFACE_OBJECT)

        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Timber.d("debug_web newState = $newState")
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        changeLayout()
                    } else if (newState != BottomSheetBehavior.STATE_SETTLING && newState != BottomSheetBehavior.STATE_DRAGGING) {
                        setDefaultLayout()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    //ничего не делаем
                }
            })
            it.setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        if (url != null) {
            binding.wvExtBottom.loadUrl(url!!)
        }

        /*binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            Timber.d("debug_web webView height = ${binding.wvExtBottom.height}")
        }*/

        binding.wvExtBottom.clearCache(true)
        disableSomeEvents()*/

        binding.wvExtBottom.settings.javaScriptEnabled = true
        //binding.wvExtBottom.webChromeClient = CustomWebChromeClient(null, this)
        //binding.wvExtBottom.webViewClient = CustomWebViewClient(null, this)
        binding.wvExtBottom.addJavascriptInterface(CustomWebInterface(object : CustomWebInterface.Callback {
            override fun onPostLoadingStarted() {
                requireActivity().runOnUiThread {
                    binding.pbWebViewBottom.visibility = View.VISIBLE
                }
            }

            override fun onPostLoadingFinished() {
                requireActivity().runOnUiThread {
                    binding.pbWebViewBottom.visibility = View.INVISIBLE
                }
            }
        }), CustomWebInterface.WEB_INTERFACE_OBJECT)

        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Timber.d("debug_web newState = $newState")
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        changeLayout()
                    } else if (newState != BottomSheetBehavior.STATE_SETTLING && newState != BottomSheetBehavior.STATE_DRAGGING) {
                        setDefaultLayout()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    //ничего не делаем
                }
            })
            it.setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.wvExtBottom.loadUrl(url ?: "")
        binding.wvExtBottom.clearCache(true)
        disableSomeEvents()
    }
}
