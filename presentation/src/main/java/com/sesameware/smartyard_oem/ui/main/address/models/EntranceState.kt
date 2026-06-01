package com.sesameware.smartyard_oem.ui.main.address.models

import androidx.annotation.DrawableRes
import com.sesameware.domain.model.response.EntranceCamera

data class EntranceState(
    val lock: Lock,
    val entranceId: Int?,
    @DrawableRes val iconRes: Int,
    val name: String,
    val cameras: List<EntranceCamera>,
)
