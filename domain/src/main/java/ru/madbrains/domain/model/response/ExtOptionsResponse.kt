package ru.madbrains.domain.model.response

import com.google.gson.annotations.SerializedName


typealias ExtOptionsResponse = ApiResult<ItemOption?>?

data class ItemOption(
    @SerializedName("paymentsUrl") val paymentsUrl: String? = null,
    @SerializedName("payments") val payments: String? = null,
    @SerializedName("cityCams") val cityCams: String? = null,
    @SerializedName("supportPhone") val supportPhone: String? = null,
    @SerializedName("centraScreenUrl") val centraScreenUrl: String? = null,
    @SerializedName("intercomScreenUrl") val intercomScreenUrl: String? = null,
    @SerializedName("activeTab") val activeTab: String? = null
)
