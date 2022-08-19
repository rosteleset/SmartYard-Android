package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */

typealias IntercomResponse = ApiResult<Intercom>

data class Intercom(
    @Json(name = "allowDoorCode")
    val _allowDoorCode: String = "", // t
    @Json(name = "autoOpen")
    val autoOpen: String = "", // 2020-03-13 15:55:34
    @Json(name = "CMS")
    val _cMS: String = "", // t
    @Json(name = "doorCode")
    val doorCode: String? = "", // 99764
    @Json(name = "VoIP")
    val _voIP: String = "", // t
    @Json(name = "whiteRabbit")
    val whiteRabbit: Int = 0, // 0
    @Json(name = "paperBill")
    val _paperBill: String? = null,
    @Json(name = "disablePlog")
    val _disablePlog: String? = null,
    @Json(name = "hiddenPlog")
    val _hiddenPlog: String? = null,
    @Json(name = "FRSDisabled")
    val _frsDisabled: String? = null
) {
    val allowDoorCode: Boolean
        get() = _allowDoorCode == "t"
    val cMS: Boolean
        get() = _cMS == "t"
    val voIP: Boolean
        get() = _voIP == "t"
    val paperBill: Boolean?
        get() = if (_paperBill == null) null else _paperBill == "t"
    val disablePlog: Boolean?
        get() = if (_disablePlog == null) null else _disablePlog == "t"
    val hiddenPlog: Boolean?
        get() = if (_hiddenPlog == null) null else _hiddenPlog == "t"
    val frsDisabled: Boolean?
        get() = if (_frsDisabled == null) null else _frsDisabled == "t"
}
