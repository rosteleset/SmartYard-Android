package com.sesameware.data

import com.sesameware.domain.model.request.AddLicensePlateNumberRequestV2
import com.sesameware.domain.model.response.LicensePlateNumberV2
import com.sesameware.domain.model.response.LprsVersion
import com.sesameware.domain.model.response.ProviderConfig
import com.sesameware.domain.model.response.SupportedPlateNumberFormat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LprsV2DtoTest {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test fun missingLprsVersionDefaultsToV1() {
        val adapter = moshi.adapter(ProviderConfig::class.java)
        val config = adapter.fromJson("{}")!!
        assertEquals("1", config.lprsVersion)
        assertEquals(LprsVersion.V1, config.lprsVersionType)
    }

    @Test fun unknownLprsVersionResolvesToV1() {
        assertEquals(LprsVersion.V1, LprsVersion.from("42"))
        assertEquals(LprsVersion.V1, LprsVersion.from(null))
        assertEquals(LprsVersion.V1, LprsVersion.from("1"))
        assertEquals(LprsVersion.V2, LprsVersion.from("2"))
    }

    @Test fun lprsVersion2ParsedFromConfig() {
        val adapter = moshi.adapter(ProviderConfig::class.java)
        val config = adapter.fromJson("""{"lprsVersion":"2"}""")!!
        assertEquals(LprsVersion.V2, config.lprsVersionType)
    }

    @Test fun addRequestOmitsNullValidityFields() {
        val adapter = moshi.adapter(AddLicensePlateNumberRequestV2::class.java)
        val json = adapter.toJson(AddLicensePlateNumberRequestV2(flatId = 1, number = "A123AA77"))
        assertTrue(json.contains("\"countryCode\":\"ru\""))
        assertFalse(json.contains("validFrom"))
        assertFalse(json.contains("validTo"))
    }

    @Test fun addRequestKeepsProvidedValidityFields() {
        val adapter = moshi.adapter(AddLicensePlateNumberRequestV2::class.java)
        val json = adapter.toJson(
            AddLicensePlateNumberRequestV2(
                flatId = 1,
                number = "A123AA77",
                countryCode = "am",
                validFrom = "2026-10-14T10:00:00+03:00",
                validTo = "2026-10-14T18:00:00+03:00"
            )
        )
        assertTrue(json.contains("\"countryCode\":\"am\""))
        assertTrue(json.contains("validFrom"))
        assertTrue(json.contains("validTo"))
    }

    @Test fun listItemToleratesMissingValidity() {
        val adapter = moshi.adapter(LicensePlateNumberV2::class.java)
        val item = adapter.fromJson("""{"plateNumber":"A123AA77","countryCode":"ru"}""")!!
        assertEquals("A123AA77", item.plateNumber)
        assertEquals("ru", item.countryCode)
        assertNull(item.validFrom)
        assertNull(item.validTo)
    }

    @Test fun supportedFormatParsed() {
        val adapter = moshi.adapter(SupportedPlateNumberFormat::class.java)
        val format = adapter.fromJson("""{"countryCode":"ru","regex":"[A-Z]\\d{3}"}""")!!
        assertEquals("ru", format.countryCode)
        assertEquals("[A-Z]\\d{3}", format.regex)
    }
}
