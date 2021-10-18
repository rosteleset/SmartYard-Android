package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.GeoRepository
import ru.madbrains.domain.model.request.GetAddressRequest
import ru.madbrains.domain.model.request.GetCoderRequest
import ru.madbrains.domain.model.request.GetHousesRequest
import ru.madbrains.domain.model.request.GetServicesRequest
import ru.madbrains.domain.model.request.GetStreetsRequest
import ru.madbrains.domain.model.response.GetAddressResponse
import ru.madbrains.domain.model.response.GetAllLocationsResponse
import ru.madbrains.domain.model.response.GetCoderResponse
import ru.madbrains.domain.model.response.GetHousesResponse
import ru.madbrains.domain.model.response.GetServicesResponse
import ru.madbrains.domain.model.response.GetStreetsResponse

/**
 * @author Nail Shakurov
 * Created on 11/03/2020.
 */
class GeoRepositoryImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : GeoRepository, BaseRepository(moshi) {
    override suspend fun getServices(id: Int): GetServicesResponse {
        return safeApiCall {
            lantaApi.getServices(GetServicesRequest(id)).getResponseBody()
        }
    }

    override suspend fun getAllLocations(): GetAllLocationsResponse {
        return safeApiCall {
            lantaApi.getAllLocations()
        }
    }

    override suspend fun getStreets(locationId: Int): GetStreetsResponse {
        return safeApiCall {
            lantaApi.getStreets(GetStreetsRequest(locationId))
        }
    }

    override suspend fun getHouses(streetId: Int): GetHousesResponse {
        return safeApiCall {
            lantaApi.getHouses(GetHousesRequest(streetId))
        }
    }

    override suspend fun getAddress(streetId: Int): GetAddressResponse {
        return safeApiCall {
            lantaApi.getAddress(GetAddressRequest(streetId))
        }
    }

    override suspend fun getCoder(address: String): GetCoderResponse {
        return safeApiCall {
            lantaApi.getCoder(GetCoderRequest(address))
        }
    }
}
