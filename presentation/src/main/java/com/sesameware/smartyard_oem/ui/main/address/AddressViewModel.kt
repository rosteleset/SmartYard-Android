package com.sesameware.smartyard_oem.ui.main.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.DatabaseInteractor
import com.sesameware.domain.interactors.ExtInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.interactors.WebRtcStreamingInteractor
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.domain.model.AddressItem
import com.sesameware.domain.model.StateButton
import com.sesameware.domain.model.request.ExtRequest
import com.sesameware.domain.model.response.Address
import com.sesameware.domain.model.response.CamMap
import com.sesameware.domain.model.response.EntranceCamera
import com.sesameware.domain.model.response.EntrancesView
import com.sesameware.domain.model.response.Story
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import com.sesameware.smartyard_oem.ui.main.address.helpers.CombinedLiveData
import com.sesameware.smartyard_oem.ui.main.address.models.AddressUiModel
import com.sesameware.smartyard_oem.ui.main.address.models.EntranceState
import com.sesameware.smartyard_oem.ui.main.address.models.ExtItemModel
import com.sesameware.smartyard_oem.ui.main.address.models.HouseUiModel
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import com.sesameware.smartyard_oem.ui.main.address.models.Lock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber

class AddressViewModel(
    private val addressInteractor: AddressInteractor,
    override val mPreferenceStorage: PreferenceStorage,
    override val mAuthInteractor: AuthInteractor,
    private val issueInteractor: IssueInteractor,
    override val mDatabaseInteractor: DatabaseInteractor,
    private val extInteractor: ExtInteractor,
    private val webRtcInteractor: WebRtcStreamingInteractor
) : GenericViewModel() {

    val entranceView: EntrancesView
        get() = DataModule.providerConfig.entrancesView
    private val houseUiState = MutableLiveData<List<HouseUiModel>>()
    private val issueUiState = MutableLiveData<List<IssueModel>>()
    private val storiesUiState = MutableLiveData<List<Story>>()
    val stories: LiveData<List<Story>> get() = storiesUiState
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

    val houseIdFlats: HashMap<Int, List<Flat>> = hashMapOf()

    private val _selectedWebRtcUrl = MutableStateFlow<String?>(null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val webRtcState: StateFlow<WebRtcState> = _selectedWebRtcUrl
        .debounce { url ->
            if (url == null) 0L else 300L
        }
        .flatMapLatest { url ->
            if (url == null) {
                flowOf(WebRtcState.Idle)
            } else {
                webRtcInteractor.playStream(url)
                    .catch { emit(WebRtcState.Error("Stream failed")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WebRtcState.Idle
        )

    fun openDoor(id: Lock) {
        viewModelScope.withProgress {
            mAuthInteractor.openDoor(id.domophoneId, id.doorId)
        }
    }

    init {
        getDataList()
    }

    fun setHouseItemExpanded(position: Int, isExpanded: Boolean) {
        val list = houseUiState.value ?: return
        if (position < 0 || position >= list.size) return
        if (list[position].isExpanded == isExpanded) return

        list[position].isExpanded = isExpanded
    }

    fun setEntranceInitialPosition(houseId: Int, index: Int) {
        val list = houseUiState.value ?: return

        val houseIndex = list.indexOfFirst { it.houseId == houseId }
        if (houseIndex == -1) return

        if (list[houseIndex].initialSliderPosition != index) {
            list[houseIndex].initialSliderPosition = index
        }
    }

    fun setWebRtcUrl(whepUrl: String?) {
        Timber.d("debug_dmm new whepUrl: $whepUrl")
        _selectedWebRtcUrl.value = whepUrl
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
        houseUiState.value?.forEach { it.isExpanded = false }
    }

    fun getDataList(forceRefresh: Boolean = false) {
        viewModelScope.withProgress(progress = null) {
            _progress.value = true
            populateHouseIdFlats(forceRefresh)
            val job1 = launch(Dispatchers.IO) {
                val houses = getHouses(forceRefresh)
                houseUiState.postValue(houses)
            }
            val job2 = launch(Dispatchers.IO) {
                val issues = getIssues(forceRefresh)
                issueUiState.postValue(issues)
            }
            val job3 = launch(Dispatchers.IO) {
                if (DataModule.providerConfig.hasStories) {
                    val storiesRes = addressInteractor.getStories()
                    storiesUiState.postValue(storiesRes?.data ?: emptyList())
                } else {
                    storiesUiState.postValue(emptyList())
                }
            }
            joinAll(job1, job2, job3)
            _progress.value = false
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
        val houseIdToFlats = coroutineScope {
            houseFlats.keys.map { houseId ->
                async {
                    val flats = houseFlats[houseId]!!.map { flatId ->
                        async {
                            val resIntercom = addressInteractor.getIntercom(flatId)
                            val frsEnabled = (resIntercom.data.frsDisabled == false)
                            Flat(flatId, flatToNumber[flatId]!!, frsEnabled)
                        }
                    }.awaitAll().filterNotNull().sortedBy {
                        it.flatNumber
                    }
                    houseId to flats
                }
            }.awaitAll().filterNotNull()
        }.toMap()

        houseIdFlats.clear()
        houseIdFlats.putAll(houseIdToFlats)
        Timber.d("debug_dmm houseIdFlats = $houseIdFlats")
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
        val scope = CoroutineScope(currentCoroutineContext())
        scope.launch {
            mDatabaseInteractor.deleteAll()
        }

        if (response.data.isEmpty()) return emptyList()

        val expandedHouseIds: Set<Int>
        val houseIdPositions: Map<Int, Int>
        val entranceIndices: Map<Int, Int>
        val state = houseUiState.value
        if (state == null) {
            expandedHouseIds = mPreferenceStorage.expandedHouseIds ?: emptySet()
            houseIdPositions = mPreferenceStorage.houseIdPositions ?: emptyMap()
            entranceIndices = emptyMap()
        } else {
            expandedHouseIds = state.toExpandedHouseIds()
            houseIdPositions = state.toHouseIdPositions()
            entranceIndices = state.toEntranceIndices()
        }
        val camMapList = addressInteractor.camMap()?.data
        val cameraByEntranceId = getEntranceCamerasByEntranceId(camMapList) ?: emptyMap()
        val cameraByDomophoneId = getEntranceCamerasByDomophoneId(camMapList) ?: emptyMap()
        val addressItems = mutableListOf<AddressItem>()
        val houseList = response.data.map { addressDto ->
            val entranceList = addressDto.doors.map { entranceDto ->
                val dbItem = AddressItem(
                    name = entranceDto.name,
                    address = addressDto.address,
                    icon = entranceDto.icon,
                    domophoneId = entranceDto.domophoneId,
                    doorId = entranceDto.doorId,
                    state = StateButton.CLOSE
                )
                addressItems.add(dbItem)

                val entranceId = entranceDto.entrance.toIntOrNull()

                EntranceState(
                    iconRes = when (entranceDto.icon) {
                        "barrier" -> R.drawable.ic_barrier
                        "gate" -> R.drawable.ic_gates
                        "wicket" -> R.drawable.ic_wicket
                        "entrance" -> R.drawable.ic_porch
                        else -> R.drawable.ic_barrier
                    },
                    name = entranceDto.name,
                    lock = Lock(
                        domophoneId = entranceDto.domophoneId,
                        doorId = entranceDto.doorId
                    ),
                    entranceId = entranceId,
                    cameras = cameraByEntranceId[entranceId] ?: cameraByDomophoneId[entranceDto.domophoneId] ?: emptyList()
                )
            }
            val houseHasEntrances = entranceList.isNotEmpty()
            val houseHasFlats = houseIdFlats[addressDto.houseId]?.isNotEmpty() ?: false
            val isExpanded = expandedHouseIds.contains(addressDto.houseId)

            val extList = addressDto.toExtModels()
            HouseUiModel(
                houseId = addressDto.houseId,
                address = addressDto.address,
                entranceList = entranceList,
                cameraCount = addressDto.cctv,
                hasEventLog = addressDto.hasPlog && houseHasEntrances && houseHasFlats,
                isExpanded = isExpanded,
                extList = extList,
                initialSliderPosition = entranceIndices[addressDto.houseId] ?: 0,
            )
        }.toMutableList()

        houseList.sortWith(
            compareBy(
                { houseIdPositions[it.houseId] },
                { it.entranceList.isEmpty() },
                { it.address },
            )
        )

        coroutineScope {
            mDatabaseInteractor.insertItems(addressItems)
        }

        val firstLaunch = mPreferenceStorage.justRegistered
        if (firstLaunch && expandedHouseIds.isEmpty()) {
            val newState = houseList[0].copy(isExpanded = true)
            houseList[0] = newState
            mPreferenceStorage.justRegistered = false
        }
        return houseList
    }

    private suspend fun Address.toExtModels(): List<ExtItemModel> {
        val address = this
        return coroutineScope {
            address.ext.mapIndexedNotNull { i, item ->
                item.extId?.let { extId ->
                    async {
                        extInteractor.ext(ExtRequest(extId))?.let { extData ->
                            val basePath = extData.data.basePath
                            val code = extData.data.code
                            val version = extData.data.version

                            val hasOptions = version == 2 && extData.data.options != null
                            var isHeaderHidden = false
                            var statusBarColor: String? = null
                            var statusBarStyle: String? = null

                            if (hasOptions) {
                                isHeaderHidden = extData.data.options?.isHeaderHidden ?: false
                                statusBarColor = extData.data.options?.statusBarColor
                                statusBarStyle = extData.data.options?.statusBarStyle
                            }

                            ExtItemModel(
                                extId = item.extId,
                                caption = item.caption,
                                icon = item.icon,
                                order = item.order ?: i,
                                highlight = item.highlight,
                                basePath = basePath,
                                code = code,
                                version = version,
                                isHeaderHidden = isHeaderHidden,
                                statusBarColor = statusBarColor,
                                statusBarStyle = statusBarStyle,
                            )
                        }
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .sortedWith(compareBy { it.order })
        }
    }

    private fun getEntranceCamerasByDomophoneId(camMapList: List<CamMap>?): Map<Int, List<EntranceCamera>>? {
        if (camMapList == null) return null

        return camMapList.groupBy(
            { it.domophoneId },
            { listOf(it.entranceCamera) + it.additionalCameras.orEmpty() }
            ).mapValues { (_, lists) -> lists.flatten() }
            .toMap()
    }

    private fun getEntranceCamerasByEntranceId(camMapList: List<CamMap>?): Map<Int, List<EntranceCamera>>? {
        if (camMapList == null) return null

        return camMapList.filterNot { it.entranceId == null }
            .groupBy(
                { it.entranceId!! },
                { listOf(it.entranceCamera) + it.additionalCameras.orEmpty() }
            )
            .mapValues { (_, lists) -> lists.flatten() }
            .toMap()
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

    private fun List<HouseUiModel>.toExpandedHouseIds(): Set<Int> =
        this.filter { it.isExpanded }
            .map { it.houseId }
            .toSet()

    private fun List<HouseUiModel>.toHouseIdPositions(): Map<Int, Int> =
        this.take(MAX_SAVED_POSITIONS)
            .mapIndexed { i, h -> h.houseId to i }
            .toMap()

    private fun List<HouseUiModel>.toEntranceIndices(): Map<Int, Int> =
        this.associate { it.houseId to it.initialSliderPosition }

    fun persistUi() {
        mPreferenceStorage.expandedHouseIds = houseUiState.value?.toExpandedHouseIds()
        mPreferenceStorage.houseIdPositions = houseUiState.value?.toHouseIdPositions()
    }

    fun setWebRtcUrlForTopmostExpandedItemInRange(visiblePositions: IntRange) {
        val firstExpanded = firstExpandedOrNull(visiblePositions) ?: return
        Timber.d("debug_dmm found item ${firstExpanded.address}")
        val allCameras = firstExpanded.entranceList.flatMap { it.cameras }
        val lastSelectedCam = allCameras.getOrNull(firstExpanded.initialSliderPosition)

        setWebRtcUrl(lastSelectedCam?.whepUrl)
    }

    private fun firstExpandedOrNull(positions: IntRange): HouseUiModel? {
        val list = houseUiState.value ?: return null
        val safeRange = positions.first.coerceIn(0, list.size)..positions.last.coerceIn(-1, list.size - 1)
        if (safeRange.isEmpty()) return null

        return list.slice(safeRange).firstOrNull { it.isExpanded }
    }

    companion object {
        private const val MAX_SAVED_POSITIONS = 100
    }
}

