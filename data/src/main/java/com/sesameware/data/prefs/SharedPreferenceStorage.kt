package com.sesameware.data.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import com.google.gson.Gson
import com.sesameware.data.R
import com.sesameware.domain.model.response.UserName
import kotlinx.parcelize.Parcelize
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


interface PreferenceStorage {
    var authToken: String?
    var pushToken: String?
    var pushTokenRegistered: String?
    var userName: UserName?
    var phone: String?
    var countryPhoneCode: String?
    var notificationData: NotificationData
    var notifySoundUri: String?
    var addressOptions: AddressOptions
    var whereIsContractWarningSeen: Boolean
    var onboardingCompleted: Boolean
    var xDmApiRefresh: Boolean
    var providerId: String?
    var providerBaseUrl: String?
    var showCamerasOnMap: Boolean
    var houseIdPositions: Map<Int, Int>?
    var expandedHouseIds: Set<Int>?
    var justRegistered: Boolean
    var nightModeHasChanged: Boolean
    var nightMode: NightMode
    var askedAboutLockedScreen: Boolean

    fun clear()
}

/**
 * [PreferenceStorage] impl backed by [android.content.SharedPreferences].
 */
class SharedPreferenceStorage constructor(
    context: Context
) : PreferenceStorage {

    private var apiRefresh = false

    companion object {
        const val PREFS_NAME = "smart_yard"
        const val PREF_ONBOARDING = "pref_onboarding"
        const val PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN"
        const val PREF_PUSH_TOKEN = "PREF_PUSH_TOKEN"
        const val PREF_PUSH_TOKEN_REGISTERED = "PREF_PUSH_TOKEN_REGISTERED"
        const val PREF_SENT_NAME = "PREF_SENT_NAME"
        const val PREF_PHONE = "PREF_PHONE"
        const val PREF_COUNTRY_PHONE_CODE = "PREF_COUNTRY_PHONE_CODE"
        const val PREF_NOTIFICATION_DATA = "PREF_NOTIFICATION_DATA"
        const val PREF_NOTIFY_SOUND_URI = "PREF_NOTIFY_SOUND_URI"
        const val PREF_ADDRESS_OPTIONS = "PREF_ADDRESS_OPTIONS_2"
        const val PREF_WHERE_IS_CONTRACT_WARNING_SEEN = "PREF_WHERE_IS_CONTRACT_WARNING_SEEN"
        const val PREF_PROVIDER_ID = "PREF_PROVIDER_ID"
        const val PREF_PROVIDER_BASE_URL = "PREF_PROVIDER_BASE_URL"
        const val PREF_SHOW_CAMERAS_ON_MAP = "PREF_SHOW_CAMERAS_ON_MAP"
        const val PREFS_HOUSE_ID_POSITIONS = "PREFS_HOUSE_ID_POSITIONS"
        const val PREFS_EXPANDED_HOUSE_IDS = "PREFS_EXPANDED_HOUSE_IDS"
        const val PREFS_JUST_REGISTERED = "PREFS_ADDRESS_LIST_FIRST_LAUNCH"
        const val PREFS_NIGHT_MODE_HAS_CHANGED = "PREFS_NIGHT_MODE_HAS_CHANGED"
        const val PREFS_NIGHT_MODE = "PREFS_THEME"
        const val PREFS_ASKED_ABOUT_LOCKED_SCREEN = "PREFS_ASKED_ABOUT_LOCKED_SCREEN"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    override var onboardingCompleted by BooleanPreference(prefs, PREF_ONBOARDING, false)
    override var authToken by StringPreference(prefs, PREF_AUTH_TOKEN, null)
    override var pushToken by StringPreference(prefs, PREF_PUSH_TOKEN, null)
    override var pushTokenRegistered by StringPreference(prefs, PREF_PUSH_TOKEN_REGISTERED, null)
    override var userName by SerializablePreferenceNullable(
        prefs,
        PREF_SENT_NAME,
        null,
        UserName::class.java
    )
    override var phone by StringPreference(prefs, PREF_PHONE, null)
    override var countryPhoneCode by StringPreference(prefs, PREF_COUNTRY_PHONE_CODE, null)
    override var notificationData by SerializablePreference(
        prefs,
        PREF_NOTIFICATION_DATA,
        NotificationData(),
        NotificationData::class.java
    )
    override var notifySoundUri by StringPreference(prefs, PREF_NOTIFY_SOUND_URI, null)
    override var addressOptions by SerializablePreference(
        prefs,
        PREF_ADDRESS_OPTIONS,
        AddressOptions(),
        AddressOptions::class.java
    )
    override var whereIsContractWarningSeen by BooleanPreference(prefs, PREF_WHERE_IS_CONTRACT_WARNING_SEEN, false)
    override var xDmApiRefresh: Boolean
        get() = apiRefresh
        set(value) { apiRefresh = value }
    override var providerId by StringPreference(prefs, PREF_PROVIDER_ID, null)
    override var providerBaseUrl by StringPreference(prefs, PREF_PROVIDER_BASE_URL, null)
    override var showCamerasOnMap by BooleanPreference(prefs, PREF_SHOW_CAMERAS_ON_MAP, true)
    override var houseIdPositions: Map<Int, Int>? by MapPreference(
        prefs,
        PREFS_HOUSE_ID_POSITIONS,
        getHouseIdPositionsConverter()
    )
    override var expandedHouseIds: Set<Int>? by SetPreference(
        prefs,
        PREFS_EXPANDED_HOUSE_IDS,
        getExpandedHouseIdsConverter()
    )
    override var justRegistered by BooleanPreference(prefs,
        PREFS_JUST_REGISTERED, true)

    override var nightModeHasChanged by BooleanPreference(prefs,
        PREFS_NIGHT_MODE_HAS_CHANGED, false)

    private val featureNightModeIsEnabled = runCatching {
        context.resources.getBoolean(R.bool.feature_night_mode_is_enabled)
    }.getOrDefault(false)

    override var nightMode by SerializablePreference(
        prefs, PREFS_NIGHT_MODE,
        if (featureNightModeIsEnabled) NightMode.FOLLOW_SYSTEM else NightMode.NO,
        NightMode::class.java)

    override var askedAboutLockedScreen by BooleanPreference(prefs,
        PREFS_ASKED_ABOUT_LOCKED_SCREEN, false)

    override fun clear() {
        providerId = null
        providerBaseUrl = null
        authToken = null
        userName = null
        phone = null
        countryPhoneCode = null
        pushTokenRegistered = null
        houseIdPositions = null
        expandedHouseIds = null
        justRegistered = true
        nightModeHasChanged = false
        nightMode = if (featureNightModeIsEnabled) NightMode.FOLLOW_SYSTEM else NightMode.NO
        askedAboutLockedScreen = false
    }
}

class SerializablePreferenceNullable<T>(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: T?,
    private val tClass: Class<T>
) : ReadWriteProperty<Any, T?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val gson = Gson()
        val string = preferences.getString(name, gson.toJson(defaultValue))
        return gson.fromJson(string, tClass)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        val gson = Gson()
        preferences.edit { putString(name, gson.toJson(value)) }
    }
}

