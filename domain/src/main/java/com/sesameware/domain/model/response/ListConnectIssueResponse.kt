package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 31/03/2020.
 */

typealias ListConnectIssueResponse = ApiResult<List<Issue>>?

data class Issue(
    @Json(name = "address")
    val address: String? = "", // г Тамбов, Свободная, дом 4 корп.1  Подготовить конверт с qr-кодом. Далее заявку отправить курьеру.
    @Json(name = "courier")
    val courier: String? = "", // f
    @Json(name = "key")
    val key: String? = "" // REM-417561
)
