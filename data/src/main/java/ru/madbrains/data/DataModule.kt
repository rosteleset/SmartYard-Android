package ru.madbrains.data

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.madbrains.data.interceptors.CommonInterceptor
import ru.madbrains.data.interceptors.SessionInterceptor
import ru.madbrains.data.local.db.ItemsDatabase
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.data.prefs.SharedPreferenceStorage
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.data.repository.AddressRepositoryImpl
import ru.madbrains.data.repository.DatabaseRepositoryImpl
import ru.madbrains.data.repository.SipRepositoryImpl
import ru.madbrains.data.repository.AuthRepositoryImpl
import ru.madbrains.data.repository.GeoRepositoryImpl
import ru.madbrains.data.repository.IssueRepositoryImpl
import ru.madbrains.data.repository.CCTVRepositoryImpl
import ru.madbrains.data.repository.InboxRepositoryImpl
import ru.madbrains.data.repository.PayRepositroyImpl
import ru.madbrains.data.repository.FRSRepositoryImpl
import ru.madbrains.data.repository.ExtRepositoryImpl
import ru.madbrains.domain.interfaces.AddressRepository
import ru.madbrains.domain.interfaces.AuthRepository
import ru.madbrains.domain.interfaces.SipRepository
import ru.madbrains.domain.interfaces.DatabaseRepository
import ru.madbrains.domain.interfaces.CCTVRepository
import ru.madbrains.domain.interfaces.GeoRepository
import ru.madbrains.domain.interfaces.InboxRepository
import ru.madbrains.domain.interfaces.IssueRepository
import ru.madbrains.domain.interfaces.PayRepository
import ru.madbrains.domain.interfaces.FRSRepository
import ru.madbrains.domain.interfaces.ExtRepository
import ru.madbrains.domain.model.response.ProviderConfig

import timber.log.Timber
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object DataModule {
    var BASE_URL = "https://localhost/"
    var providerConfig = ProviderConfig()

    val sberApiUserName = ""
    val sberApiPassword = ""
    val orderNumberToId = hashMapOf<String, String>()
    fun extractOrderId(orderNumber: String): String {
        var r = ""
        synchronized(orderNumberToId) {
            if (orderNumberToId.containsKey(orderNumber)) {
                r = orderNumberToId[orderNumber] ?: ""
                orderNumberToId.remove(orderNumber)
            }
        }
        return r
    }
    fun getOrderId(orderNumber: String): String {
        var r = ""
        synchronized(orderNumberToId) {
            if (orderNumberToId.containsKey(orderNumber)) {
                r = orderNumberToId[orderNumber] ?: ""
            }
        }
        return r
    }

    fun create() = module {
        single {
            createApi(
                createHttpClient(get()),
                get()
            )
        }

        single { createMoshi() }

        single {
            Room.databaseBuilder(get(), ItemsDatabase::class.java, ItemsDatabase.DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }

        single { get<ItemsDatabase>().itemDao() }

        factory { DatabaseRepositoryImpl(get()) as DatabaseRepository }

        factory { AuthRepositoryImpl(get(), get()) as AuthRepository }

        factory { AddressRepositoryImpl(get(), get()) as AddressRepository }

        factory { GeoRepositoryImpl(get(), get()) as GeoRepository }

        factory { IssueRepositoryImpl(get(), get()) as IssueRepository }

        factory { InboxRepositoryImpl(get(), get()) as InboxRepository }

        factory { CCTVRepositoryImpl(get(), get()) as CCTVRepository }

        factory { PayRepositroyImpl(get(), get()) as PayRepository }

        factory { SipRepositoryImpl(get(), get()) as SipRepository }

        factory { FRSRepositoryImpl(get(), get()) as FRSRepository }

        factory { ExtRepositoryImpl(get(), get()) as ExtRepository }

        single { createPreferenceStorage(androidContext()) }
    }

    private fun createApi(client: OkHttpClient, moshi: Moshi): TeledomApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TeledomApi::class.java)
    }

    private fun createHttpClient(preferenceStorage: PreferenceStorage): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory
        val builder = OkHttpClient.Builder()
        with(builder) {
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
            addInterceptor(CommonInterceptor())
            addInterceptor(SessionInterceptor(preferenceStorage))
            addNetworkInterceptor(loggingInterceptor())
            sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager).hostnameVerifier{_, _ -> true}
        }
        return builder.build()
    }

    private fun loggingInterceptor(): Interceptor {
        val logger =
            HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) }
        return HttpLoggingInterceptor(logger).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun createMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private fun createPreferenceStorage(context: Context): PreferenceStorage =
        SharedPreferenceStorage(context)
}
