package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.GeoRepository
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
class GeoInteractor(
    private val repository: GeoRepository
) {

    suspend fun getServices(id: Int): GetServicesResponse {
        return repository.getServices(id)
    }

    suspend fun getAllLocations(): GetAllLocationsResponse {
        return repository.getAllLocations()
    }

    suspend fun getStreets(locationId: Int): GetStreetsResponse {
        return repository.getStreets(locationId)
    }

    suspend fun getHouses(streetId: Int): GetHousesResponse {
        return repository.getHouses(streetId)
    }

    suspend fun getAddress(streetId: Int): GetAddressResponse {
        return repository.getAddress(streetId)
    }

    suspend fun getCoder(address: String): GetCoderResponse {
        return repository.getCoder(address)
    }
}
