package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.ListLicensePlatesNumbersResponse
import com.sesameware.domain.model.response.RemoveLicensePlateResponse

interface LPRSRepository {
    suspend fun addLicensePlateNumber(
        flatId: Int,
        number: String
    ): AddLicensePlateNumberResponse

    suspend fun listLicensePlateNumbers(
        flatId: Int
    ): ListLicensePlatesNumbersResponse

    suspend fun removeLicensePlateNumber(
        flatId: Int,
        number: String
    ): RemoveLicensePlateResponse
}
