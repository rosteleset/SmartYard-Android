package ru.madbrains.smartyard.ui.launcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.smartyard.BuildConfig
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel

/**
 * @author Artem Budarin
 * Created on 12/05/2020.
 */
class LauncherViewModel(
    private val preferenceStorage: PreferenceStorage
) : GenericViewModel() {

    private val _launchDestination = MutableLiveData<Event<LaunchDestination>>()
    val launchDestination: LiveData<Event<LaunchDestination>> = _launchDestination

    init {
        if (preferenceStorage.appVersion != BuildConfig.VERSION_CODE || !preferenceStorage.onboardingCompleted) {
            preferenceStorage.appVersion = BuildConfig.VERSION_CODE
            preferenceStorage.onboardingCompleted = false
            _launchDestination.value = Event(LaunchDestination.ONBOARDING_ACTIVITY)
        } else {
            _launchDestination.value = Event(LaunchDestination.REGISTRATION_ACTIVITY)
        }
//        checkAndRegisterFcmToken()
    }

    enum class LaunchDestination {
        ONBOARDING_ACTIVITY,
        REGISTRATION_ACTIVITY
    }
}
