package com.sesameware.smartyard_oem.ui.main.address.models.interfaces

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Nail Shakurov
 * Created on 2020-02-11.
 */
class VideoCameraModel : ObjectItem() {
    var counter = 0
    var houseId = 0
    var address = ""

    fun toParcelable(): VideoCameraModelP {
        return VideoCameraModelP(houseId, address)
    }
}
@Parcelize
class VideoCameraModelP(
    val houseId: Int,
    val address: String
) : Parcelable
