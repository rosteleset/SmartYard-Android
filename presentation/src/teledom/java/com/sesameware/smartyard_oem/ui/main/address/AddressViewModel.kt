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
import com.sesameware.domain.model.response.Address
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import com.sesameware.smartyard_oem.ui.main.address.helpers.CombinedLiveData
import com.sesameware.smartyard_oem.ui.main.address.models.AddressUiModel
import com.sesameware.smartyard_oem.ui.main.address.models.EntranceId
import com.sesameware.smartyard_oem.ui.main.address.models.EntranceState
import com.sesameware.smartyard_oem.ui.main.address.models.HouseUiModel
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AddressViewModel(
    private val addressInteractor: AddressInteractor,
    override val mPreferenceStorage: PreferenceStorage,
    override val mAuthInteractor: AuthInteractor,
    private val issueInteractor: IssueInteractor,
    override val mDatabaseInteractor: DatabaseInteractor
) : GenericViewModel() {

    private val houseUiState = MutableLiveData<List<HouseUiModel>>()
    private val issueUiState = MutableLiveData<List<IssueModel>>()
    val addressUiState = CombinedLiveData<List<AddressUiModel>>(
        houseUiState,
        issueUiState
    ) { list ->
        list.reduceOrNull { acc, models -> acc + models } ?: listOf()
    }

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress

    private val _navigateToAuth = MutableLiveData<Event<Unit>>()
    val navigateToAuth: LiveData<Event<Unit>> get() = _navigateToAuth

    var houseIdFlats: HashMap<Int, List<Flat>> = hashMapOf()
        private set

    fun openDoor(id: EntranceId) {
        viewModelScope.withProgress {
            mAuthInteractor.openDoor(id.domophoneId, id.doorId)
        }
    }

    init {
        getDataList()
    }

    fun setHouseItemExpanded(position: Int, isExpanded: Boolean) {
        val list = houseUiState.value?.toMutableList() ?: return
        val newState = list[position].copy(isExpanded = isExpanded)
        list[position] = newState
        houseUiState.value = list
    }

    fun setHouseItemSavedPosition(oldPosition: Int, newPosition: Int) {
        val list = houseUiState.value?.toMutableList() ?: return
        // Ignore Issue items, and House items moved over Issue items
        if (oldPosition >= list.size || newPosition >= list.size) return
        val state = list.removeAt(oldPosition)
        list.add(newPosition, state)
        houseUiState.value = list
    }

    fun onItemDrag() {
        val list = houseUiState.value?.toMutableList() ?: return
        val collapsedList = list.map { it.copy(isExpanded = false) }
        houseUiState.value = collapsedList
    }

    fun getDataList(forceRefresh: Boolean = false) {
        viewModelScope.withProgress(progress = _progress) {
            populateHouseIdFlats(forceRefresh)
            launch(Dispatchers.IO) {
                val houses = getHouses(forceRefresh)
                houseUiState.postValue(houses)
            }
            launch(Dispatchers.IO) {
                issueUiState.postValue(getIssues(forceRefresh))
            }
        }
        viewModelScope.withProgress(progress = null) {
            getIssues(forceRefresh)
        }
    }

    private suspend fun populateHouseIdFlats(forceRefresh: Boolean) {
        mPreferenceStorage.xDmApiRefresh = forceRefresh
        val res = addressInteractor.getSettingsList()
        val houseFlats = hashMapOf<Int, MutableSet<Int>>()
        val flatToNumber = hashMapOf<Int, String>()
        res?.data?.forEach { settingItem ->
            flatToNumber[settingItem.flatId] = settingItem.flatNumber
            if (settingItem.hasPlog) {
                (houseFlats.getOrPut(settingItem.houseId) { mutableSetOf() }).add(settingItem.flatId)
            }
        }

        val houseIdFlats = hashMapOf<Int, List<Flat>>()  // идентификатор дома с квартирами пользователя
        houseFlats.keys.forEach { houseId ->
            houseIdFlats[houseId] = houseFlats[houseId]!!.map { flatId ->
                val resIntercom = addressInteractor.getIntercom(flatId)
                val frsEnabled = (resIntercom.data.frsDisabled == false)
                Flat(flatId, flatToNumber[flatId]!!, frsEnabled)
            }
            houseIdFlats[houseId]?.sortedBy {
                it.flatNumber
            }
        }

        Timber.d("debug_dmm houseIdFlats = $houseIdFlats")
        this.houseIdFlats = houseIdFlats
    }

    private suspend fun getHouses(forceRefresh: Boolean): List<HouseUiModel> {
        mPreferenceStorage.xDmApiRefresh = forceRefresh
        val response = addressInteractor.getAddressList()
        if (response?.data == null) {
            if (!mPreferenceStorage.whereIsContractWarningSeen) {
                _navigateToAuth.postValue(Event(Unit))
            }
            return listOf()
        }

        Timber.d(this.javaClass.simpleName, response.data.size)
        mDatabaseInteractor.deleteAll()

        if (response.data.isEmpty()) return emptyList()

        val expandedHouseIds: Set<Int>
        val houseIdPositions: Map<Int, Int>
        val state = houseUiState.value
        if (state == null) {
            expandedHouseIds = mPreferenceStorage.expandedHouseIds ?: emptySet()
            houseIdPositions = mPreferenceStorage.houseIdPositions ?: emptyMap()
        } else {
            expandedHouseIds = state.toExpandedHouseIds()
            houseIdPositions = state.toHouseIdPositions()
        }

        val houseList = response.data.map { addressDto ->
            val entranceList = addressDto.doors.map { entranceDto ->
                addToWidgetDatabase(entranceDto, addressDto)
                EntranceState(
                    iconRes = when (entranceDto.icon) {
                        "barrier" -> R.drawable.ic_barrier
                        "gate" -> R.drawable.ic_gates
                        "wicket" -> R.drawable.ic_wicket
                        "entrance" -> R.drawable.ic_porch
                        else -> R.drawable.ic_barrier
                    },
                    name = entranceDto.name,
                    entranceId = EntranceId(
                        domophoneId = entranceDto.domophoneId,
                        doorId = entranceDto.doorId
                    )
                )
            }
            val houseHasEntrances = entranceList.isNotEmpty()
            val houseHasFlats = houseIdFlats[addressDto.houseId]?.isNotEmpty() ?: false
            val isExpanded = expandedHouseIds.contains(addressDto.houseId)
            HouseUiModel(
                houseId = addressDto.houseId,
                address = addressDto.address,
                entranceList = entranceList,
                cameraCount = addressDto.cctv,
                hasEventLog = addressDto.hasPlog && houseHasEntrances && houseHasFlats,
                isExpanded = isExpanded,
            )
        }.toMutableList()

        houseList.sortWith(
            compareBy<HouseUiModel>(
                { houseIdPositions[it.houseId] },
                { it.entranceList.isEmpty() },
                { it.address },

            )
        )

        val firstLaunch = mPreferenceStorage.justRegistered
        if (firstLaunch && expandedHouseIds.isEmpty()) {
            val newState = houseList[0].copy(isExpanded = true)
            houseList[0] = newState
            mPreferenceStorage.justRegistered = false
        }

        return houseList
    }

    private suspend fun getIssues(forceRefresh: Boolean): List<IssueModel> {
        mPreferenceStorage.xDmApiRefresh = forceRefresh
        val issueList = if (DataModule.providerConfig.issuesVersion != "2") {
            issueInteractor.listConnectIssue()?.data
        } else {
            issueInteractor.listConnectIssueV2()?.data
        }

        val issueModelList = issueList?.map {
            IssueModel(
                it.address ?: "",
                it.key ?: "",
                it.courier ?: ""
            )
        } ?: emptyList()

        return issueModelList
    }

    private suspend fun addToWidgetDatabase(
        entranceDto: Address.Door,
        addressDto: Address
    ) {
        mDatabaseInteractor
            .createItem(
                AddressItem(
                    name = entranceDto.name,
                    address = addressDto.address,
                    icon = entranceDto.icon,
                    domophoneId = entranceDto.domophoneId,
                    doorId = entranceDto.doorId,
                    state = StateButton.CLOSE
                )
            )
    }

    private fun List<HouseUiModel>.toExpandedHouseIds(): Set<Int> =
        this.filter { it.isExpanded }
            .map { it.houseId }
            .toSet()

    private fun List<HouseUiModel>.toHouseIdPositions(): Map<Int, Int> =
        this.take(MAX_SAVED_POSITIONS)
            .mapIndexed { i, h -> h.houseId to i }
            .toMap()

    fun persistUi() {
        mPreferenceStorage.expandedHouseIds = houseUiState.value?.toExpandedHouseIds()
        mPreferenceStorage.houseIdPositions = houseUiState.value?.toHouseIdPositions()
    }

    companion object {
        private const val MAX_SAVED_POSITIONS = 100
    }
}

