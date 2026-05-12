package com.sesameware.smartyard_oem.ui.main.address.models

import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP

sealed interface HouseAction

data class OnOpenEntranceClick(val entranceId: EntranceId) : HouseAction
data class OnExpandClick(val position: Int, val isExpanded: Boolean) : HouseAction
data class OnHouseAddressLongClick(val position: Int) : HouseAction
data class OnItemFullyExpanded(val position: Int) : HouseAction
data class OnCameraClick(val model: VideoCameraModelP) : HouseAction
data class OnEventLogClick(val title: String, val houseId: Int) : HouseAction
data class OnWebExtensionClick(val title: String?, val basePath: String?, val code: String?) : HouseAction