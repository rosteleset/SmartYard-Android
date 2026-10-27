package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.ListLicensePlatesNumbersResponse
import com.sesameware.domain.model.response.ListLicensePlatesNumbersV2Response
import com.sesameware.domain.model.response.RemoveLicensePlateResponse
import com.sesameware.domain.model.response.SupportedPlateNumberFormatsResponse

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

    suspend fun addLicensePlateNumberV2(
        flatId: Int,
        number: String,
        countryCode: String,
        validFrom: String?,
        validTo: String?
    ): AddLicensePlateNumberResponse

    suspend fun listLicensePlateNumbersV2(
        flatId: Int
    ): ListLicensePlatesNumbersV2Response

    suspend fun listSupportedPlateNumberFormats(): SupportedPlateNumberFormatsResponse
}
