package com.sesameware.smartyard_oem.ui.launcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Artem Budarin
 * Created on 12/05/2020.
 */
class LauncherViewModel(
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    private val _launchDestination = MutableLiveData<Event<LaunchDestination>>()
    val launchDestination: LiveData<Event<LaunchDestination>> = _launchDestination

    init {
        if (mPreferenceStorage.appVersion != BuildConfig.VERSION_CODE || !mPreferenceStorage.onboardingCompleted) {
            mPreferenceStorage.appVersion = BuildConfig.VERSION_CODE
            mPreferenceStorage.onboardingCompleted = false
            _launchDestination.value = Event(LaunchDestination.ONBOARDING_ACTIVITY)
        } else {
            _launchDestination.value = Event(LaunchDestination.REGISTRATION_ACTIVITY)
        }
    }

    enum class LaunchDestination {
        ONBOARDING_ACTIVITY,
        REGISTRATION_ACTIVITY
    }
}
