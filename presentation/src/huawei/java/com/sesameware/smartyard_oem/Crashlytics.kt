package com.sesameware.smartyard_oem

import com.huawei.agconnect.crash.AGConnectCrash

object Crashlytics {
    fun getInstance(): AGConnectCrash {
        return AGConnectCrash.getInstance()
    }
}
