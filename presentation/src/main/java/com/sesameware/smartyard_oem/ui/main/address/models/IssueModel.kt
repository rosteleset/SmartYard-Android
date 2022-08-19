package com.sesameware.smartyard_oem.ui.main.address.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem

/**
 * @author Nail Shakurov
 * Created on 24/04/2020.
 */
@Parcelize
data class IssueModel(
    var address: String = "",
    var key: String = "",
    var _courier: String = ""
) : DisplayableItem, Parcelable {
    val courier: Boolean
        get() = _courier == "t"
}
