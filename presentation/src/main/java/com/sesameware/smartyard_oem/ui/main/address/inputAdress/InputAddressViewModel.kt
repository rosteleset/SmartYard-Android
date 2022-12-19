package com.sesameware.smartyard_oem.ui.main.address.inputAdress

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.model.CommonError
import com.sesameware.domain.model.response.HousesData
import com.sesameware.domain.model.response.LocationData
import com.sesameware.domain.model.response.ServicesData
import com.sesameware.domain.model.response.StreetsData
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 11/03/2020.
 */
class InputAddressViewModel(private val geoInteractor: GeoInteractor) : GenericViewModel() {

    val cityList = MutableLiveData<List<LocationData>>()
    val progressCity = MutableLiveData<Boolean>()

    val streetList = MutableLiveData<List<StreetsData>>()
    val progressStreet = MutableLiveData<Boolean>()

    val houseList = MutableLiveData<List<HousesData>>()
    val progressHouse = MutableLiveData<Boolean>()

    val confirmError = MutableLiveData<Event<CommonError>>()

    val servicesList = MutableLiveData<Event<List<ServicesData>>>()

    val navigationToNoNetwork = MutableLiveData<Event<String>>()

    init {
        getAllLocation()
    }

    private fun getAllLocation() {
        progressCity.postValue(true)
        viewModelScope.launchSimple({
            confirmError.value = Event(it)
            true
        }) {
            val res = geoInteractor.getAllLocations()
            cityList.postValue(res.data)
            progressCity.postValue(false)
        }
    }

    fun getStreet(locationId: Int) {
        progressStreet.postValue(true)
        viewModelScope.launchSimple({
            confirmError.value = Event(it)
            true
        }) {
            val res = geoInteractor.getStreets(locationId)
            streetList.postValue(res.data ?: emptyList())
            progressStreet.postValue(false)
        }
    }

    fun getHouses(streetId: Int) {
        progressHouse.postValue(true)
        viewModelScope.launchSimple({
            confirmError.value = Event(it)
            true
        }) {
            val res = geoInteractor.getHouses(streetId)
            houseList.postValue(res.data)
            progressHouse.postValue(false)
        }
    }

    fun getServices(houseId: Int?, address: String) {
        if (houseId == null) {
            navigationToNoNetwork.postValue(Event(address))
        } else {
            viewModelScope.withProgress({
                confirmError.value = Event(it)
                true
            }) {
                val res = geoInteractor.getServices(houseId)
                if (res?.data?.isEmpty() != false)
                    navigationToNoNetwork.postValue(Event(address))
                else {
                    servicesList.postValue(Event(res.data))
                    // navigationToNoNetwork.postValue(Event(address))
                }
            }
        }
    }
}
