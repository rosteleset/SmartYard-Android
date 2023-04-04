package com.sesameware.smartyard_oem.ui.main.address.models.interfaces

import android.os.Parcelable
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import kotlinx.parcelize.Parcelize

@Parcelize
class EventLogModel : ObjectItem(), Parcelable {
    var counter = 0
    var houseId = 0
    var address = ""
    var flats: List<Flat> = listOf()
}
