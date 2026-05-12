package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.LPRSRepository
import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.ApiResult
import com.sesameware.domain.model.response.LicensePlate
import com.sesameware.domain.model.response.LicensePlateMapper.cyrillicLettersToLatin
import com.sesameware.domain.model.response.LicensePlateMapper.latinLettersToCyrillic
import com.sesameware.domain.model.response.ListLicensePlatesResponse
import com.sesameware.domain.model.response.RemoveLicensePlateResponse

class LPRSInteractor(
    private val repository: LPRSRepository
) {
    suspend fun addLicensePlate(
        flatId: Int,
        licensePlate: LicensePlate
    ): AddLicensePlateNumberResponse {
        val number = licensePlate.value.cyrillicLettersToLatin()
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
        licensePlate: LicensePlate
    ): RemoveLicensePlateResponse {
        val number = licensePlate.value.cyrillicLettersToLatin()
        return repository.removeLicensePlateNumber(flatId, number)
    }
}
