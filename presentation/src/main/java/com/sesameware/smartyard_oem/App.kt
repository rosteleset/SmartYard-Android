package com.sesameware.smartyard_oem

import android.app.Application
import android.webkit.WebView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.sesameware.smartyard_oem.di.Modules
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
        initLogger()
        AndroidThreeTen.init(this)
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@App)
            fragmentFactory()
            modules(Modules.get())
        }
    }

    private fun initLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            WebView.setWebContentsDebuggingEnabled(true)
        }
        //Timber.plant(Timber.DebugTree())
        //WebView.setWebContentsDebuggingEnabled(true)
    }

    companion object {
        const val release = "release"
    }
}
