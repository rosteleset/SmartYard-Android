package com.sesameware.data

import com.sesameware.data.local.dao.OfflineAddressDao
import com.sesameware.data.local.entity.OfflineAddressEntity
import com.sesameware.data.local.entity.OfflineDoorEntity
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.data.remote.TeledomApi
import com.sesameware.data.repository.AddressRepositoryImpl
import com.sesameware.data.repository.OfflineCacheSession
import com.sesameware.domain.model.response.Address
import com.sesameware.domain.model.response.ApiResult
import com.sesameware.domain.model.response.GetAddressListResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.startCoroutine

class OfflineAddressRepositoryTest {
    private class MemoryDao : OfflineAddressDao() {
        var addresses = emptyList<OfflineAddressEntity>()
        var doors = emptyList<OfflineDoorEntity>()
        override suspend fun getAllAddresses() = addresses.sortedBy { it.position }
        override suspend fun getDoorsForAddress(houseId: Int) = doors.filter { it.houseId == houseId }
        override suspend fun getAllDoors() = doors.sortedBy { it.position }
        override suspend fun insertAddresses(addresses: List<OfflineAddressEntity>) { this.addresses = addresses }
        override suspend fun insertDoors(doors: List<OfflineDoorEntity>) { this.doors = doors }
        override suspend fun deleteAllAddresses() { addresses = emptyList() }
        override suspend fun deleteAllDoors() { doors = emptyList() }
    }

    private class Fixture {
        var token: String? = "first-account"
        val dao = MemoryDao()
        val session = OfflineCacheSession()
        var fetch: suspend () -> Response<GetAddressListResponse> = { response(emptyList()) }
        val preferences = Proxy.newProxyInstance(
            PreferenceStorage::class.java.classLoader, arrayOf(PreferenceStorage::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAuthToken" -> token
                else -> error("Unexpected preference access: ${method.name}")
            }
        } as PreferenceStorage
        @Suppress("UNCHECKED_CAST")
        val api = Proxy.newProxyInstance(
            TeledomApi::class.java.classLoader, arrayOf(TeledomApi::class.java)
        ) { _, method, args ->
            check(method.name == "getAddressList")
            fetch.startCoroutine(args.last() as Continuation<Response<GetAddressListResponse>>)
            COROUTINE_SUSPENDED
        } as TeledomApi
        fun repository() = AddressRepositoryImpl(api, Moshi.Builder().build(), dao, preferences, session)
    }

    @Test fun retainsEmptyAddressesFiltersCodesAndKeepsDoorIdentityPerAddress() = runBlocking {
        val fixture = Fixture()
        fixture.fetch = { response(listOf(
            Address(houseId = 20, address = "First", doors = listOf(
                Address.Door(domophoneId = 7, doorId = 1, name = "Entrance", doorCode = "123"),
                Address.Door(domophoneId = 7, doorId = 2, name = "Gate", doorCode = "456"),
                Address.Door(domophoneId = 9, doorCode = " ")
            )),
            Address(houseId = 10, address = "Second", doors = listOf(
                Address.Door(domophoneId = 7, doorId = 1, doorCode = "789")
            )),
            Address(houseId = 30, address = "Empty")
        )) }
        val repo = fixture.repository()
        repo.getAddressList()
        val cached = repo.getOfflineAddresses()
        assertEquals(listOf(20, 10, 30), cached.map { it.houseId })
        assertEquals(listOf("123", "456"), cached.first().doors.map { it.doorCode })
        assertTrue(cached.last().doors.isEmpty())
        assertEquals(3, fixture.dao.doors.map { Triple(it.houseId, it.domophoneId, it.doorId) }.toSet().size)
    }

    @Test fun networkFailureDoesNotEraseCacheAndSuccessfulEmptyResponseDoes() = runBlocking {
        val fixture = Fixture()
        fixture.fetch = { response(listOf(Address(houseId = 1))) }
        val repo = fixture.repository()
        repo.getAddressList()
        fixture.fetch = { throw IOException("offline") }
        try { repo.getAddressList(); fail("Expected failure") } catch (_: com.sesameware.domain.model.CommonErrorThrowable) { }
        assertEquals(1, repo.getOfflineAddresses().size)
        fixture.fetch = { response(emptyList()) }
        repo.getAddressList()
        assertTrue(repo.getOfflineAddresses().isEmpty())
    }

    @Test fun anotherAccountCannotReadCodes() = runBlocking {
        val fixture = Fixture()
        fixture.fetch = { response(listOf(Address(houseId = 1))) }
        val repo = fixture.repository()
        repo.getAddressList()
        fixture.token = "second-account"
        assertTrue(repo.getOfflineAddresses().isEmpty())
        fixture.token = null
        assertTrue(repo.getOfflineAddresses().isEmpty())
    }

    @Test fun logoutFromAnotherRepositoryInvalidatesInflightResponseEvenForSameAccount() = runBlocking {
        val fixture = Fixture()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        fixture.fetch = {
            started.complete(Unit)
            release.await()
            response(listOf(Address(houseId = 1)))
        }
        val request = async { fixture.repository().getAddressList() }
        started.await()
        fixture.repository().clearOfflineCache()
        release.complete(Unit)
        request.await()
        assertTrue(fixture.repository().getOfflineAddresses().isEmpty())
        assertTrue(fixture.dao.doors.isEmpty())
    }

    companion object {
        private fun response(addresses: List<Address>): Response<GetAddressListResponse> =
            Response.success(ApiResult(200, "OK", "", addresses))
    }
}
