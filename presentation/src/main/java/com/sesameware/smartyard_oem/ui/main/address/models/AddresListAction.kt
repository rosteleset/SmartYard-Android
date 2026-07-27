package com.sesameware.smartyard_oem.ui.main.address.models

import com.sesameware.domain.model.response.EntranceCamera
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP

sealed interface HouseAction

data class OnOpenEntranceClick(val lock: Lock) : HouseAction
data class OnEntrancePreviewClick(val camera: EntranceCamera, val lock: Lock) : HouseAction
data class OnExpandClick(val position: Int, val isExpanded: Boolean) : HouseAction
data class OnHouseAddressLongClick(val position: Int) : HouseAction
data class OnCameraClick(val model: VideoCameraModelP) : HouseAction
data class OnEventLogClick(val title: String, val houseId: Int) : HouseAction
data class OnWebExtensionClick(val title: String?, val basePath: String?, val code: String?, val isHeaderHidden: Boolean, val statusBarColor: String?, val statusBarStyle: String?) : HouseAction
data class OnEntrancePageSelected(val houseId: Int, val page: Int, val entranceCamera: EntranceCamera?) : HouseAction
