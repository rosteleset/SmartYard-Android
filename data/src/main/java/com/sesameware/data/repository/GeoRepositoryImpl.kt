package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.GeoRepository
import com.sesameware.domain.model.request.GetAddressRequest
import com.sesameware.domain.model.request.GetCoderRequest
import com.sesameware.domain.model.request.GetHousesRequest
import com.sesameware.domain.model.request.GetServicesRequest
import com.sesameware.domain.model.request.GetStreetsRequest
import com.sesameware.domain.model.response.GetAddressResponse
import com.sesameware.domain.model.response.GetAllLocationsResponse
import com.sesameware.domain.model.response.GetCoderResponse
import com.sesameware.domain.model.response.GetHousesResponse
import com.sesameware.domain.model.response.GetServicesResponse
import com.sesameware.domain.model.response.GetStreetsResponse

/**
 * @author Nail Shakurov
 * Created on 11/03/2020.
 */
class GeoRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : GeoRepository, BaseRepository(moshi) {
    override suspend fun getServices(id: Int): GetServicesResponse {
        return safeApiCall {
            teledomApi.getServices(
                DataModule.BASE_URL + "geo/getServices",
                GetServicesRequest(id)).getResponseBody()
        }
    }

    override suspend fun getAllLocations(): GetAllLocationsResponse {
        return safeApiCall {
            teledomApi.getAllLocations(DataModule.BASE_URL + "geo/getAllLocations")
        }
    }

    override suspend fun getStreets(locationId: Int): GetStreetsResponse {
        return safeApiCall {
            teledomApi.getStreets(
                DataModule.BASE_URL + "geo/getStreets",
                GetStreetsRequest(locationId))
        }
    }

    override suspend fun getHouses(streetId: Int): GetHousesResponse {
        return safeApiCall {
            teledomApi.getHouses(
                DataModule.BASE_URL + "geo/getHouses",
                GetHousesRequest(streetId))
        }
    }

    override suspend fun getAddress(streetId: Int): GetAddressResponse {
        return safeApiCall {
            teledomApi.getAddress(
                DataModule.BASE_URL + "geo/address",
                GetAddressRequest(streetId))
        }
    }

    override suspend fun getCoder(address: String): GetCoderResponse {
        return safeApiCall {
            teledomApi.getCoder(
                DataModule.BASE_URL + "geo/coder",
                GetCoderRequest(address))
        }
    }
}
