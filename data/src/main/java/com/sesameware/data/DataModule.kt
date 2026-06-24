package com.sesameware.data

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.sesameware.data.DataModule.BASE_URL
import com.sesameware.data.interceptors.CommonInterceptor
import com.sesameware.data.interceptors.SessionInterceptor
import com.sesameware.data.local.db.ItemsDatabase
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.data.prefs.SharedPreferenceStorage
import com.sesameware.data.remote.TeledomApi
import com.sesameware.data.repository.AddressRepositoryImpl
import com.sesameware.data.repository.AuthRepositoryImpl
import com.sesameware.data.repository.CCTVRepositoryImpl
import com.sesameware.data.repository.DatabaseRepositoryImpl
import com.sesameware.data.repository.ExtRepositoryImpl
import com.sesameware.data.repository.FRSRepositoryImpl
import com.sesameware.data.repository.GeoRepositoryImpl
import com.sesameware.data.repository.InboxRepositoryImpl
import com.sesameware.data.repository.IssueRepositoryImpl
import com.sesameware.data.repository.LPRSRepositoryImpl
import com.sesameware.data.repository.PayRepositroyImpl
import com.sesameware.data.repository.SipRepositoryImpl
import com.sesameware.data.repository.WebRtcStreamingRepositoryImpl
import com.sesameware.domain.interfaces.AddressRepository
import com.sesameware.domain.interfaces.AuthRepository
import com.sesameware.domain.interfaces.CCTVRepository
import com.sesameware.domain.interfaces.DatabaseRepository
import com.sesameware.domain.interfaces.ExtRepository
import com.sesameware.domain.interfaces.FRSRepository
import com.sesameware.domain.interfaces.GeoRepository
import com.sesameware.domain.interfaces.InboxRepository
import com.sesameware.domain.interfaces.IssueRepository
import com.sesameware.domain.interfaces.LPRSRepository
import com.sesameware.domain.interfaces.PayRepository
import com.sesameware.domain.interfaces.SipRepository
import com.sesameware.domain.interfaces.WebRtcStreamingRepository
import com.sesameware.domain.model.response.ProviderConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoEncoderFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

object DataModule {
    var BASE_URL = BuildConfig.PROVIDER_URL
    var providerConfig = ProviderConfig()
    var providerName = BuildConfig.PROVIDER_NAME
    var defaultPhonePattern = BuildConfig.DEFAULT_PHONE_PATTERN
    var phonePattern = defaultPhonePattern
    var defaultLicensePlatePattern = BuildConfig.DEFAULT_LICENSE_PLATE_PATTERN
    var licensePlatePattern = defaultLicensePlatePattern
    var xDmApiRefresh = false
    val serverTz: String
        get() = providerConfig.timeZone.orEmpty().ifEmpty { BuildConfig.SERVER_TZ }
//    val mapBoxToken = BuildConfig.MAP_BOX_TOKEN

    fun create() = module {
        single { createHttpClient(get()) }

        single(named("clean")) { createCleanHttpClient() }

        single { createApi(get(), get()) }

        single { createMoshi() }

        single {
            Room.databaseBuilder(get(), ItemsDatabase::class.java, ItemsDatabase.DATABASE_NAME)
                .fallbackToDestructiveMigration(false)
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

        factory { LPRSRepositoryImpl(get(), get()) as LPRSRepository }

        factory { ExtRepositoryImpl(get(), get()) as ExtRepository }

        single { createPreferenceStorage(androidContext()) }

        single<EglBase> { EglBase.create() }

        single<VideoDecoderFactory> {
            val eglBase: EglBase = get()
            val isBuggyDevice = shouldForceSoftwareDecoder()
            SafeVideoDecoderFactory(eglBase.eglBaseContext, disableHighProfile = isBuggyDevice)
        }

        single<VideoEncoderFactory> {
            val eglBase: EglBase = get()
            DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        }

        single<PeerConnectionFactory> {
            val context = androidContext()

            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            PeerConnectionFactory.builder()
                .setVideoDecoderFactory(get<VideoDecoderFactory>())
                .setVideoEncoderFactory(get<VideoEncoderFactory>())
                .createPeerConnectionFactory()
        }

        single { WebRtcStreamingRepositoryImpl(get(), get(named("clean"))) as WebRtcStreamingRepository }
    }

    private fun shouldForceSoftwareDecoder(): Boolean {
        val is32Bit = Build.SUPPORTED_64_BIT_ABIS.isEmpty()

        // Trouble devices
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val isBuggyDevice = manufacturer.contains("samsung", ignoreCase = true) &&
                (model.contains("A13", ignoreCase = true) ||
                        model.contains("A12", ignoreCase = true) ||
                        model.contains("A03", ignoreCase = true) ||
                        model.contains("A04", ignoreCase = true))

        return is32Bit || isBuggyDevice
    }

    private fun createApi(client: OkHttpClient, moshi: Moshi): TeledomApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL.ifEmpty { "http://localhost" })
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

    private fun createCleanHttpClient(): OkHttpClient =
        OkHttpClient.Builder().addNetworkInterceptor(loggingInterceptor()).build()

    private fun loggingInterceptor(): Interceptor {
        val logger =
            HttpLoggingInterceptor.Logger { message ->
                Timber.tag("OkHttp").d(message)
                val crashlytics = Crashlytics.getInstance()
                crashlytics.log("OkHttp $message")
            }
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
