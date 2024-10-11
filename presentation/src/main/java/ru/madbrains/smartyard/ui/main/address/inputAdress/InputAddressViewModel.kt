package ru.madbrains.smartyard.ui.main.address.inputAdress

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.model.CommonError
import ru.madbrains.domain.model.response.HousesData
import ru.madbrains.domain.model.response.LocationData
import ru.madbrains.domain.model.response.ServicesData
import ru.madbrains.domain.model.response.StreetsData
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import timber.log.Timber

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

    val navigationToAddingAddress = MutableLiveData<Event<String>>()

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
            cityList.postValue(res.data ?: emptyList())
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
            houseList.postValue(res.data ?: emptyList())
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
                val flat = address.split(",")[3].trim().toIntOrNull() ?: 0
                val res = geoInteractor.getServices(houseId, flat)
                if (res?.data?.isEmpty() == true)
                    navigationToNoNetwork.postValue(Event(address))
                else {
                    if (res?.data?.get(0)?.title!!.isEmpty()){
                        navigationToAddingAddress.postValue(Event(address))
                    }else{
                        servicesList.postValue(Event(res.data))
                    }
                }
            }
        }
    }
}
