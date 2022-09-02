package com.sesameware.smartyard_oem.ui.main.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.DatabaseInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.AddressItem
import com.sesameware.domain.model.StateButton
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import com.sesameware.smartyard_oem.ui.main.address.models.ParentModel
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.EventLogModel
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModel
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.Yard
import timber.log.Timber

class AddressViewModel(
    private val addressInteractor: AddressInteractor,
    private val preferenceStorage: PreferenceStorage,
    private val authInteractor: AuthInteractor,
    private val issueInteractor: IssueInteractor,
    private val databaseInteractor: DatabaseInteractor
) : GenericViewModel() {

    private val _dataList = MutableLiveData<List<DisplayableItem>>()
    val dataList: LiveData<List<DisplayableItem>>
        get() = _dataList

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean>
        get() = _progress
    private val _navigationToAuth = MutableLiveData<Event<Unit>>()
    val navigationToAuth: LiveData<Event<Unit>>
        get() = _navigationToAuth

    var expandedHouseId = mutableSetOf<Int>()  // множество развёрнутых адресов

    fun openDoor(domophoneId: Int, doorId: Int?) {
        viewModelScope.withProgress {
            authInteractor.openDoor(domophoneId, doorId)
        }
    }

    //учитывать ли кэш при следующем запросе списка адресов
    var nextListNoCache = true

    init {
        getDataList()
    }

    private suspend fun getHouseIdFlats(): HashMap<Int, List<Flat>> {
        val res = addressInteractor.getSettingsList()
        val houseFlats = hashMapOf<Int, MutableSet<Int>>()
        val flatToNumber = hashMapOf<Int, String>()
        res?.data?.forEach { settingItem ->
            flatToNumber[settingItem.flatId] = settingItem.flatNumber
            if (settingItem.hasPlog) {
                (houseFlats.getOrPut(settingItem.houseId) {mutableSetOf()}).add(settingItem.flatId)
            }
        }

        val houseIdFlats = hashMapOf<Int, List<Flat>>()  // идентификатор дома с квартирами пользователя
        houseFlats.keys.forEach {houseId ->
            houseIdFlats[houseId] = houseFlats[houseId]!!.map { flatId ->
                val resIntercom = addressInteractor.getIntercom(flatId)
                val frsEnabled = (resIntercom.data.frsDisabled == false)
                Flat(flatId, flatToNumber[flatId]!!, frsEnabled)
            }
            houseIdFlats[houseId]?.sortedBy {
                it.flatNumber
            }
        }

        Timber.d("debug_mm houseIdFlats = $houseIdFlats")
        return houseIdFlats
    }

    fun getDataList(forceRefresh: Boolean = false) {
        val noCache = nextListNoCache || forceRefresh
        nextListNoCache = false
        viewModelScope.withProgress(
            handleError = {
                true
            },
            progress = _progress
        ) {
            if (noCache) {
                preferenceStorage.xDmApiRefresh = true
            }
            val houseIdFlats = getHouseIdFlats()
            if (noCache) {
                preferenceStorage.xDmApiRefresh = true
            }
            val res = addressInteractor.getAddressList()
            if (res?.data == null) {
                if (!preferenceStorage.whereIsContractWarningSeen) {
                    _navigationToAuth.value = (Event(Unit))
                }
            } else {
                Timber.d(this.javaClass.simpleName, res.data.size)
                databaseInteractor.deleteAll()
                var hasExpanded = false
                val list: MutableList<DisplayableItem> = (
                    res.data.map { addressItem ->
                        val mutableList = mutableListOf<DisplayableItem>()
                        var hasYards = false
                        addressItem.doors.forEach {
                            Timber.d(
                                this.javaClass.simpleName,
                                "Door address: ${addressItem.address}"
                            )

                            databaseInteractor
                                .createItem(
                                    AddressItem(
                                        name = it.name,
                                        address = addressItem.address,
                                        icon = it.icon,
                                        domophoneId = it.domophoneId,
                                        doorId = it.doorId,
                                        state = StateButton.CLOSE
                                    )
                                )

                            mutableList.add(
                                Yard().apply {
                                    image = when (it.icon) {
                                        "barrier" -> R.drawable.ic_barrier
                                        "gate" -> R.drawable.ic_gates
                                        "wicket" -> R.drawable.ic_wicket
                                        "entrance" -> R.drawable.ic_porch
                                        else -> R.drawable.ic_barrier
                                    }
                                    caption = it.name
                                    open = false
                                    domophoneId = it.domophoneId
                                    doorId = it.doorId
                                    hasYards = true
                                }
                            )
                        }
                        if (addressItem.cctv > 0) {
                            mutableList.add(
                                VideoCameraModel().apply {
                                    caption = "Видеокамеры"
                                    counter = addressItem.cctv
                                    houseId = addressItem.houseId
                                    address = addressItem.address
                                }
                            )
                        }
                        if (addressItem.hasPlog && hasYards && houseIdFlats[addressItem.houseId]?.size != 0) {
                            mutableList.add(
                                EventLogModel().apply {
                                    caption = "События"
                                    counter = 0
                                    houseId = addressItem.houseId
                                    address = addressItem.address
                                    flats = houseIdFlats[addressItem.houseId]?.toList() ?: listOf()
                                }
                            )
                        }
                        hasExpanded = hasExpanded || expandedHouseId.contains(addressItem.houseId)
                        ParentModel(
                            addressItem.address,
                            addressItem.houseId,
                            mutableList,
                            hasYards,
                            expandedHouseId.contains(addressItem.houseId)
                        )
                    }.toMutableList()
                )
                list.sortWith(compareBy(
                    {!(it as ParentModel).hasYards},
                    {(it as ParentModel).addressTitle}
                ))
                if (!forceRefresh && list.size > 0 && !hasExpanded) {
                    (list[0] as? ParentModel)?.let { parent ->
                        expandedHouseId.add(parent.houseId)
                        parent.isExpanded = true
                    }
                }
                val listConnect = issueInteractor.listConnectIssue()?.data
                _dataList.value = list.plus(
                    listConnect?.map {
                        IssueModel(
                            it.address ?: "",
                            it.key ?: "",
                            it.courier ?: ""
                        )
                    } ?: emptyList()
                )
            }
        }
    }
}
