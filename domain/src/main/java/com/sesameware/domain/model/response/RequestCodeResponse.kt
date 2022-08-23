package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias RequestCodeResponse = ApiResult<RequestCode>?

data class RequestCode(
    @Json(name = "method") val _method: String = AUTH_METHOD_SMS_CODE,
    @Json(name = "confirmationNumbers") val confirmationNumbers: MutableList<String>?
) {
    val method: AuthMethod
        get() {
            return when(_method) {
                AUTH_METHOD_OUTGOING_CALL -> AuthMethod.OUTGOING_CALL
                AUTH_METHOD_FLASH_CALL -> AuthMethod.FLASH_CALL
                else -> AuthMethod.SMS_CODE
            }
        }

    companion object {
        const val AUTH_METHOD_SMS_CODE = "smsCode"
        const val AUTH_METHOD_OUTGOING_CALL = "outgoingCall"
        const val AUTH_METHOD_FLASH_CALL = "flashCall"
    }
}

enum class AuthMethod {
    SMS_CODE,
    OUTGOING_CALL,
    FLASH_CALL;

    companion object {
        fun getType(type: String): AuthMethod {
            return when (type) {
                RequestCode.AUTH_METHOD_OUTGOING_CALL -> OUTGOING_CALL
                RequestCode.AUTH_METHOD_FLASH_CALL -> FLASH_CALL
                else -> SMS_CODE
            }
        }
    }
}
