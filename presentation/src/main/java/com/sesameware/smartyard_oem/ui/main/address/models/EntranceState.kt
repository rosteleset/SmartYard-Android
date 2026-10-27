package com.sesameware.smartyard_oem.ui.main.address.models

import androidx.annotation.DrawableRes
import com.sesameware.smartyard_oem.ui.common.player.StreamSourceConfig

data class EntranceState(
    val lock: Lock,
    val entranceId: Int?,
    @DrawableRes val iconRes: Int,
    val name: String,
    val cameras: List<StreamSourceConfig>,
)
