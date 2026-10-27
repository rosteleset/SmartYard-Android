package com.sesameware.data.repository

import com.sesameware.domain.model.CommonError
import com.sesameware.domain.model.CommonErrorThrowable
import com.sesameware.domain.model.ErrorStatus
import com.sesameware.domain.model.response.ApiResultNull
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
open class BaseRepository(open val moshi: Moshi) {
    private val TAG = "BaseRepository"
    internal fun <T> Response<T>.getResponseBody(): T? {
        if (!this.isSuccessful) {
            throw HttpException(this)
        }
        return this.body()
    }

    internal suspend fun <T> safeApiCall(apiCall: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            try {
                apiCall.invoke()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw CommonErrorThrowable(convertError(throwable))
            }
        }
    }

    private fun convertError(throwable: Throwable): CommonError {
        Timber.tag(TAG).e(throwable)
        val status = getStatus(throwable)
        return if (throwable is HttpException) {
            val httpCode = throwable.code()
            val errorResponse = convertErrorBody(throwable)
            CommonError(throwable, status, httpCode, errorResponse)
        } else {
            CommonError(throwable, status)
        }
    }

    private fun convertErrorBody(throwable: HttpException): ApiResultNull? {
        return try {
            throwable.response()?.errorBody()?.source()?.let {
                val moshiAdapter = moshi.adapter(ApiResultNull::class.java)
                moshiAdapter.fromJson(it)
            }
        } catch (exception: Exception) {
            null
        }
    }
    companion object {
        fun getStatus(throwable: Throwable): ErrorStatus {
            return when (throwable) {
                is UnknownHostException -> ErrorStatus.ERROR_CONNECTION
                is SocketTimeoutException -> ErrorStatus.TIMEOUT
                is HttpException -> {
                    when (throwable.code()) {
                        401 -> ErrorStatus.AUTHORIZATION_ON_ANOTHER
                        406 -> ErrorStatus.UNAUTHORIZED
                        410 -> ErrorStatus.UNAUTHORIZED
                        424 -> ErrorStatus.UNAUTHORIZED
                        425 -> ErrorStatus.ATTENTION
                        400 -> ErrorStatus.BAD_RESPONSE
                        422 -> ErrorStatus.BAD_RESPONSE
                        404 -> ErrorStatus.NOT_FOUND
                        429 -> ErrorStatus.TOO_MANY_REQUESTS
                        else -> ErrorStatus.HTTP_OTHER
                    }
                }
                is IOException -> ErrorStatus.ERROR_CONNECTION
                else -> ErrorStatus.OTHER
            }
        }
    }
}
