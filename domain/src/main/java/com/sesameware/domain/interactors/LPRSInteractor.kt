package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.LPRSRepository
import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.ApiResult
import com.sesameware.domain.model.response.LicensePlate
import com.sesameware.domain.model.response.LicensePlateMapper.cyrillicLettersToLatin
import com.sesameware.domain.model.response.LicensePlateMapper.latinLettersToCyrillic
import com.sesameware.domain.model.response.ListLicensePlatesResponse
import com.sesameware.domain.model.response.RemoveLicensePlateResponse
import com.sesameware.domain.model.response.SupportedPlateNumberFormatsResponse

class LPRSInteractor(
    private val repository: LPRSRepository
) {
    suspend fun addLicensePlate(
        flatId: Int,
        licensePlate: LicensePlate
    ): AddLicensePlateNumberResponse {
        val number = licensePlate.number.cyrillicLettersToLatin()
        return repository.addLicensePlateNumber(flatId, number)
    }

    suspend fun listLicensePlates(
        flatId: Int
    ): ListLicensePlatesResponse {
        return repository.listLicensePlateNumbers(flatId)?.let { response ->
            val licensePlates = response.data.map { LicensePlate(it.latinLettersToCyrillic()) }

            ApiResult(
                code = response.code,
                name = response.name,
                message = response.message,
                data = licensePlates,
            )
        }
    }

    suspend fun removeLicensePlate(
        flatId: Int,
        number: String
    ): RemoveLicensePlateResponse {
        return repository.removeLicensePlateNumber(flatId, number)
    }

    suspend fun addLicensePlateV2(
        flatId: Int,
        number: String,
        countryCode: String,
        validFrom: String?,
        validTo: String?
    ): AddLicensePlateNumberResponse {
        return repository.addLicensePlateNumberV2(flatId, number, countryCode, validFrom, validTo)
    }

    suspend fun listLicensePlatesV2(
        flatId: Int
    ): ListLicensePlatesResponse {
        return repository.listLicensePlateNumbersV2(flatId)?.let { response ->
            val licensePlates = response.data.map {
                LicensePlate(
                    number = it.plateNumber,
                    countryCode = it.countryCode,
                    validFrom = it.validFrom,
                    validTo = it.validTo,
                    watcherId = it.watcherId,
                )
            }

            ApiResult(
                code = response.code,
                name = response.name,
                message = response.message,
                data = licensePlates,
            )
        }
    }

    suspend fun listSupportedPlateNumberFormats(): SupportedPlateNumberFormatsResponse {
        return repository.listSupportedPlateNumberFormats()
    }
}
