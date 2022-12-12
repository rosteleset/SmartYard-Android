package com.sesameware.smartyard_oem.ui.custom_web_view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCustomWebBottomBinding
import timber.log.Timber

class CustomWebBottomFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentCustomWebBottomBinding? = null
    val binding get() = _binding!!

    private var url = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)

        arguments?.let {
            url = it.getString(URL, url)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomWebBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.wvExtBottom.settings.javaScriptEnabled = true
        binding.wvExtBottom.webChromeClient = CustomWebChromeClient(null, this)
        binding.wvExtBottom.webViewClient = CustomWebViewClient(null, this)
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

        //костыль для подгона высоты
        /*dialog?.let {
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
        }*/

        binding.wvExtBottom.loadUrl(url)
        binding.wvExtBottom.clearCache(true)
        disableSomeEvents()
    }

    //костыль: пересчитываем высоту WebView для правильной работы скроллинга
    /*fun changeLayout() {
        Timber.d("debug_web changeLayout")
        val position = IntArray(2)
        binding.wvExtBottom.getLocationOnScreen(position)
        val newHeight = requireContext().resources.displayMetrics.heightPixels - position[1]
        Timber.d("debug_web changeLayout newHeight = $newHeight")
        if (newHeight > 0) {
            val lp = binding.wvExtBottom.layoutParams
            lp.height = newHeight
            binding.wvExtBottom.layoutParams = lp
            binding.root.requestLayout()
        } else {
            setDefaultLayout()
        }
    }

    private fun setDefaultLayout() {
        val lp = binding.wvExtBottom.layoutParams
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.wvExtBottom.layoutParams = lp
        binding.root.requestLayout()
    }*/

    private fun disableSomeEvents() {
        binding.wvExtBottom.setOnLongClickListener {
            true
        }

        binding.wvExtBottom.setOnDragListener { _, _ ->
            true
        }
    }

    companion object {
        const val URL = "url"

        @JvmStatic
        fun newInstance(url: String) =
            CustomWebBottomFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, url)
                }
            }
    }
}
