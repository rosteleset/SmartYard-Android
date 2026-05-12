package com.sesameware.smartyard_oem.ui.main.settings.basicSettings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.NightMode
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.response.UserName
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.DatabaseInteractor
import com.sesameware.domain.model.TF
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.SoundChooser
import java.util.regex.Pattern

/**
 * @author Nail Shakurov
 * Created on 16/03/2020.
 */
class BasicSettingsViewModel(
    override val mPreferenceStorage: PreferenceStorage,
    override val mDatabaseInteractor: DatabaseInteractor,
    override val mAuthInteractor: AuthInteractor
) : GenericViewModel() {

    val userName = MutableLiveData<UserName>()
    val userPhone = MutableLiveData<String>()

    val isPushSetting = MutableLiveData<Boolean>()
    val isPushMoneySetting = MutableLiveData<Boolean>()

    init {
        refreshUserData()
        viewModelScope.withProgress {
            val res = mAuthInteractor.userNotification(
                null, null
            )
            isPushSetting.value = TF.getString(res.data.enable)
            isPushMoneySetting.value = TF.getString(res.data.money)
        }
    }

    fun setPushMoneySetting(flag: Boolean) {
        viewModelScope.withProgress {
            val res = mAuthInteractor.userNotification(
                TF.getBoolean(flag),
                null
            )
            isPushMoneySetting.value = TF.getString(res.data.money)
        }
    }

    fun setPushSetting(flag: Boolean) {
        viewModelScope.withProgress {
            mPreferenceStorage.xDmApiRefresh = true
            val res = mAuthInteractor.userNotification(
                null,
                TF.getBoolean(flag)
            )
            isPushSetting.value = TF.getString(res.data.enable)
        }
    }

    fun refreshUserData() {
        userName.postValue(mPreferenceStorage.userName ?: UserName("", ""))
        val phoneCode = mPreferenceStorage.countryPhoneCode
        val phone = mPreferenceStorage.phone
        val formatted = if (phone != null) {
            if (phoneCode != null) {
                formatPhoneNumber(phoneCode, phone)
            } else {
                "+$phone"
            }
        } else {
            ""
        }
        userPhone.postValue(formatted)
    }

    private fun formatPhoneNumber(countryCodeIso: String, rawPhoneNumber: String): String {
        val cci = countryCodeIso
        if (cci.isEmpty() || cci.length > 3 || !cci.isDigitsOnly())
            throw IllegalArgumentException("countryCodeIso must contain from 1 to 3 digits only")

        val pn = Pattern.compile("\\D+").matcher(rawPhoneNumber).replaceAll("")
        if (pn.length < 10 || pn.length > 15 || !pn.isDigitsOnly())
            throw IllegalArgumentException("rawPhoneNumber must contain from 10 to 15 digits")

        val ccl = countryCodeIso.length
        val pnl = rawPhoneNumber.length
        val areaCode = rawPhoneNumber.substring(ccl until pnl - 7)
        val subscriberNumber0 = rawPhoneNumber.substring(pnl - 7 until pnl - 4)
        val subscriberNumber1 = rawPhoneNumber.substring(pnl - 4 until pnl - 2)
        val subscriberNumber2 = rawPhoneNumber.substring(pnl - 2 until pnl)

        val formatted = StringBuilder()
        formatted.append("+$countryCodeIso ", "($areaCode) ", "$subscriberNumber0-",
            "$subscriberNumber1-", subscriberNumber2)
        return formatted.toString()
    }

    fun saveSoundToPref(tone: SoundChooser.RingtoneU) {
        mPreferenceStorage.notifySoundUri = tone.uri.toString()
    }

    fun saveShowOnMapPref(value: Boolean) {
        mPreferenceStorage.showCamerasOnMap = value
    }

    fun setNightMode(mode: NightMode) {
        with (mPreferenceStorage) {
            if (mode == nightMode) return
            nightModeHasChanged = true
            nightMode = mode
            AppCompatDelegate.setDefaultNightMode(mode.value)
        }
    }
}
