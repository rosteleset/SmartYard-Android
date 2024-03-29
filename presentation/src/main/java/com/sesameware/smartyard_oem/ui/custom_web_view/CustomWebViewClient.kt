package com.sesameware.smartyard_oem.ui.custom_web_view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import org.koin.ext.clearQuotes
import timber.log.Timber

class CustomWebViewClient(
    private val fragmentId: Int,
    private val popupId: Int,
    private val fragment: CustomWebViewFragment? = null,
    private val bottomFragment: CustomWebBottomFragment? = null

) : WebViewClient() {
    private var pageTitle = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        try {
            request?.url?.toString()?.let { url ->
                if (!(url.startsWith("http:") || url.startsWith("https:"))
                    || url.contains(ANCHOR_EXTERNAL)) {
                    Intent(Intent.ACTION_VIEW, request.url).apply {
                        if (bottomFragment != null) {
                            bottomFragment.requireActivity().startActivity(this)
                        }
                        else {
                            fragment?.requireActivity()?.startActivity(this)
                        }
                    }

                    return true
                } else {
                    if (url.contains(ANCHOR_PUSH)) {
                        if (bottomFragment != null) {
                            bottomFragment.dismiss()
                            val f = bottomFragment.requireActivity().supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as? CustomWebViewFragment
                            f?.findNavController()?.navigate(fragmentId,
                                Bundle().apply {
                                    putInt(CustomWebViewFragment.FRAGMENT_ID, fragmentId)
                                    putInt(CustomWebViewFragment.POPUP_ID, popupId)
                                    putString(CustomWebViewFragment.BASE_PATH, url)
                                    putString(CustomWebViewFragment.CODE, "")
                                    putString(CustomWebViewFragment.TITLE, (f.binding.wvExt.webViewClient as CustomWebViewClient).pageTitle)
                                    putBoolean(CustomWebViewFragment.HAS_BACK_BUTTON, true)
                                })

                            return true
                        }

                        fragment?.findNavController()?.navigate(
                            fragmentId,
                            Bundle().apply {
                                putInt(CustomWebViewFragment.FRAGMENT_ID, fragmentId)
                                putInt(CustomWebViewFragment.POPUP_ID, popupId)
                                putString(CustomWebViewFragment.BASE_PATH, url)
                                putString(CustomWebViewFragment.CODE, "")
                                putString(CustomWebViewFragment.TITLE, pageTitle)
                                putBoolean(CustomWebViewFragment.HAS_BACK_BUTTON, true)
                            })

                        return true
                    }

                    if (url.contains(ANCHOR_REPLACE)) {
                        if (bottomFragment != null) {
                            bottomFragment.dismiss()
                            val f = bottomFragment.requireActivity().supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as? CustomWebViewFragment
                            if (f != null) {
                                val option = NavOptions.Builder()
                                    .setPopUpTo(fragmentId, true)
                                    .build()
                                f.findNavController().navigate(fragmentId,
                                    Bundle().apply {
                                        putInt(CustomWebViewFragment.FRAGMENT_ID, fragmentId)
                                        putInt(CustomWebViewFragment.POPUP_ID, popupId)
                                        putString(CustomWebViewFragment.BASE_PATH, url)
                                        putString(CustomWebViewFragment.CODE, "")
                                        putString(CustomWebViewFragment.TITLE, f.binding.tvEWVTitle.text.toString())
                                        putBoolean(CustomWebViewFragment.HAS_BACK_BUTTON, f.hasBackButton)
                                    }, option)
                            }

                            return true
                        }

                        val option = NavOptions.Builder()
                            .setPopUpTo(fragmentId, true)
                            .build()
                        fragment?.findNavController()?.navigate(fragmentId,
                            Bundle().apply {
                                putInt(CustomWebViewFragment.FRAGMENT_ID, fragmentId)
                                putInt(CustomWebViewFragment.POPUP_ID, popupId)
                                putString(CustomWebViewFragment.BASE_PATH, url)
                                putString(CustomWebViewFragment.CODE, "")
                                putString(CustomWebViewFragment.TITLE, fragment.binding.tvEWVTitle.text.toString())
                                putBoolean(CustomWebViewFragment.HAS_BACK_BUTTON, true)
                            }, option)

                        return true
                    }

                    if (url.contains(ANCHOR_POPUP)) {
                        if (bottomFragment != null) {
                            val option = NavOptions.Builder()
                                .setPopUpTo(popupId, true)
                                .build()
                            bottomFragment.findNavController().navigate(popupId,
                                Bundle().apply {
                                    putInt(CustomWebBottomFragment.FRAGMENT_ID, fragmentId)
                                    putInt(CustomWebBottomFragment.POPUP_ID, popupId)
                                    putString(CustomWebBottomFragment.URL, url)
                                }, option)

                            return true
                        }

                        fragment?.findNavController()?.navigate(popupId,
                            Bundle().apply {
                                putInt(CustomWebBottomFragment.FRAGMENT_ID, fragmentId)
                                putInt(CustomWebBottomFragment.POPUP_ID, popupId)
                                putString(CustomWebBottomFragment.URL, url)
                            })

                        return true
                    }

                    if (url.contains(ANCHOR_CLOSE)) {
                        bottomFragment?.dismiss()
                        Intent(Intent.ACTION_VIEW, request.url).apply {
                            bottomFragment?.requireActivity()?.startActivity(this)
                        }

                        return true
                    }

                    return false
                }
            }
        } catch (e: ActivityNotFoundException) {
            Timber.d("debug_web ActivityNotFoundException")
            return true
        } catch (e: Throwable) {
            Timber.d("debug_web Throwable")
            return true
        }

        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        Timber.d("debug_web onPageStarted = $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        Timber.d("debug_web onPageFinished = $url")
        fragment?.binding?.wvExt?.evaluateJavascript("document.title") {
            pageTitle = it.clearQuotes()
        }
        CookieManager.getInstance().apply {
            Timber.d("debug_web cookie: ${getCookie(url)}")
            setAcceptCookie(true)
            acceptCookie()
            flush()
        }

        /*if (bottomFragment != null) {
            //"костыль" с отсроченным вызовом пересчёта высоты WebView для правильной работы скроллинга
            Handler().postDelayed({
                bottomFragment.changeLayout()
            }, 800)
        }*/
    }

    companion object {
        const val ANCHOR_PUSH = "#smart-yard-push"
        const val ANCHOR_REPLACE = "#smart-yard-replace"
        const val ANCHOR_EXTERNAL = "#smart-yard-external"
        const val ANCHOR_POPUP = "#smart-yard-popup"
        const val ANCHOR_CLOSE = "#smart-yard-close"
    }
}
