package ru.madbrains.domain.model.response

import com.squareup.moshi.Json
import timber.log.Timber


typealias ContractsResponse = ApiResult<List<ContractsResponseItem>>?

data class ContractsResponseItem(
    @Json(name = "address")
    val address: String,

    @Json(name = "balance")
    val balance: Int,

    @Json(name = "blocked")
    val blocked: String,

    @Json(name = "bonus")
    val bonus: Int,

    @Json(name = "cards")
    val cards: List<Any>,

    @Json(name = "cityId")
    val cityId: Int,

    @Json(name = "cityTitle")
    val cityTitle: String,

    @Json(name = "limitAvailable")
    val limitAvailable: Boolean,

    @Json(name = "limitDays")
    val limitDays: Int,

    @Json(name = "clientId")
    val clientId: Int,

    @Json(name = "contractName")
    val contractName: String,

    @Json(name = "contractOwner")
    private val _contractOwner: String,

    @Json(name = "hasGates")
    private val _hasGates: String,

    @Json(name = "hasPlog")
    private val _hasPlog: String,

    @Json(name = "houseId")
    val houseId: String,

    @Json(name = "limitStatus")
    val limitStatus: Boolean,

    @Json(name = "merchant")
    val merchant: String,

    @Json(name = "payAdvice")
    val payAdvice: Int,

    @Json(name = "services")
    val services: List<String>,

    @Json(name = "parentControlStatus")
    private val _parentControlStatus: String?,

    @Json(name = "parentControlEnable")
    private val _parentControlEnable: String,
) {
    val parentControlStatus = getParentStatus()
    val parentControlEnable = _parentControlEnable == "t"
    val hasGates = _hasGates == "t"
    val hasPlog = _hasPlog == "t"
    val contractOwner = _contractOwner == "t"

    private fun getParentStatus(): Boolean? {
        return if (_parentControlStatus == null) {
            null
        } else {
            _parentControlStatus == "t"
        }
    }
}