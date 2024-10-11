package ru.madbrains.smartyard

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.madbrains.smartyard.di.Modules
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
    }

//    private fun initYandex() {
//        val apiKey = "your-api-key"
//        val config = YandexMetricaConfig.newConfigBuilder(apiKey).build()
//        YandexMetrica.activate(applicationContext, config)
//        YandexMetrica.enableActivityAutoTracking(this)
//    }

    companion object {
        const val release = "release"
    }
}
