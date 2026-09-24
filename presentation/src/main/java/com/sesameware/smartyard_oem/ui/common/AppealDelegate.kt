package com.sesameware.smartyard_oem.ui.common

import com.sesameware.smartyard_oem.databinding.FormAppealBinding

interface AppealDelegate {
    fun skipAppeal(): Boolean = false
    fun extendConfig(binding: FormAppealBinding)
}
