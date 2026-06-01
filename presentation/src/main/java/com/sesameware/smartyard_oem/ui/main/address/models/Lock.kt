package com.sesameware.smartyard_oem.ui.main.address.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Lock(
    val domophoneId: Int,
    val doorId: Int? = null
) : Parcelable
