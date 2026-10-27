package com.sesameware.smartyard_oem.ui.reg

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.withResumed
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.domain.model.CommonErrorThrowable
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.MessagingService.TypeMessage
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.providers.ProvidersFragmentDirections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class RegistrationViewModel(
    override val mPreferenceStorage: PreferenceStorage,
    private val inboxInteractor: InboxInteractor,
    override val mAuthInteractor: AuthInteractor
) : GenericViewModel() {
    /** Shared because registration startup and config loading mutate the same provider state. */
    @Volatile
    var providerJob: Job? = null
        private set
    private val providerJobLock = Any()

    val authToken: String?
        get() = mPreferenceStorage.authToken

    fun cancelProviderRequests() {
        providerJob?.cancel()
    }

    fun onStart(
        fragment: Fragment,
        messageId: String,
        messageType: TypeMessage,
        activity: Activity
    ) {
        startProviderJob(activity, fragment) {
            if (mPreferenceStorage.authToken.isNullOrBlank()) {
                if (DataModule.BASE_URL.isNotEmpty()) {
                    mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
                    Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
                    try {
                        mAuthInteractor.phonePattern()?.let { result ->
                            currentCoroutineContext().ensureActive()
                            DataModule.phonePattern = result.data
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: CommonErrorThrowable) {
                        Timber.d("debug_dmm    phonePattern error: ${e.message}")
                    }
                    currentCoroutineContext().ensureActive()
                    navigateToNumberEntry(fragment, activity)
                }
                return@startProviderJob
            }

            try {
                loadProviderConfig()
                currentCoroutineContext().ensureActive()
                mAuthInteractor.phonePattern()?.let { result ->
                    currentCoroutineContext().ensureActive()
                    DataModule.phonePattern = result.data
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CommonErrorThrowable) {
                Timber.d("debug_dmm    getProviderConfig error: ${e.message}")
                if (e.data.httpCode == 401) {
                    currentCoroutineContext().ensureActive()
                    logout(activity)
                    return@startProviderJob
                }
            }

            currentCoroutineContext().ensureActive()
            if (mPreferenceStorage.userName?.firstName.isNullOrEmpty()) {
                val action =
                    ProvidersFragmentDirections.actionProvidersFragmentToAppealFragment()
                navigateIfCurrentProviderDestination(fragment, activity) {
                    fragment.findNavController().navigate(action)
                }
            } else {
                val action =
                    ProvidersFragmentDirections.actionProvidersFragmentToMainActivity(messageId)
                action.messageType = messageType
                val navigated = navigateIfCurrentProviderDestination(fragment, activity) {
                    fragment.findNavController().navigate(action)
                }
                if (navigated && isUiAlive(fragment, activity)) {
                    activity.finish()
                }
            }
        }
    }

    /** Keeps suspend semantics for callers that must await config before continuing. */
    suspend fun getProviderConfig() = coroutineScope {
        val callerScope = this
        withContext(Dispatchers.Main.immediate) {
            val job = callerScope.async(Dispatchers.Main.immediate, start = CoroutineStart.LAZY) {
                loadProviderConfig()
            }
            replaceProviderJob(job)
            job.start()
            // Unlike join(), await() propagates cancellation of this provider job
            // so the caller cannot continue with phone-pattern/config updates.
            job.await()
            currentCoroutineContext().ensureActive()
        }
    }

    private suspend fun loadProviderConfig() {
        currentCoroutineContext().ensureActive()
        Timber.d("debug_dmm call getProviderConfig")
        if (DataModule.BASE_URL.isNotEmpty()) {
            mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
            Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
            mAuthInteractor.getOptions()?.let { result ->
                currentCoroutineContext().ensureActive()
                DataModule.providerConfig = result.data
            }
            return
        }

        val pId = mPreferenceStorage.providerId
        Timber.d("debug_dmm providerId = $pId")
        if (pId?.isNotEmpty() == true) {
            mAuthInteractor.providers()?.data?.forEach {
                currentCoroutineContext().ensureActive()
                if (it.id == pId) {
                    DataModule.BASE_URL = it.baseUrl + if (!it.baseUrl.endsWith("/")) "/" else ""
                    mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
                    Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
                    DataModule.providerName = it.name
                    mAuthInteractor.getOptions()?.let { result ->
                        currentCoroutineContext().ensureActive()
                        DataModule.providerConfig = result.data
                    }
                    return
                }
            }
        }
    }

    private fun startProviderJob(
        activity: Activity,
        fragment: Fragment,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            block()
        }
        synchronized(providerJobLock) {
            replaceProviderJob(job)
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) job.cancel()
        }
        val lifecycle = fragment.lifecycle
        lifecycle.addObserver(lifecycleObserver)
        job.invokeOnCompletion {
            lifecycle.removeObserver(lifecycleObserver)
        }
        if (!isUiAlive(fragment, activity)) {
            job.cancel()
        }
        job.start()
    }

    private fun replaceProviderJob(job: Job) {
        val previousJob = synchronized(providerJobLock) {
            providerJob.also { providerJob = job }
        }
        job.invokeOnCompletion {
            synchronized(providerJobLock) {
                if (providerJob === job) providerJob = null
            }
        }
        previousJob?.cancel()
    }

    private suspend fun navigateToNumberEntry(fragment: Fragment, activity: Activity) {
        val action = ProvidersFragmentDirections.actionGlobalNumberRegFragment()
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.providersFragment, true)
            .build()
        navigateIfCurrentProviderDestination(fragment, activity) {
            NavHostFragment.findNavController(fragment).navigate(action, options)
        }
    }

    private suspend fun navigateIfCurrentProviderDestination(
        fragment: Fragment,
        activity: Activity,
        navigate: () -> Unit
    ): Boolean {
        val context = currentCoroutineContext()
        // Do not lose completed startup while stopped, or navigate after saved state.
        // A disconnect cancels this wait along with the provider request job.
        return fragment.lifecycle.withResumed {
            context.ensureActive()
            if (!isUiAlive(fragment, activity)) return@withResumed false
            val navController = NavHostFragment.findNavController(fragment)
            if (navController.currentDestination?.id != R.id.providersFragment) return@withResumed false
            navigate()
            true
        }
    }

    fun isUiAlive(fragment: Fragment, activity: Activity): Boolean =
        (!activity.isFinishing && !activity.isDestroyed && fragment.isAdded &&
            fragment.activity === activity && !fragment.parentFragmentManager.isStateSaved)
}
