package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import ru.madbrains.domain.model.CommonError
import ru.madbrains.domain.model.CommonErrorThrowable
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.domain.model.ErrorStatus.AUTHORIZATION_ON_ANOTHER
import ru.madbrains.domain.model.ErrorStatus.BAD_RESPONSE
import ru.madbrains.domain.model.ErrorStatus.HTTP_OTHER
import ru.madbrains.domain.model.ErrorStatus.NOT_FOUND
import ru.madbrains.domain.model.ErrorStatus.ERROR_CONNECTION
import ru.madbrains.domain.model.ErrorStatus.OTHER
import ru.madbrains.domain.model.ErrorStatus.TIMEOUT
import ru.madbrains.domain.model.ErrorStatus.TOO_MANY_REQUESTS
import ru.madbrains.domain.model.ErrorStatus.UNAUTHORIZED
import ru.madbrains.domain.model.response.ApiResultNull
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
                is UnknownHostException -> ERROR_CONNECTION
                is IOException -> ERROR_CONNECTION
                is SocketTimeoutException -> TIMEOUT
                is HttpException -> {
                    when (throwable.code()) {
                        401 -> AUTHORIZATION_ON_ANOTHER
                        406 -> UNAUTHORIZED
                        410 -> UNAUTHORIZED
                        424 -> UNAUTHORIZED
                        400 -> BAD_RESPONSE
                        422 -> BAD_RESPONSE
                        404 -> NOT_FOUND
                        429 -> TOO_MANY_REQUESTS
                        else -> HTTP_OTHER
                    }
                }
                else -> OTHER
            }
        }
    }
}
