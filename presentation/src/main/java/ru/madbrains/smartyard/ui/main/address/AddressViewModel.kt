package ru.madbrains.smartyard.ui.main.address

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.util.copy
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.CCTVInteractor
import ru.madbrains.domain.interactors.DatabaseInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.CommonErrorThrowable
import ru.madbrains.domain.model.StateButton
import ru.madbrains.domain.model.request.BalanceDetailRequest
import ru.madbrains.domain.model.response.BalanceDetailItem
import ru.madbrains.domain.model.response.BalanceDetailResponse
import ru.madbrains.domain.model.response.CameraCctvItemItem
import ru.madbrains.domain.model.response.CameraCctvResponse
import ru.madbrains.domain.model.response.Card
import ru.madbrains.domain.model.response.CardItem
import ru.madbrains.domain.model.response.ContractsResponse
import ru.madbrains.domain.model.response.ContractsResponseItem
import ru.madbrains.domain.model.response.MobilePayItem
import ru.madbrains.domain.model.response.NewFakeResponse
import ru.madbrains.domain.model.response.NewFakeResponseItem
import ru.madbrains.domain.model.response.PlaceItemItem
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.event_log.Flat
import ru.madbrains.smartyard.ui.main.address.models.IssueModel
import ru.madbrains.smartyard.ui.main.address.models.ParentModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.AddressIntercomItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.EventLogModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.IntercomItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.Yard
import ru.madbrains.smartyard.ui.showStandardAlert
import ru.yoomoney.sdk.march.with
import timber.log.Timber

