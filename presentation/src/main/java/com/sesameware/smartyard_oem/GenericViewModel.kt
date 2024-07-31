package com.sesameware.smartyard_oem

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sesameware.data.BuildConfig
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.data.repository.BaseRepository.Companion.getStatus
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.DatabaseInteractor
import com.sesameware.domain.model.CommonError
import com.sesameware.domain.model.CommonErrorThrowable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias ExceptionHandler = suspend (CommonError) -> Boolean

open class GenericViewModel : ViewModel(), KoinComponent {
    val globalData: GlobalDataSource by inject()
    open val mPreferenceStorage: PreferenceStorage by inject()
    open val mAuthInteractor: AuthInteractor by inject()
    open val mDatabaseInteractor: DatabaseInteractor by inject()

    val localErrorsSink = MutableLiveData<Event<CommonError>>()
    var logout = MutableLiveData<Event<Boolean>>()

    protected fun CoroutineScope.withProgress(
        handleError: ExceptionHandler = { true },
        progress: MutableLiveData<Boolean>? = globalData.progressVisibility,
        context: CoroutineContext = EmptyCoroutineContext,
        query: suspend CoroutineScope.() -> Unit
    ): Job {
        return this.launch(
            context + CoroutineExceptionHandler { _, _ ->
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

    fun CoroutineScope.launchSimple(
        handleError: ExceptionHandler = { true },
        context: CoroutineContext = EmptyCoroutineContext,
        query: suspend CoroutineScope.() -> Unit
    ): Job {
        return this.withProgress(handleError, progress = null, query = query, context = context)
    }

    fun logout(context: Context) {
        viewModelScope.withProgress {
            mPreferenceStorage.providerId = null
            mPreferenceStorage.providerBaseUrl = null
            mPreferenceStorage.authToken = null
            mPreferenceStorage.sentName = null
            mPreferenceStorage.pushTokenRegistered = null
            mDatabaseInteractor.deleteAll()
            DataModule.BASE_URL = BuildConfig.PROVIDER_URL
            refreshPushToken(context.applicationContext)
            logout.postValue(Event(true))
        }
    }
}