class SerializablePreference<T>(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: T,
    private val tClass: Class<T>
) : ReadWriteProperty<Any, T> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val gson = Gson()
        val string = preferences.getString(name, gson.toJson(defaultValue))
        return gson.fromJson(string, tClass)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val gson = Gson()
        preferences.edit { putString(name, gson.toJson(value)) }
    }
}

class BooleanPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.edit { putBoolean(name, value) }
    }
}

class StringPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: String?
) : ReadWriteProperty<Any, String?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return preferences.getString(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.edit { putString(name, value) }
    }
}

class IntPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Int
) : ReadWriteProperty<Any, Int> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return preferences.getInt(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.edit { putInt(name, value) }
    }
}

class MapPreference<K, V>(
    private val preferences: SharedPreferences,
    private val name: String,
    private val converter: MapConverter<K, V>
) : ReadWriteProperty<Any, Map<K, V>?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Map<K, V>? {
        val stringSet = preferences.getStringSet(name, null) ?: return null
        val map = mutableMapOf<K, V>()
        stringSet.forEach { line ->
            val (kSt, vSt) = line.split(DELIMITER)
            map[converter.keyFromString(kSt)] = converter.valueFromString(vSt)
        }
        return map
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Map<K, V>?) {
        val stringSet = value?.map { e ->
            "${e.key.toString()}$DELIMITER${e.value.toString()}" }?.toSet()
        preferences.edit { putStringSet(name, stringSet) }
    }

    companion object {
        private const val DELIMITER = "_"
    }
}

class SetPreference<T>(
    private val preferences: SharedPreferences,
    private val name: String,
    private val converter: SetConverter<T>
) : ReadWriteProperty<Any, Set<T>?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Set<T>? {
        val stringSet = preferences.getStringSet(name, null) ?: return null
        return stringSet.map { converter.fromString(it)}.toSet()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Set<T>?) {
        val stringSet = value?.map { it.toString() }?.toSet()
        preferences.edit { putStringSet(name, stringSet) }
    }
}

interface MapConverter<K, V> {
    fun keyFromString(s: String): K
    fun valueFromString(s: String): V
}

interface SetConverter<T> {
    fun fromString(s: String): T
}

private fun getHouseIdPositionsConverter() : MapConverter<Int, Int> =
    object : MapConverter<Int, Int> {
        override fun keyFromString(s: String): Int = s.toInt()
        override fun valueFromString(s: String): Int = s.toInt()
    }

private fun getExpandedHouseIdsConverter(): SetConverter<Int> =
    object : SetConverter<Int> {
        override fun fromString(s: String): Int = s.toInt()
    }

@Parcelize
enum class NightMode(val value: Int) : Parcelable {
    FOLLOW_SYSTEM(-1),
    NO(1),
    YES(2),
}