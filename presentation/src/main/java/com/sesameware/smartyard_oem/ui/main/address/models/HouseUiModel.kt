package com.sesameware.smartyard_oem.ui.main.address.models

data class HouseUiModel(
    val houseId: Int,
    val address: String,
    val entranceList: List<EntranceState>,
    val cameraCount: Int,
    val hasEventLog: Boolean,
    val isExpanded: Boolean = false,
    val extList: List<ExtItemModel> = listOf(),
    val selectedEntranceIndex: Int = 0,
) : AddressUiModel
