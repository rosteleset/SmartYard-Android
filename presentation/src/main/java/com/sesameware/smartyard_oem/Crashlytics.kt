package com.sesameware.smartyard_oem

import com.google.firebase.crashlytics.FirebaseCrashlytics

object Crashlytics {
    fun getInstance(): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance()
    }
}
