package com.sesameware.domain.interfaces

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
interface GeoRepository {

    suspend fun getServices(id: Int): GetServicesResponse

    suspend fun getAllLocations(): GetAllLocationsResponse

    suspend fun getStreets(locationId: Int): GetStreetsResponse

    suspend fun getHouses(streetId: Int): GetHousesResponse

    suspend fun getAddress(streetId: Int): GetAddressResponse

    suspend fun getCoder(address: String): GetCoderResponse
}
