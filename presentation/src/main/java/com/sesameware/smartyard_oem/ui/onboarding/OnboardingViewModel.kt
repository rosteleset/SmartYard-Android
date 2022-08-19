package com.sesameware.smartyard_oem.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Artem Budarin
 * Created on 08/05/2020.
 */
class OnboardingViewModel(
    private val preferenceStorage: PreferenceStorage
) : GenericViewModel() {

    private val _navigateToNextPage = MutableLiveData<Event<Unit>>()
    val navigateToNextPage: LiveData<Event<Unit>> = _navigateToNextPage

    private val _navigateToRegistration = MutableLiveData<Event<Unit>>()
    val navigateToRegistration: LiveData<Event<Unit>> = _navigateToRegistration

    fun onNextClick() {
        _navigateToNextPage.value = Event(Unit)
    }

    fun onCompleteClick() {
        preferenceStorage.onboardingCompleted = true
        _navigateToRegistration.value = Event(Unit)
    }

    fun onSkipClick() {
        preferenceStorage.onboardingCompleted = true
        _navigateToRegistration.value = Event(Unit)
    }
}
