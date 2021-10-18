package ru.madbrains.domain.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

typealias GetServicesResponse = ApiResult<List<ServicesData>>?

@Parcelize
data class ServicesData(
    @Json(name = "icon") val icon: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "byDefault") val byDefault: String,
    @Json(name = "canChange") val canChange: String
) : Parcelable
