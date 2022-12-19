package com.sesameware.domain.model.response

import com.google.gson.annotations.SerializedName

/**
 * @author Nail Shakurov
 * Created on 16/03/2020.
 */
typealias ResetCodeResponse = ApiResult<Code>

data class Code(
    @SerializedName("code")
    val code: Int = 0 // 3232

)
