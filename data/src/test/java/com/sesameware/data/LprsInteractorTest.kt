package com.sesameware.data

import com.sesameware.domain.interactors.LPRSInteractor
import com.sesameware.domain.interfaces.LPRSRepository
import com.sesameware.domain.model.response.ApiResult
import com.sesameware.domain.model.response.ApiResultNull
import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.LicensePlateNumberV2
import com.sesameware.domain.model.response.ListLicensePlatesNumbersResponse
import com.sesameware.domain.model.response.ListLicensePlatesNumbersV2Response
import com.sesameware.domain.model.response.RemoveLicensePlateResponse
import com.sesameware.domain.model.response.SupportedPlateNumberFormat
import com.sesameware.domain.model.response.SupportedPlateNumberFormatsResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LprsInteractorTest {
    private class FakeRepository : LPRSRepository {
        var v1ListCalled = false
        var v2ListCalled = false
        var lastV2Add: List<Any?>? = null

        override suspend fun addLicensePlateNumber(flatId: Int, number: String) =
            ApiResultNull(200, "OK", "")

        override suspend fun listLicensePlateNumbers(flatId: Int): ListLicensePlatesNumbersResponse {
            v1ListCalled = true
            return ApiResult(200, "OK", "", listOf("A123AA77"))
        }

        override suspend fun removeLicensePlateNumber(flatId: Int, number: String): RemoveLicensePlateResponse =
            ApiResultNull(200, "OK", "")

        override suspend fun addLicensePlateNumberV2(
            flatId: Int,
            number: String,
            countryCode: String,
            validFrom: String?,
            validTo: String?
        ): AddLicensePlateNumberResponse {
            lastV2Add = listOf(flatId, number, countryCode, validFrom, validTo)
            return ApiResultNull(200, "OK", "")
        }

        override suspend fun listLicensePlateNumbersV2(flatId: Int): ListLicensePlatesNumbersV2Response {
            v2ListCalled = true
            return ApiResult(
                200, "OK", "",
                listOf(
                    LicensePlateNumberV2("A123AA77", "ru", "2026-10-14T10:00:00+03:00", null)
                )
            )
        }

        override suspend fun listSupportedPlateNumberFormats(): SupportedPlateNumberFormatsResponse =
            ApiResult(200, "OK", "", listOf(SupportedPlateNumberFormat("ru", "[A-Z]\\d{3}")))
    }

    @Test fun v1ListUsesLegacyEndpointAndCarriesNoCountryData() = runBlocking {
        val repo = FakeRepository()
        val interactor = LPRSInteractor(repo)
        val result = interactor.listLicensePlates(1)
        assertTrue(repo.v1ListCalled)
        assertFalse(repo.v2ListCalled)
        assertEquals("ru", result?.data?.first()?.countryCode)
    }

    @Test fun v2ListUsesV2EndpointAndMapsCountryAndValidity() = runBlocking {
        val repo = FakeRepository()
        val interactor = LPRSInteractor(repo)
        val result = interactor.listLicensePlatesV2(1)
        assertTrue(repo.v2ListCalled)
        assertFalse(repo.v1ListCalled)
        val plate = result?.data?.first()!!
        assertEquals("A123AA77", plate.number)
        assertEquals("ru", plate.countryCode)
        assertEquals("2026-10-14T10:00:00+03:00", plate.validFrom)
        assertNull(plate.validTo)
    }

    @Test fun v2AddForwardsAllFields() = runBlocking {
        val repo = FakeRepository()
        val interactor = LPRSInteractor(repo)
        interactor.addLicensePlateV2(5, "A123AA77", "am", "from", "to")
        assertEquals(listOf<Any?>(5, "A123AA77", "am", "from", "to"), repo.lastV2Add)
    }

    @Test fun supportedFormatsPassedThrough() = runBlocking {
        val repo = FakeRepository()
        val interactor = LPRSInteractor(repo)
        val formats = interactor.listSupportedPlateNumberFormats()?.data!!
        assertEquals(1, formats.size)
        assertEquals("ru", formats.first().countryCode)
    }
}
