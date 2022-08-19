package com.sesameware.domain.model

import androidx.annotation.StringRes
import com.sesameware.domain.R
import com.sesameware.domain.model.response.ApiResultNull

data class CommonError(
    val cause: Throwable,
    val status: ErrorStatus,
    val httpCode: Int = -1,
    val errorData: ApiResultNull? = null
)

data class CommonErrorThrowable(
    val data: CommonError
) : Throwable()

enum class ErrorStatus(@StringRes val messageId: Int, http: Boolean = true) {
    ERROR_CONNECTION(R.string.common_error_connection, false),
    TIMEOUT(R.string.common_timeout, false),
    OTHER(R.string.common_error, false),
    UNAUTHORIZED(R.string.common_unauthorized),
    AUTHORIZATION_ON_ANOTHER(R.string.common_do_authorization_on_another),
    NOT_FOUND(R.string.common_not_found),
    BAD_RESPONSE(R.string.common_error),
    TOO_MANY_REQUESTS(R.string.common_too_many),
    HTTP_OTHER(R.string.common_error)
}
