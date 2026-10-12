package com.sesameware.domain.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

typealias GetServicesResponse = ApiResult<List<ServicesData>>?

@Parcelize
data class ServicesData(
    @param:Json(name = "icon") val icon: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String = "",
    @param:Json(name = "byDefault") val byDefault: String = "f",
    @param:Json(name = "canChange") val canChange: String = "t"
) : Parcelable
