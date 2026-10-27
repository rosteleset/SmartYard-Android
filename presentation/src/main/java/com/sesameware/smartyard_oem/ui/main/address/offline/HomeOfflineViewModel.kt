package com.sesameware.smartyard_oem.ui.main.address.offline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.model.OfflineAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class HomeOfflineViewModel(
    private val addressInteractor: AddressInteractor,
    private val sharedPrefs: PreferenceStorage
) : ViewModel() {
    private val _offlineAddresses = MutableLiveData<List<OfflineAddress>>()
    val offlineAddresses: LiveData<List<OfflineAddress>> = _offlineAddresses
    private var loaded = false

    fun loadData() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val addresses = try {
                addressInteractor.getOfflineAddresses().map { addr ->
                    addr.isExpanded = sharedPrefs.expandedHouseIds?.contains(addr.houseId) ?: false
                    addr
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            _offlineAddresses.value = addresses
        }
    }

    fun setHouseItemExpanded(position: Int, isExpanded: Boolean) {
        val list = _offlineAddresses.value ?: return
        if (position < 0 || position >= list.size) return
        if (list[position].isExpanded == isExpanded) return

        list[position].isExpanded = isExpanded
    }

    fun persistExpandedHouseIds() {
        sharedPrefs.expandedHouseIds = offlineAddresses.value
            ?.filter { addr ->
                addr.isExpanded
            }?.map { addr ->
                addr.houseId
            }?.toSet()
    }
}