class AddressViewModel(
    private val addressInteractor: AddressInteractor,
    private val preferenceStorage: PreferenceStorage,
    private val authInteractor: AuthInteractor,
    private val issueInteractor: IssueInteractor,
    private val databaseInteractor: DatabaseInteractor,
    private val payInteractor: PayInteractor,
    private val cctvInteractor: CCTVInteractor,
) : GenericViewModel() {
    private val _addressList = MutableLiveData<List<AddressIntercomItem>>()
    val addressList: LiveData<List<AddressIntercomItem>>
        get() = _addressList

    private val _dataList = MutableLiveData<List<DisplayableItem>>()
    val dataList: LiveData<List<DisplayableItem>>
        get() = _dataList

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean>
        get() = _progress

    private val _navigationToAuth = MutableLiveData<Event<Unit>>()
    val navigationToAuth: LiveData<Event<Unit>>
        get() = _navigationToAuth

    private val _openLink = MutableLiveData<Pair<String, String>>()
    val openLink: LiveData<Pair<String, String>>
        get() = _openLink

    private val _placesList = MutableLiveData<List<PlaceItemItem>>()
    val placesList: LiveData<List<PlaceItemItem>>
        get() = _placesList

    private val _camerasList = MutableLiveData<List<CameraCctvItemItem>>()
    val camerasList: LiveData<List<CameraCctvItemItem>>
        get() = _camerasList

    private val _isLoading = MutableLiveData<Pair<Int, Boolean>>()
    val isLoading: LiveData<Pair<Int, Boolean>>
        get() = _isLoading

    private val _contractsList = MutableLiveData<List<ContractsResponseItem>>()
    val contractsList: LiveData<List<ContractsResponseItem>>
        get() = _contractsList

    private val _city = MutableLiveData<String>()
    val city: LiveData<String>
        get() = _city

    private val _balanceDetail =
        MutableLiveData<Pair<Int, Pair<String, List<BalanceDetailItem>>>>(null)
    val balanceDetail: LiveData<Pair<Int, Pair<String, List<BalanceDetailItem>>>>
        get() = _balanceDetail

    private val _cards = MutableLiveData<Pair<String, CardItem>?>(null)
    val cards: LiveData<Pair<String, CardItem>?>
        get() = _cards

    private val _payState = MutableLiveData<MobilePayItem>()
    val payState: LiveData<MobilePayItem>
        get() = _payState

    private val _playingVideoId = MutableLiveData<Int?>()
    val playingVideoId: LiveData<Int?>
        get() = _playingVideoId

    private val _sbpPayOrder = MutableLiveData<NewFakeResponseItem>()
    val sbpPayOrder: LiveData<NewFakeResponseItem>
        get() = _sbpPayOrder

    val userPhone = preferenceStorage.phone

    var expandedHouseId = mutableSetOf<Int>()  // множество развёрнутых адресов


    fun setPlayingId(id: Int?) {
        _playingVideoId.value = id
    }

    fun setCity(city: String) {
        if (city != _city.value) {
            _city.value = city
        }
    }

    fun openDoor(domophoneId: Long, doorId: Int?) {
        viewModelScope.launchSimple {
            authInteractor.openDoor(domophoneId, doorId)
        }
    }

    //учитывать ли кэш при следующем запросе списка адресов
    var nextListNoCache = true

    init {
        getDataList()
        getPlaces()
        getCameras()
    }

    fun onStartLoading() {
        globalData.progressVisibility.postValue(true)
    }

    fun onFinishLoading() {
        globalData.progressVisibility.postValue(false)
    }

    fun newSbpPay(
        contractTitle: String,
        merchant: String,
        summa: Double,
        description: String? = null,
        comment: String? = null,
        notifyMethod: String? = null,
        email: String? = null
    ) {
        viewModelScope.withProgress {
            try {
                val res = payInteractor.newFake(
                    contractTitle,
                    merchant,
                    summa,
                    description,
                    comment,
                    notifyMethod,
                    email
                )
                if (res?.code == 200) {
                    _sbpPayOrder.postValue(res.data)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun createRuComment(comment: String?): String {
        return when (comment) {
            ReasonCardCanceled.FAILED_3D_SECURE.reasonName -> ReasonCardCanceled.FAILED_3D_SECURE.str
            ReasonCardCanceled.CALL_ISSUER.reasonName -> ReasonCardCanceled.CALL_ISSUER.str
            ReasonCardCanceled.CARD_EXPIRED.reasonName -> ReasonCardCanceled.CARD_EXPIRED.str
            ReasonCardCanceled.FRAUD_SUSPECTED.reasonName -> ReasonCardCanceled.FRAUD_SUSPECTED.str
            ReasonCardCanceled.GENERAL_DECLINE.reasonName -> ReasonCardCanceled.GENERAL_DECLINE.str
            ReasonCardCanceled.INSUFFICIENT_FUNDS.reasonName -> ReasonCardCanceled.INSUFFICIENT_FUNDS.str
            ReasonCardCanceled.INVALID_CARD_NUMBER.reasonName -> ReasonCardCanceled.INVALID_CARD_NUMBER.str
            ReasonCardCanceled.INVALID_CSC.reasonName -> ReasonCardCanceled.INVALID_CSC.str
            ReasonCardCanceled.ISSUER_UNAVAILABLE.reasonName -> ReasonCardCanceled.ISSUER_UNAVAILABLE.str
            ReasonCardCanceled.PAYMENT_METHOD_LIMIT_EXCEEDED.reasonName -> ReasonCardCanceled.PAYMENT_METHOD_LIMIT_EXCEEDED.str
            ReasonCardCanceled.PAYMENT_METHOD_RESTRICTED.reasonName -> ReasonCardCanceled.PAYMENT_METHOD_RESTRICTED.str
            else -> ""
        }
    }


    fun checkPay(mdOrder: String? = null, orderId: String? = null) {
        viewModelScope.withProgress {
            try {
                val res = payInteractor.checkPay(mdOrder, orderId)
                if (res?.code == 200) {
                    val mobileItem = res.data.let { item ->
                        MobilePayItem(
                            id = item.id,
                            createdAt = item.createdAt,
                            contractId = item.contractId,
                            contractTitle = item.contractTitle,
                            summa = item.summa,
                            description = item.description,
                            processedAt = item.processedAt,
                            bindingId = item.bindingId,
                            features = item.features,
                            orderId = item.orderId,
                            pan = item.pan,
                            status = item.status,
                            errorCode = item.errorCode,
                            transactionId = item.transactionId,
                            type = item.type,
                            comment = createRuComment(item.comment),
                            confirmationUrl = item.confirmationUrl
                        )
                    }
                    Timber.d("checkPay RESPONSE:${res.data}")
                    _payState.postValue(mobileItem)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun deleteCard(merchant: String, bindingId: String) {
        viewModelScope.launchSimple {
            try {
                val res = payInteractor.removeCard(merchant, bindingId)
                if (res?.code == null) {
                    _cards.value?.let { (contractName, _) ->
                        getCards(contractName)
                    }
//                    _cards.value?.let { (contractName, cardsItem) ->
//                        val item = mutableListOf<Card>()
//                        cardsItem.cards.forEach { card ->
//                            if (card != null){
//                                if (bindingId != card.bindingId){
//                                    item.add(card)
//                                }
//                            }
//                        }
//                        val updateList = cardsItem.copy(cards = item)
//                        _cards.postValue( contractName to updateList)
//                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun checkSbpPay(
        merchant: String,
        id: String,
        status: Int,
        orderId: String,
        processed: String,
        test: String
    ) {
        viewModelScope.withProgress {
            try {
                val res = payInteractor.checkFake(merchant, id, status, orderId, processed, test)
                if (res?.code == 200) {
                    val mobileItem = res.data.let { item ->
                        MobilePayItem(
                            id = item.id,
                            createdAt = item.createdAt,
                            contractId = item.contractId,
                            contractTitle = item.contractTitle,
                            summa = item.summa,
                            description = item.description,
                            processedAt = item.processedAt,
                            bindingId = item.bindingId,
                            features = item.features,
                            orderId = item.orderId,
                            pan = item.pan,
                            status = item.status,
                            errorCode = item.errorCode,
                            transactionId = item.transactionId,
                            type = item.type,
                            comment = createRuComment(item.comment),
                            confirmationUrl = item.confirmationUrl
                        )
                    }
                    _payState.postValue(mobileItem)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun getBalanceDetail(id: String, to: String, from: String) {
        viewModelScope.withProgress {
            val res = payInteractor.getBalanceDetail(id, to, from)
            res?.data?.let {
                _balanceDetail.postValue(id.toInt() to ("$to $from" to it))
            }
        }
    }

    fun getCards(contractName: String) {
        viewModelScope.launchSimple {
            try {
                val res = payInteractor.getCards(contractName)
                if (res?.code == 200) {
                    _cards.postValue(contractName to res.data)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun removeCards() {
        _cards.value = null
    }

    fun setAutoPay(merchant: String, bindingId: String? = null, contractTitle: String? = null) {
        viewModelScope.withProgress {
            try {
                val res = payInteractor.addAutoPay(merchant, bindingId, contractTitle)
                if (res?.code == null) {
                    contractTitle?.let {
                        getCards(contractName = contractTitle)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun removeAutoPay(merchant: String, bindingId: String? = null, contractTitle: String? = null) {
        viewModelScope.withProgress {
            try {
                val res = payInteractor.removeAutoPay(merchant, bindingId, contractTitle)
                if (res?.code == null) {
                    contractTitle?.let {
                        getCards(contractName = contractTitle)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun payAuto(
        merchant: String,
        contractTitle: String,
        summa: Double,
        bindingId: String,
        notifyMethod: String,
        email: String? = null,
        description: String? = null
    ) {
        viewModelScope.withProgress {
            try {
                val res = payInteractor.payAuto(
                    merchant,
                    contractTitle,
                    summa,
                    bindingId,
                    notifyMethod,
                    email,
                    description
                )
                if (res?.code == 200) {
                    val mobileItem = res.data.let { item ->
                        MobilePayItem(
                            id = item.id,
                            createdAt = item.createdAt,
                            contractId = item.contractId,
                            contractTitle = item.contractTitle,
                            summa = item.summa,
                            description = item.description,
                            processedAt = item.processedAt,
                            bindingId = item.bindingId,
                            features = item.features,
                            orderId = item.orderId,
                            pan = item.pan,
                            status = item.status,
                            errorCode = item.errorCode,
                            transactionId = item.transactionId,
                            type = item.type,
                            comment = createRuComment(item.comment),
                            confirmationUrl = item.confirmationUrl
                        )
                    }
                    _payState.postValue(mobileItem)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun mobilePay(
        merchant: String,
        token: String,
        contractTitle: String,
        summa: Double,
        description: String? = null,
        notifyMethod: String = "push",
        email: String? = null,
        saveCard: Boolean? = null,
        saveAuto: Boolean? = null
    ) {
        viewModelScope.withProgress {
            try {
                val res =
                    payInteractor.mobilePay(
                        merchant,
                        token,
                        contractTitle,
                        summa,
                        description,
                        notifyMethod,
                        email,
                        saveCard,
                        saveAuto
                    )
                if (res?.code == 200){
                    val data = res.data.copy(comment = createRuComment(res.data.comment))
                    _payState.postValue(data)
                }
            } catch (_: Exception) { }
        }
    }

    fun sendBalanceDetail(context: Context, id: Int, from: String, to: String, mail: String) {
        viewModelScope.withProgress {
            val res = payInteractor.sendBalanceDetail(id = id, from = from, to = to, mail = mail)
            when (res?.code) {
                200 -> {
                    showStandardAlert(context = context, "Детализация успешно отправлена!")
                }

                else -> {
                    showStandardAlert(context = context, "Ошибка отправки детализации!")
                }
            }
        }
    }

    private suspend fun getHouseIdFlats(): HashMap<Int, List<Flat>> {
        val res = addressInteractor.getSettingsList()
        val houseFlats = hashMapOf<Int, MutableSet<Int>>()
        val flatToNumber = hashMapOf<Int, String>()
        res?.data?.forEach { settingItem ->
            flatToNumber[settingItem.flatId] = settingItem.flatNumber
            if (settingItem.hasPlog == "t") {
                (houseFlats.getOrPut(settingItem.houseId) { mutableSetOf() }).add(settingItem.flatId)
            }
        }

        val houseIdFlats =
            hashMapOf<Int, List<Flat>>()  // идентификатор дома с квартирами пользователя
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

        Timber.d("debug_mm houseIdFlats = $houseIdFlats")
        return houseIdFlats
    }

    fun generateOpenUrl(houseId: Int, flat: Int, domophoneId: Long) {
        viewModelScope.withProgress {
            val res = addressInteractor.generateOpenUrl(houseId, flat, domophoneId)
            res?.data?.let {
                val text = it.text
                val title = it.title
                val url = it.url
                _openLink.postValue("$flat$domophoneId" to "$title $text $url")
            }
        }
    }

    fun resetCode(flatId: Int, domophoneId: Long) {
        viewModelScope.launchSimple {
            _isLoading.postValue(flatId to true)
            val currentList = _placesList.value.orEmpty().toMutableList()
            val res = addressInteractor.resetCode(flatId, domophoneId)
            val code = res.data.code
            placesList.value?.forEachIndexed { index, placeItemItem ->
                if (placeItemItem.flatId == flatId && placeItemItem.domophoneId.toLong() == domophoneId) {
                    val mod = currentList[index].copy(doorCode = code.toString())
                    currentList[index] = mod
                }
            }
            _placesList.postValue(currentList)
            _isLoading.postValue(flatId to false)
        }
    }

    fun activateLimit(contractId: Int) {
        viewModelScope.withProgress {
            try {
                val res = addressInteractor.activateLimit(contractId)
                if (res?.code == null) {
                    getContracts()
                }
            } catch (t: CommonErrorThrowable) {
                globalData.globalErrorsSink.value = Event(t.data)
                Timber.e("activateLimit throwable:$t")
            }
        }
    }

    fun setParentControl(clientId: Int) {
        viewModelScope.launchSimple {
            val currentList = _contractsList.value.orEmpty().toMutableList()
            val res = addressInteractor.setParentControl(clientId)
            if (res?.code == 204) {
                contractsList.value?.forEachIndexed { index, contractsResponseItem ->
                    if (contractsResponseItem.clientId == clientId) {
                        val mod =
                            currentList[index].copy(_parentControlStatus = if (contractsResponseItem.parentControlStatus == true) "f" else "t")
                        currentList[index] = mod
                    }
                }
            }
            _contractsList.postValue(currentList)
        }
    }

    private fun getPlaces() {
        viewModelScope.withProgress(progress = null) {
            val res = addressInteractor.getPlaces()
            val listPlaces = mutableListOf<PlaceItemItem>()
            res?.data?.let { list ->
                list.forEach {
                    listPlaces.add(
                        PlaceItemItem(
                            domophoneId = it.domophoneId,
                            address = it.address,
                            cctv = it.cctv,
                            name = it.name,
                            doorId = it.doorId,
                            flatId = it.flatId,
                            hasPlog = it.hasPlog,
                            houseId = it.houseId,
                            clientId = it.clientId,
                            doorCode = it.doorCode,
                            contractOwner = it.contractOwner,
                            flatNumber = it.flatNumber,
                            frsEnabled = it.frsEnabled,
                            icon = it.icon
                        )
                    )
                }
                _placesList.postValue(list)
                onFinishLoading()
            }
        }
    }

    fun getContracts() {
        viewModelScope.withProgress(progress = null) {
            try {
                addressInteractor.getContracts()?.let { res ->
                    _contractsList.postValue(res.data)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun getCameras() {
        viewModelScope.launchSimple {
            val cameraList = mutableListOf<CameraCctvItemItem>()
            val cctvList = mutableListOf<CameraCctvItemItem>()
            val res = addressInteractor.geCameras()
            res?.data?.forEach {
                cameraList.add(it)
            }

            if (cameraList.size == 0 || cameraList.size != 9) {
                try {
                    val resCctv = cctvInteractor.getCCTVOverview()
                    resCctv?.forEach {
                        cctvList.add(
                            CameraCctvItemItem(
                                id = it.id,
                                lat = it.latitude ?: 0.0,
                                lon = it.longitude ?: 0.0,
                                name = it.name,
                                token = it.token,
                                url = it.url,
                                isCityCctv = true,
                                fullUrl = it.fullUrl,
                                screenshotUrl = it.screenshotUrl,
                            )
                        )
                    }
                } catch (_: Exception) {
                }
                if (cameraList.size < 9) {
                    cctvList.forEach {
                        if (cameraList.size != 9) {
                            cameraList.add(it)
                        }
                    }
                } else if (cameraList.size > 9) {
                    cctvList.forEach {
                        if (cameraList.size % 2 == 0) {
                            cameraList.add(it)
                        }
                    }
                }
            }
            _camerasList.postValue(cameraList)
        }
    }

    fun refresh() {
        viewModelScope.withProgress(progress = _progress) {
            onStartLoading()
//            _camerasList.postValue(mutableListOf())
//            _placesList.postValue(mutableListOf())
            getDataList()
            getCameras()
            getPlaces()
        }
    }

    fun getDataList(forceRefresh: Boolean = false) {
        val noCache = nextListNoCache || forceRefresh
        nextListNoCache = false
        viewModelScope.withProgress(handleError = { true }, progress = null) {
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
                        res.data.mapIndexed { _, addressItem ->
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
                            if (hasYards && houseIdFlats[addressItem.houseId]?.size != 0) {
                                mutableList.add(
                                    EventLogModel().apply {
                                        caption = "События"
                                        counter = 0
                                        houseId = addressItem.houseId
                                        address = addressItem.address
                                        flats =
                                            houseIdFlats[addressItem.houseId]?.toList() ?: listOf()
                                    }
                                )
                            }
                            hasExpanded =
                                hasExpanded || expandedHouseId.contains(addressItem.houseId)
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
                    { !(it as ParentModel).hasYards },
                    { (it as ParentModel).addressTitle }
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
