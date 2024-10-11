package ru.madbrains.domain.interfaces

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
interface GeoRepository {

    suspend fun getServices(id: Int, flat: Int): GetServicesResponse

    suspend fun getAllLocations(): GetAllLocationsResponse

    suspend fun getStreets(locationId: Int): GetStreetsResponse

    suspend fun getHouses(streetId: Int): GetHousesResponse

    suspend fun getAddress(streetId: Int): GetAddressResponse

    suspend fun getCoder(address: String): GetCoderResponse
}
