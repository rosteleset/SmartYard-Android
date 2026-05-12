package com.sesameware.data.repository

import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.LPRSRepository
import com.sesameware.domain.model.request.AddLicensePlateNumberRequest
import com.sesameware.domain.model.request.ListLicensePlateNumbersRequest
import com.sesameware.domain.model.request.RemoveLicensePlateNumberRequest
import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.ListLicensePlatesNumbersResponse
import com.sesameware.domain.model.response.RemoveLicensePlateResponse
import com.squareup.moshi.Moshi

class LPRSRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : LPRSRepository, BaseRepository(moshi)  {
    override suspend fun addLicensePlateNumber(
        flatId: Int,
        number: String
    ): AddLicensePlateNumberResponse {
        return safeApiCall {
            teledomApi.addNumber(
                DataModule.BASE_URL + "lprs/addNumber",
                AddLicensePlateNumberRequest(flatId, number)
            ).getResponseBody()
        }
    }

    override suspend fun listLicensePlateNumbers(flatId: Int): ListLicensePlatesNumbersResponse {
        return safeApiCall {
            teledomApi.listNumbers(
                DataModule.BASE_URL + "lprs/listNumbers",
                ListLicensePlateNumbersRequest(flatId)
            ).getResponseBody()
        }
    }

    override suspend fun removeLicensePlateNumber(
        flatId: Int,
        number: String
    ): RemoveLicensePlateResponse {
        return safeApiCall {
            teledomApi.removeNumber(
                DataModule.BASE_URL + "lprs/removeNumber",
                RemoveLicensePlateNumberRequest(flatId, number)
            ).getResponseBody()
        }
    }
}