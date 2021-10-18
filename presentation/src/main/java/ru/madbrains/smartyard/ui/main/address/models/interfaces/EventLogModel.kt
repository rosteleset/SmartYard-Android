package ru.madbrains.smartyard.ui.main.address.models.interfaces

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.madbrains.smartyard.ui.main.address.event_log.Flat

@Parcelize
class EventLogModel : ObjectItem(), Parcelable {
    var counter = 0
    var houseId = 0
    var address = ""
    var flats: List<Flat> = listOf()
}
