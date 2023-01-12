package com.sesameware.data

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
import com.sesameware.data.interceptors.CommonInterceptor
import com.sesameware.data.interceptors.SessionInterceptor
import com.sesameware.data.local.db.ItemsDatabase
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.data.prefs.SharedPreferenceStorage
import com.sesameware.data.remote.TeledomApi
import com.sesameware.data.repository.AddressRepositoryImpl
import com.sesameware.data.repository.DatabaseRepositoryImpl
import com.sesameware.data.repository.SipRepositoryImpl
import com.sesameware.data.repository.AuthRepositoryImpl
import com.sesameware.data.repository.GeoRepositoryImpl
import com.sesameware.data.repository.IssueRepositoryImpl
import com.sesameware.data.repository.CCTVRepositoryImpl
import com.sesameware.data.repository.InboxRepositoryImpl
import com.sesameware.data.repository.PayRepositroyImpl
import com.sesameware.data.repository.FRSRepositoryImpl
import com.sesameware.data.repository.ExtRepositoryImpl
import com.sesameware.domain.interfaces.AddressRepository
import com.sesameware.domain.interfaces.AuthRepository
import com.sesameware.domain.interfaces.SipRepository
import com.sesameware.domain.interfaces.DatabaseRepository
import com.sesameware.domain.interfaces.CCTVRepository
import com.sesameware.domain.interfaces.GeoRepository
import com.sesameware.domain.interfaces.InboxRepository
import com.sesameware.domain.interfaces.IssueRepository
import com.sesameware.domain.interfaces.PayRepository
import com.sesameware.domain.interfaces.FRSRepository
import com.sesameware.domain.interfaces.ExtRepository
import com.sesameware.domain.model.response.ProviderConfig

import timber.log.Timber
import java.util.concurrent.TimeUnit

object DataModule {
    var BASE_URL = "https://localhost/"
    var providerConfig = ProviderConfig()
    var providerName = ""
    var defaultPhonePattern = "+7 (###) ###-##-##"
    var phonePattern = defaultPhonePattern

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
        val builder = OkHttpClient.Builder()
        with(builder) {
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
            addInterceptor(CommonInterceptor())
            addInterceptor(SessionInterceptor(preferenceStorage))
            addNetworkInterceptor(loggingInterceptor())
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
