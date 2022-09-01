package com.sesameware.smartyard_oem

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.sesameware.smartyard_oem.di.Modules
import timber.log.Timber

class App : Application() {

    var isChatActive = false

    override fun onCreate() {
        super.onCreate()
        initKoin()
        initLogger()
        AndroidThreeTen.init(this)
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@App)
            modules(Modules.allModules())
        }
    }

    private fun initLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        const val release = "release"
    }
}
