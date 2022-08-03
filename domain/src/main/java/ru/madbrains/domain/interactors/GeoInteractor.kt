package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.GeoRepository
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
