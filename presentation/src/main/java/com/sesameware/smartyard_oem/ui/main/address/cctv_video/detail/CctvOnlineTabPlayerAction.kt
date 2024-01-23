package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

sealed interface CctvOnlineTabPlayerAction {
    data object OnFullScreenClick : CctvOnlineTabPlayerAction
    data object OnMuteClick : CctvOnlineTabPlayerAction
}