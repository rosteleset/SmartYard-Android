package ru.madbrains.smartyard

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport
import com.google.firebase.installations.Utils
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.data.repository.BaseRepository.Companion.getStatus
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.DatabaseInteractor
import ru.madbrains.domain.model.CommonError
import ru.madbrains.domain.model.CommonErrorThrowable
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias SuccessHandler<T> = suspend (T) -> Unit
typealias ExceptionHandler = suspend (CommonError) -> Boolean

open class GenericViewModel : ViewModel(), KoinComponent {
    val globalData: GlobalDataSource by inject()
    private val mPreferenceStorage: PreferenceStorage by inject()
    private val mInteractor: AuthInteractor by inject()
    private val databaseInteractor: DatabaseInteractor by inject()

    val localErrorsSink = MutableLiveData<Event<CommonError>>()
    var logout = MutableLiveData<Event<Boolean>>()

    protected fun CoroutineScope.withProgress(
        handleError: ExceptionHandler = { true },
        progress: MutableLiveData<Boolean>? = globalData.progressVisibility,
        context: CoroutineContext = EmptyCoroutineContext,
        query: suspend CoroutineScope.() -> Unit
    ): Job {
        return this.launch(
            context + CoroutineExceptionHandler { _, exception ->
                progress?.postValue(false)
            }
        ) {
            progress?.postValue(true)
            try {
                query()
            } catch (e: Throwable) {
                if (e is CommonErrorThrowable) {
                    localErrorsSink.value = Event(e.data)
                    if (handleError(e.data)) {
                        globalData.globalErrorsSink.value = Event(e.data)
                    }
                }
            }
            progress?.postValue(false)
        }
    }

    fun showGlobalError(throwable: Throwable) {
        globalData.globalErrorsSink.value = Event(
            CommonError(throwable, getStatus(throwable))
        )
    }

    protected fun CoroutineScope.launchSimple(
        handleError: ExceptionHandler = { true },
        context: CoroutineContext = EmptyCoroutineContext,
        query: suspend CoroutineScope.() -> Unit
    ): Job {
        return this.withProgress(handleError, progress = null, query = query, context = context)
    }

    protected fun checkAndRegisterFcmToken() {

        Timber.d("debug_dmm call checkAndRegisterFcmToken()")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w("debug_dmm fetching fcm token failed")

                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val phone = mPreferenceStorage.phone
                val exceptionMessage = task.exception?.message
                val deviceInfo = "Manufacturer: " + Build.MANUFACTURER + ", model: " + Build.MODEL +
                    ", device: " + Build.DEVICE + ", release: " + Build.VERSION.RELEASE + ", SDK: " + Build.VERSION.SDK_INT
                Timber.w("debug_dmm exception message: $exceptionMessage")
                Timber.w("debug_dmm Device info: $deviceInfo")
                FirebaseCrashlytics.getInstance().log("Date: $date; Phone: $phone; Device info: $deviceInfo; Message: $exceptionMessage\n")
                task.exception?.let { exception->
                    FirebaseCrashlytics.getInstance().recordException(exception)
                }
                return@addOnCompleteListener
            }
            mPreferenceStorage.fcmToken = task.result

            //Timber.d("debug_dmm fcm token now: ${task.result?.token}")

            mPreferenceStorage.fcmToken?.let { token ->
                Timber.d("debug_dmm token: $token")
                Timber.d("debug_dmm saved registered token: ${mPreferenceStorage.fcmTokenRegistered}")
                if (token != mPreferenceStorage.fcmTokenRegistered) {
                    viewModelScope.launchSimple {
                        mInteractor.registerPushToken(token)
                        mPreferenceStorage.fcmTokenRegistered = token
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.withProgress {
            logout.postValue(Event(true))
            mPreferenceStorage.authToken = null
            mPreferenceStorage.sentName = null
            mPreferenceStorage.fcmTokenRegistered = null
            databaseInteractor.deleteAll()
            refreshFcmToken()
        }
    }

    private fun refreshFcmToken() {
        Thread {
            Timber.d("debug_dmm refreshing fcm token..")
            FirebaseMessaging.getInstance().deleteToken()
            mPreferenceStorage.fcmToken = ""
        }.start()
    }
}
