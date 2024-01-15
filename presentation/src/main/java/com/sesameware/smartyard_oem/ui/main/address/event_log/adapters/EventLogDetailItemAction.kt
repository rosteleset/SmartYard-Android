package com.sesameware.smartyard_oem.ui.main.address.event_log.adapters

import com.sesameware.domain.model.response.Plog

sealed interface EventLogDetailItemAction {
    data class OnMuteClick(val isMuted: Boolean) : EventLogDetailItemAction
    object OnHelpClick : EventLogDetailItemAction
    data class OnAddRemoveRegistrationClick(
        val position: Int,
        val plog: Plog
    ) : EventLogDetailItemAction
    object OnPlayOrPause : EventLogDetailItemAction
    data class OnShowOrHidePlayerView(val show: Boolean) : EventLogDetailItemAction
    data class OnRewind(val forward: Boolean) : EventLogDetailItemAction
}