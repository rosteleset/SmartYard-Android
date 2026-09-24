package com.sesameware.smartyard_oem.ui.launcher

import com.sesameware.data.prefs.PreferenceStorage

interface LauncherDelegate {
    fun extendConfig(prefs: PreferenceStorage)
}