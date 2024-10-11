package ru.madbrains.smartyard.ui.main.address

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Space
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.text.TextAnnotation.Position
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.madbrains.domain.model.Services
import ru.madbrains.domain.model.response.Card
import ru.madbrains.domain.model.response.CardItem
import ru.madbrains.domain.model.response.ContractsResponseItem
import ru.madbrains.domain.model.response.DetailBalanceType
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.AddressComposeFragment.Companion.BROADCAST_CONFIRME_STATUS_PAY
import ru.madbrains.smartyard.ui.main.address.AddressComposeFragment.Companion.PAY_STATUS_CANCELED
import ru.madbrains.smartyard.ui.main.address.AddressComposeFragment.Companion.PAY_STATUS_ERROR
import ru.madbrains.smartyard.ui.main.address.AddressComposeFragment.Companion.PAY_STATUS_IN_PROCESS
import ru.madbrains.smartyard.ui.main.address.AddressComposeFragment.Companion.PAY_STATUS_SUCCESS
import ru.madbrains.smartyard.ui.main.address.auth.AuthFragment
import ru.madbrains.smartyard.ui.main.address.compose.CardFace
import ru.madbrains.smartyard.ui.main.address.compose.FlipCard
import ru.madbrains.smartyard.ui.main.address.compose.RotationAxis
import ru.madbrains.smartyard.ui.main.pay.tbank.TBankFragment
import ru.madbrains.smartyard.ui.main.pay.yocassa.YocassaPayFragment
import ru.madbrains.smartyard.ui.main.settings.accessAddress.AccessAddressFragment
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.TIME_ZONE
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateFormatRu
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dayWithMountFormatRu
import ru.tinkoff.acquiring.sdk.utils.getExtra
import timber.log.Timber
import kotlin.math.roundToInt


enum class PayStatus {
    SUCCESS, SUCCESS_PUSH, PROCESS, ERROR, ERROR_PUSH, CANCELED
}

enum class ReasonCardCanceled(val reasonName: String, val str: String) {
    FAILED_3D_SECURE("3d_secure_failed", "ошибка 3DS"),
    CALL_ISSUER("call_issuer", "call_issuer"),
    CARD_EXPIRED("card_expired", "срок действия карточки истек"),
    FRAUD_SUSPECTED("fraud_suspected", "подозрение в мошенничестве"),
    GENERAL_DECLINE("general_decline", "general_decline"),
    INSUFFICIENT_FUNDS("insufficient_funds", "нехватки средств"),
    INVALID_CARD_NUMBER("invalid_card_number", "недействительный номер карты"),
    INVALID_CSC("invalid_csc", "недействительный CSC"),
    ISSUER_UNAVAILABLE("issuer_unavailable", "эмитент недоступен"),
    PAYMENT_METHOD_LIMIT_EXCEEDED("payment_method_limit_exceeded", "платежный лимит превышен"),
    PAYMENT_METHOD_RESTRICTED("payment_method_restricted", "платежный метод с ограничениями")
}

enum class ScreenDevice {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}

enum class DragValue { Start, Center, End }

private const val MIN_PAY_SUM = 10.0

@Composable
private fun BroadCast(context: Context, onResponse: (PayStatus, Intent) -> Unit) {
    val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                PAY_STATUS_SUCCESS -> {
                    onResponse(PayStatus.SUCCESS, p1)
                }

                PAY_STATUS_IN_PROCESS -> {
                    onResponse(PayStatus.PROCESS, p1)
                }

                PAY_STATUS_CANCELED -> {
                    onResponse(PayStatus.CANCELED, p1)
                }

                PAY_STATUS_ERROR -> {
                    onResponse(PayStatus.ERROR, p1)
                }

                BROADCAST_CONFIRME_STATUS_PAY -> {
                    val status = p1.getStringExtra("status")
                    val contractTitle = p1.getStringExtra("contractTitle")
                    val payStatus = when (status) {
                        "2" -> PayStatus.SUCCESS_PUSH
                        "6" -> PayStatus.ERROR_PUSH
                        else -> PayStatus.ERROR_PUSH
                    }
                    onResponse(payStatus, p1)
                }
            }
        }
    }
    val intentFilter = IntentFilter().apply {
        addAction(PAY_STATUS_SUCCESS)
        addAction(PAY_STATUS_CANCELED)
        addAction(PAY_STATUS_IN_PROCESS)
        addAction(PAY_STATUS_ERROR)
        addAction(BROADCAST_CONFIRME_STATUS_PAY)
    }
    DisposableEffect(Unit) {
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetPayAnswer(payStatus: PayStatus, comment: String?, done: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(true)
    var textTitle = ""
    var textBody = ""
    var image = R.drawable.success_pay

    Timber.d("CANCELSTATUSSSSS SheetPayAnswer comment:$comment")
    when (payStatus) {
        PayStatus.SUCCESS, PayStatus.SUCCESS_PUSH -> {
            image = R.drawable.success_pay
            textTitle = "Оплачено"
            textBody = "Ваш баланс пополнен"
        }

        PayStatus.CANCELED -> {
            image = R.drawable.ic_error_pay
            textTitle = "Отмена"
            textBody = "Ваш платеж отменен по причине: ${comment ?: ""}"
        }

        PayStatus.ERROR, PayStatus.ERROR_PUSH -> {
            image = R.drawable.ic_error_pay
            textTitle = "Ошибка"
            textBody = "Ошибка пополнения"
        }

        PayStatus.PROCESS -> {
            image = R.drawable.acq_icon_warning
            textTitle = "Ожидание"
            textBody = "Ожидание платежа"
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colors.background,
        modifier = Modifier
            .fillMaxHeight(0.6f)
            .fillMaxWidth(),
        onDismissRequest = { done() }
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Image(painter = painterResource(id = image), contentDescription = null)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = textTitle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h4,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = textBody,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PayResultComponent(contractTitle: String, onProcess: (PayStatus) -> Boolean) {
    val context = LocalContext.current
    var payStatus by remember { mutableStateOf<PayStatus?>(null) }
    var is3DS by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf<String?>(null) }
    var intentTitle by remember { mutableStateOf<String?>(null) }
    var isStartModal by remember { mutableStateOf(false) }



    BroadCast(
        context = context,
        onResponse = { status, intent ->
            is3DS = onProcess(status)
            intentTitle = intent.getStringExtra("contractTitle")
            comment = intent.getStringExtra("comment")
            payStatus = status
        }
    )
    payStatus?.let { status ->
        when (status) {
            PayStatus.SUCCESS_PUSH, PayStatus.ERROR_PUSH -> {
                if (contractTitle == intentTitle) {
                    isStartModal = true
                }
            }

            else -> {
                if (!is3DS) {
                    isStartModal = true
                }
            }
        }
        if (isStartModal) {
            SheetPayAnswer(status, comment) {
                isStartModal = false
                payStatus = null
                intentTitle = null
            }
        }
    }
}


@Composable
private fun ContractEmptyComponent(onNavigate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.empty_contract_message)
        )
        Spacer(modifier = Modifier.height(15.dp))
        Button(
            enabled = true,
            onClick = { onNavigate() },
            modifier = Modifier.height(40.dp)
        ) {
            Text(text = "ПРИВЯЗАТЬ ДОГОВОР")
        }
    }
}


@Composable
private fun TooltipProvider(str: String) {
    Box {
        Popup(alignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.background(Color.Gray, RoundedCornerShape(4.dp))) {
                    Text(
                        text = str,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.overline
                    )
                }
                Icon(
                    painter = painterResource(id = android.R.drawable.arrow_down_float),
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun GlobalAlertDialog(
    text: String,
    onConfirmation: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        text = { Text(text = text) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = { onConfirmation() }) {
                Text(text = "Ок")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = "Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainAddress(
    mViewModel: AddressViewModel = viewModel(),
    onNavigate: (Fragment, Bundle?) -> Unit,
    startCalendar: (String) -> Unit,
) {
    val viewModel = mViewModel.contractsList.observeAsState()
    val detailBalance = mViewModel.balanceDetail.observeAsState()
    val cards = mViewModel.cards.observeAsState()
    val payState = mViewModel.payState.observeAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var refreshing by remember { mutableStateOf(false) }
    var isOpenDialog by remember { mutableStateOf(false) }

    fun refresh() = coroutineScope.launch {
        refreshing = true
        mViewModel.getContracts()
        delay(1500)
        refreshing = false
    }

    PayResultComponent(
        contractTitle = payState.value?.contractTitle ?: "",
        onProcess = { payStatus ->
            if (payStatus == PayStatus.PROCESS) {
                if (payState.value?.confirmationUrl != null) {
                    val bundle = Bundle()
                    bundle.apply {
                        putString("confirmationUrl", payState.value?.confirmationUrl)
                        putString("method", PayButtonType.SECURE.paymentSystem)
                    }
                    onNavigate(YocassaPayFragment(), bundle)
                }
            }
            (payStatus == PayStatus.PROCESS && payState.value?.confirmationUrl != null)
        }
    )

    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = ::refresh)

    if (viewModel.value.isNullOrEmpty()) {
        ContractEmptyComponent {
            onNavigate(AuthFragment(), null)
        }
    } else {
        Column(
            modifier = Modifier
                .pullRefresh(refreshState, true)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            viewModel.value?.let { list ->
                Pager(count = list.size) { index, pagerState ->
                    var state by remember { mutableStateOf(CardFace.Front) }
                    mViewModel.setCity(list[pagerState].cityTitle)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(all = 20.dp)
                            .height(500.dp)
                            .fillMaxWidth()
                    ) {
                        if (isOpenDialog) {
                            GlobalAlertDialog(
                                text = "Вы действительно хотите взять доверительный платеж?",
                                onConfirmation = {
                                    mViewModel.activateLimit(list[index].clientId)
                                    isOpenDialog = !isOpenDialog
                                }, onDismissRequest = {
                                    isOpenDialog = !isOpenDialog
                                })
                        }

                        FlipCard(
                            cardFace = state,
                            axis = RotationAxis.AxisY,
                            front = { modifier ->
                                CardComponent(modifier = modifier,
                                    cards = cards.value,
                                    item = list[index],
                                    onActivateLimit = { isOpenDialog = !isOpenDialog },
                                    clickSwitch = { mViewModel.setParentControl(list[index].clientId) },
                                    onClick = { state = state.next },
                                    getCards = { mViewModel.getCards(list[index].contractName) },
                                    deleteCard = { merchant, bindingId ->
                                        mViewModel.deleteCard(merchant, bindingId)
                                    },
                                    autoPayChange = { boolean, bindingId ->
                                        val merchant = list[index].merchant
                                        if (boolean) {
                                            mViewModel.setAutoPay(
                                                merchant = merchant,
                                                bindingId = bindingId
                                            )
                                        } else {
                                            mViewModel.removeAutoPay(
                                                merchant = merchant,
                                                bindingId = bindingId
                                            )
                                        }
                                        mViewModel.getCards(list[index].contractName)
                                    },
                                    onSettings = {
                                        val bundle = Bundle()
                                        val item = list[index]
                                        bundle.apply {
                                            putString("address", item.address)
                                            putString("clientId", item.clientId.toString())
                                            putInt("flatId", item.clientId)
                                            putBoolean("hasGates", item.hasGates)
                                            putBoolean("contractOwner", item.contractOwner)
                                        }
                                        onNavigate(AccessAddressFragment(), bundle)
                                    },
                                    pay = { data ->
                                        val bundle = Bundle()
                                        bundle.apply {
                                            putDouble("summa", data.summa)
                                            putString("notify", data.sendBill)
                                            putString("email", data.email)
                                            putString("merchant", data.merchant)
                                            putString("title", data.contractTitle)
                                            putString("method", data.method.paymentSystem)
                                            putString("bindingId", data.bindingId)
                                            putString("phone", mViewModel.userPhone)
                                            putBoolean("isAutoPay", data.isAutoPay)
                                            putBoolean("isSaveCard", data.isSaveCard)
                                        }
                                        when (data.method) {
                                            PayButtonType.SBP -> {
                                                onNavigate(TBankFragment(), bundle)
                                            }

                                            else -> {
                                                onNavigate(YocassaPayFragment(), bundle)
                                            }
                                        }
                                    }
                                )
                            },
                            back = { modifier ->
                                val flat = (list[pagerState].address).replace("квартира", "кв.")
                                val addressTitle = "г. ${list[pagerState].cityTitle}, "
                                val cityWithAddress = addressTitle + flat
                                HistoryPay(
                                    modifier = modifier,
                                    viewModel = mViewModel,
                                    addressTitle = cityWithAddress,
                                    startCalendar = {
                                        startCalendar(viewModel.value?.get(index)?.clientId.toString())
                                    },
                                    onClick = { state = state.next }
                                )
                                if (state == CardFace.Back) {
                                    detailBalance.let { pairBalance ->
                                        viewModel.value?.get(index)?.let {
                                            if (pairBalance.value?.first != it.clientId) {
                                                val calendar =
                                                    android.icu.util.Calendar.getInstance(
                                                        TIME_ZONE
                                                    )
                                                val formattedCurrentDate =
                                                    dateFormatRu.format(calendar.time)
                                                calendar.add(android.icu.util.Calendar.MONTH, -1)
                                                val formattedOneMountAgo =
                                                    dateFormatRu.format(calendar.time)

                                                mViewModel.getBalanceDetail(
                                                    it.clientId.toString(),
                                                    formattedCurrentDate,
                                                    formattedOneMountAgo
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    Box(contentAlignment = Alignment.TopCenter) {
        PullRefreshIndicator(refreshing, refreshState)
    }
}

@Composable
private fun HistoryPay(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel,
    addressTitle: String,
    onClick: () -> Unit,
    startCalendar: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val balanceDetail = viewModel.balanceDetail.observeAsState()
    val scrollState = rememberScrollState()
    var openAlertDialog by remember { mutableStateOf(false) }
    var openCalendar by remember { mutableStateOf(false) }
    var startDate by remember(key1 = balanceDetail) { mutableStateOf("") }
    var endDate by remember(key1 = balanceDetail) { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    val context = LocalContext.current

    balanceDetail.value?.let {
        val dateSplit = it.second.first.split(" ")
        to = dateSplit[0]
        from = dateSplit[1]
        dateFormatRu.parse(from)?.let { date ->
            startDate = dayWithMountFormatRu.format(date)
        }
        dateFormatRu.parse(to)?.let { date ->
            endDate = dayWithMountFormatRu.format(date)
        }
    }

    Column(modifier = modifier.padding(20.dp)) {
        Row(modifier = modifier.fillMaxWidth()) {
            Icon(
                modifier = Modifier.clickable { onClick() },
                painter = painterResource(id = R.drawable.ic_cancel_btn),
                tint = MaterialTheme.colors.primary,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(5.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Детализация счета по адресу", style = MaterialTheme.typography.subtitle1)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = addressTitle,
                style = MaterialTheme.typography.body1,
                fontSize = 14.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "История опериций".uppercase(), color = MaterialTheme.colors.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_send_btn),
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .clickable { openAlertDialog = true }
                    .background(
                        MaterialTheme.colors.background,
                        shape = RoundedCornerShape(5.dp)
                    ),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = if (startDate.isEmpty() && endDate.isEmpty()) "" else "С $startDate по $endDate")
                Text(
                    text = "Изменить",
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .clickable {
                            startCalendar()
                        },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.verticalScroll(scrollState)) {
                balanceDetail.value?.second?.second?.forEach {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            Icon(
                                painter = if (it.type == DetailBalanceType.PLUS)
                                    painterResource(id = R.drawable.wallet_pay_plus)
                                else
                                    painterResource(id = R.drawable.wallet_pay_minus),
                                tint = MaterialTheme.colors.primary,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                modifier = Modifier.width(180.dp),
                                text = it.title,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${it.summa}₽",
                                style = MaterialTheme.typography.subtitle1,
                                maxLines = 1
                            )
                            Text(
                                text = it.date,
                                style = TextStyle(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    openAlertDialog.let {
        if (it) {
            DialogItem(
                onDismissRequest = { openAlertDialog = false },
                composable = {
                    Column(
                        modifier = Modifier.height(400.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Отправка детализации",
                            modifier = Modifier.padding(16.dp),
                        )
                        Column {
                            Text(text = "за период:")
                            OutlinedTextField(
                                readOnly = true,
                                value = "$from - $to",
                                onValueChange = { value ->
                                },
                                trailingIcon = {
                                    Image(
                                        modifier = Modifier.clickable {
                                            startCalendar()
                                        },
                                        painter = painterResource(id = R.drawable.edit_icon),
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        Column {
                            Text(text = "на электронную почту:")
                            OutlinedTextField(
                                value = email,
                                onValueChange = { value ->
                                    email = value
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Email
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                enabled = email.isNotEmpty(),
                                onClick = {
                                    balanceDetail.value?.first?.let { id ->
                                        viewModel.sendBalanceDetail(context, id, from, to, email)
                                    }
                                    openAlertDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.background(MaterialTheme.colors.onPrimary)
                            ) {
                                Text(text = "Отправить")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DialogItem(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    composable: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Image(
                modifier = Modifier.clickable { onDismissRequest() },
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_close_dialog),
                contentDescription = null
            )
            Card(
                modifier = modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                composable()
            }
        }
    }
}

@Composable
private fun ParentControlDialog(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
    DialogItem(
        onDismissRequest = { onDismissRequest() },
        composable = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(15.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = stringResource(id = R.string.parent_control_title))
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = stringResource(id = R.string.parent_control_text))
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderPayComponent(sum: String?, valueSum: (String) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    var isErrorPay by remember { mutableStateOf(false) }
    var paySum by remember(key1 = sum) { mutableStateOf(sum) }
    val focusManager = LocalFocusManager.current

    if (isFocused) {
        LaunchedEffect(key1 = Unit) {
            focusManager.clearFocus()
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "Сумма:",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSecondary
        )
    }
    Spacer(modifier = Modifier.height(15.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            isError = isErrorPay,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            placeholder = { Text(text = "0") },
            shape = RoundedCornerShape(8.dp),
            value = paySum ?: "",
            onValueChange = { value ->
                val regVal = value.replace(
                    regex = Regex("[^0-9.]+|(?<![0-9])[.]|(?<![0-9])[.](?![0-9])"),
                    ""
                )
                if (regVal.count() < 8) {
                    paySum = regVal
                    paySum?.let { value ->
                        valueSum(value)
                    }
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = MaterialTheme.colors.background,
                errorBorderColor = MaterialTheme.colors.error,
                errorTextColor = MaterialTheme.colors.error,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.primary,
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                isFocused = !isFocused
                paySum?.let { value ->
                    isErrorPay =
                        value.isEmpty() || value.toDoubleOrNull()?.let { it < MIN_PAY_SUM } ?: true
                }
            }),
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = null
                )
            }
        )
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            Modifier
                .padding(top = 5.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val priceList = listOf("250", "300", "1000")
            priceList.forEach { summa ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            paySum = summa
                            paySum?.let { value ->
                                valueSum(value)
                            }
                        }
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colors.primaryVariant)
                        .size(width = 55.dp, height = 25.dp)
                ) {
                    Text(text = "$summa₽")
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }
}

@Composable
private fun PaymentAgreementText(limitUrl: String, serviceUrl: String) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = MaterialTheme.colors.secondaryVariant)) {
            append("Нажимая “Оплатить” вы соглашаетесь с ")
        }

        pushStringAnnotation(tag = "URL", annotation = limitUrl)
        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
            append("лимитами")
        }
        pop()

        withStyle(style = SpanStyle(color = MaterialTheme.colors.secondaryVariant)) {
            append(" и принимаете ")
        }
        pushStringAnnotation(tag = "URL", annotation = serviceUrl)
        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
            append("условия услуги")
        }
        pop()
    }

    ClickableText(
        style = MaterialTheme.typography.caption,
        text = annotatedString,
        onClick = { offset ->
            if (limitUrl.isNotEmpty() && serviceUrl.isNotEmpty()) {
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        startPdf(context = context, url = annotation.item)
                    }
            }
        }
    )
}

private fun startPdf(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (uri.toString().endsWith(".pdf", ignoreCase = true)) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        val pm = context.packageManager
        val resolvedApps =
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolvedApps.isNotEmpty()) {
            startActivity(context, intent, null)
        } else {
            startActivity(context, Intent(Intent.ACTION_VIEW, uri), null)
        }
    } else {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(context, intent, null)
    }
}

fun getNavigationBarHeight(context: Context): Float {
    val resources = context.resources
    val density = resources.displayMetrics.density

    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId) / density
    } else 0F
}

@Composable
fun screenCategory(): ScreenDevice {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    return when {
        screenHeightDp < 669 -> ScreenDevice.SMALL
        screenHeightDp in 669..900 -> ScreenDevice.MEDIUM
        screenHeightDp in 901..1200 -> ScreenDevice.LARGE
        else -> ScreenDevice.EXTRA_LARGE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetPay(
    cards: Pair<String, CardItem>?,
    contractName: String,
    isShowBottomSheet: (Boolean) -> Unit,
    autoPayChange: (Boolean, String) -> Unit,
    pay: (DataForPay) -> Unit,
    deleteCard: (String, String) -> Unit,
    start: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)
    val scrollState = rememberScrollState()
    var isAutoPay by remember { mutableStateOf(true) }
    var isSendBill by remember { mutableStateOf(false) }
    var paySum by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var isErrorPay by remember { mutableStateOf(false) }
    var isErrorMail by remember { mutableStateOf(false) }
    var payState by remember { mutableStateOf<PayButton?>(null) }
    var isSaveCard by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val screenDevice = screenCategory()
    var isEnablePayButton by remember { mutableStateOf(false) }
    Timber.d("PAYSTATEISAUTOPAY 1 isAutoPay:${isAutoPay}")

    start()
    cards.let {
        val summa = it?.second?.payAdvice
        paySum = summa?.let { value ->
            if (value == 0.0) null else summa.toString()
        }

        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = MaterialTheme.colors.surface,
            onDismissRequest = { isShowBottomSheet(false) },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 10.dp, start = 10.dp, bottom = 10.dp, top = 0.dp)
            ) {
                Text(
                    text = "Пополнить баланс",
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onSecondary
                )
                if (screenDevice != ScreenDevice.SMALL) {
                    Text(
                        text = "№${contractName}",
                        color = MaterialTheme.colors.onSecondary,
                        style = MaterialTheme.typography.h6,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                HeaderPayComponent(paySum) { value ->
                    paySum = value
                    isEnablePayButton = (value.toDoubleOrNull() ?: 0.0) >= MIN_PAY_SUM
                }

                HorizontalDivider(
                    color = MaterialTheme.colors.background,
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 15.dp)
                )
                if (screenDevice == ScreenDevice.SMALL) {
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))

                }
                Column(
                    Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    BodyPayComponent(
                        cards = cards?.second,
                        deleteCard = { merchant, bindingId ->
                            deleteCard(merchant, bindingId)
                        },
                        completePaymentMethod = { state ->
                            if (state.type != PayButtonType.NEW_CARD) {
                                Timber.d("PAYSTATEISAUTOPAY 2 isAutoPay:${isAutoPay} stateIsAutoPay:${state.isAutoPay}")
                                isAutoPay = state.isAutoPay == true
                                Timber.d("PAYSTATEISAUTOPAY 3 isAutoPay:${isAutoPay}")
                            }
                            payState = state
                        })
                }

                HorizontalDivider(
                    color = MaterialTheme.colors.background,
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 15.dp, bottom = 15.dp)
                )

                FooterPayComponent(
                    isSaveCard = isSaveCard,
                    payState = payState?.type,
                    isAutoPay = isAutoPay,
                    isSendBillToMail = isSendBill,
                    savedCard = {
                        isSaveCard = !isSaveCard
                        isAutoPay = isSaveCard
                    },
                    autoPay = { bool ->
                        isAutoPay = bool
                        if (payState?.type != PayButtonType.NEW_CARD) {
                            payState?.bindingId?.let { bindingId ->
                                autoPayChange(isAutoPay, bindingId)
                            }
                        }
                    },
                    isErrorMail = isErrorMail,
                    sendBill = {
                        isSendBill = !isSendBill
                    },
                    onEmailChange = { value ->
                        email = value
                    }
                )

                Spacer(modifier = Modifier.height(15.dp))

                PaymentAgreementText(
                    cards?.second?.documentLimit ?: "",
                    cards?.second?.documentServiceTerms ?: ""
                )

                Spacer(modifier = Modifier.height(15.dp))
                cards.let {
                    Button(
                        enabled = cards != null && isEnablePayButton,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(50.dp),
                        onClick = {
                            payState?.let { state ->
                                paySum?.let { value ->
                                    isErrorPay = value.isEmpty() || value.toDoubleOrNull()
                                        ?.let { it < MIN_PAY_SUM } ?: true
                                    isErrorMail = isSendBill && email.isEmpty()
                                    if (!isErrorPay && !isErrorMail) {
                                        pay(
                                            DataForPay(
                                                summa = value.toDouble(),
                                                method = state.type,
                                                isAutoPay = isAutoPay,
                                                isSaveCard = isSaveCard,
                                                sendBill = if (isSendBill) "email" else "push",
                                                bindingId = state.bindingId ?: "",
                                                merchant = it?.second?.merchant ?: "",
                                                contractTitle = contractName,
                                                email = email
                                            )
                                        )
                                    }
                                }
                            }
                        },
                    ) {
                        Text(text = "Оплатить")
                    }
                }
            }
            if (getNavigationBarHeight(context) != 0f) {
                Spacer(modifier = Modifier.height(getNavigationBarHeight(context).dp))
            }
        }
    }
}

@Composable
private fun SkeletonMethodsPay() {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .fillMaxWidth()
            .height(110.dp)
            .shimmer()
            .background(MaterialTheme.colors.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BodyPayComponent(
    cards: CardItem?,
    completePaymentMethod: (PayButton) -> Unit,
    deleteCard: (String, String) -> Unit,
) {
    var isShowOptionPaySheet by remember { mutableStateOf(false) }

    Text(
        text = "Выбор способа оплаты",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSecondary
    )
    Spacer(modifier = Modifier.height(10.dp))
    if (cards == null) {
        SkeletonMethodsPay()
    } else {
        val listCards by remember(key1 = cards) { mutableStateOf(createPayCardsList(cards.cards)) }
        var methodPay by remember(key1 = cards) { mutableStateOf(listCards[0]) }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .height(115.dp)
                .background(MaterialTheme.colors.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 5.dp, end = 5.dp)
                    .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(10.dp))
                    .height(50.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                methodPay.let {
                    completePaymentMethod(it)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                            painter = painterResource(id = it.image),
                            contentDescription = null
                        )
                        Text(text = it.title)
                    }
                    RadioButton(
                        selected = true,
                        colors = RadioButtonColors(
                            disabledSelectedColor = MaterialTheme.colors.onSurface,
                            disabledUnselectedColor = Color.Gray,
                            selectedColor = MaterialTheme.colors.primary,
                            unselectedColor = MaterialTheme.colors.onSecondary
                        ),
                        onClick = {}
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable {
                        isShowOptionPaySheet = !isShowOptionPaySheet
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    color = MaterialTheme.colors.secondaryVariant,
                    text = "Все способы оплаты",
                    modifier = Modifier.padding(start = 10.dp),
                )
                Image(
                    modifier = Modifier
                        .padding(end = 10.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null
                )
            }
            if (isShowOptionPaySheet) {
                SheetOptionsPay(
                    listCards = listCards,
                    showBottomSheet = {
                        isShowOptionPaySheet = it
                    },
                    complete = {
                        methodPay = it
                    },
                    deleteCard = { bindingId ->
                        deleteCard(cards.merchant, bindingId)
                    }
                )
            }
        }
    }

}

private fun createPayCardsList(cards: List<Card?>): List<PayButton> {
    val savedCards = mutableListOf<PayButton?>()
    val radioOptions = mutableListOf(
        //TODO Времено выключенно
//        PayButton(
//            image = R.drawable.sbp_1,
//            title = "Система быстрых платежей",
//            type = PayButtonType.SBP
//        ),
//        PayButton(
//            image = R.drawable.credit_pay,
//            title = "Доверительный платеж",
//            type = PayButtonType.CREDIT_PAY
//        ),
/////////////
        PayButton(
            image = R.drawable.newcard_pay,
            title = "Новая банковская карта",
            subTitle = "МИР, VISA, Mastercard",
            type = PayButtonType.NEW_CARD,
            isAutoPay = true
        ),
    )

    cards.forEach { card ->
        savedCards.add(
            when (card?.paymentSystem) {
                PayButtonType.VISA.paymentSystem -> {
                    PayButton(
                        image = R.drawable.visa_pay,
                        title = "Visa **${card.displayLabel}",
                        type = PayButtonType.VISA,
                        bindingId = card.bindingId,
                        isAutoPay = card.autoPay
                    )
                }

                PayButtonType.MASTER_CARD.paymentSystem -> {
                    PayButton(
                        image = R.drawable.mastercard_pay,
                        title = "MasterCard **${card.displayLabel}",
                        type = PayButtonType.MASTER_CARD,
                        bindingId = card.bindingId,
                        isAutoPay = card.autoPay
                    )
                }

                PayButtonType.MIR.paymentSystem -> {
                    PayButton(
                        image = R.drawable.mir_pay,
                        title = "MIR **${card.displayLabel}",
                        type = PayButtonType.MIR,
                        bindingId = card.bindingId,
                        isAutoPay = card.autoPay
                    )
                }

                else -> {
                    null
                }
            }
        )
    }

    savedCards.forEach {
        if (it != null) {
            radioOptions.add(0, it)
        }
    }
    return radioOptions
}

@Composable
private fun DeleteAction(padding: Dp) {
    Column(
        modifier = Modifier.padding(start = padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_delete),
            contentDescription = null,
            tint = MaterialTheme.colors.surface
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableAnchorsComponent(
    active: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
    startAction: @Composable (Dp) -> Unit,
    action: () -> Unit,
) {
    val density = LocalDensity.current
    val anchors = with(density) {
        DraggableAnchors {
            DragValue.Start at if (active) -60.dp.toPx() else 0f
            DragValue.Center at 0f
            DragValue.End at 0f
        }
    }
    val state = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Center,
            anchors = anchors,
            positionalThreshold = { totalDistance: Float -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween()
        )
    }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .background(MaterialTheme.colors.primary)
            .clickable {
                action()
            }
    ) {
        startAction(24.dp)
        Box(
            modifier = Modifier
                .anchoredDraggable(state, Orientation.Horizontal, reverseDirection = true)
                .offset {
                    IntOffset(
                        x = -state
                            .requireOffset()
                            .roundToInt(),
                        y = 0,
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun PayButtonElement(item: PayButton, isSelected: Boolean, onClick: (PayButton) -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxWidth()
            .padding(5.dp)
            .clickable { onClick(item) }
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colors.primary else Color.Transparent,
                RoundedCornerShape(10.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = item.image),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = item.title)
                item.subTitle?.let { subTitle ->
                    Text(text = subTitle)
                }
            }
        }
        RadioButton(
            selected = isSelected,
            colors = RadioButtonColors(
                disabledSelectedColor = MaterialTheme.colors.onSurface,
                disabledUnselectedColor = Color.Gray,
                selectedColor = MaterialTheme.colors.primary,
                unselectedColor = MaterialTheme.colors.onSecondary
            ),
            onClick = {
                onClick(item)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetOptionsPay(
    listCards: List<PayButton>,
    showBottomSheet: (Boolean) -> Unit,
    complete: (PayButton) -> Unit,
    deleteCard: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)
    var selectedOptions by remember { mutableStateOf(listCards[0]) }
    val list by remember(listCards) { mutableStateOf(listCards) }

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colors.surface,
        onDismissRequest = { showBottomSheet(false) },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = "Выбор способа оплаты",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSecondary
            )
            Spacer(modifier = Modifier.height(25.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background),
                horizontalAlignment = Alignment.Start
            ) {
                list.forEach {
                    if (it.bindingId.isNullOrEmpty()) {
                        PayButtonElement(item = it, isSelected = selectedOptions == it) { state ->
                            selectedOptions = state
                        }
                    } else {
                        DraggableAnchorsComponent(
                            action = {
                                deleteCard(it.bindingId)
                                val payBtn = list.first { l -> l.bindingId != it.bindingId }
                                selectedOptions = payBtn
                            },
                            content = {
                                PayButtonElement(
                                    item = it,
                                    isSelected = selectedOptions == it
                                ) { state ->
                                    selectedOptions = state
                                }
                            },
                            startAction = { dp ->
                                DeleteAction(dp)
                            }
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    complete(selectedOptions)
                    showBottomSheet(false)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
            ) {
                Text(text = "Готово")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FooterPayComponent(
    isSaveCard: Boolean,
    isAutoPay: Boolean,
    payState: PayButtonType?,
    isSendBillToMail: Boolean,
    isErrorMail: Boolean,
    savedCard: () -> Unit,
    autoPay: (Boolean) -> Unit,
    sendBill: () -> Unit,
    onEmailChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        LaunchedEffect(key1 = Unit) {
            focusManager.clearFocus()
        }
    }

    Column(Modifier.fillMaxWidth()) {
        if (payState == PayButtonType.NEW_CARD) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Сохранить карту")
                SwitchCustom(boolean = isSaveCard) {
                    savedCard()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Автоплатеж")
            SwitchCustom(boolean = isAutoPay, enabled = isSaveCard) {
                autoPay(it)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Выслать чек на почту")
            SwitchCustom(boolean = isSendBillToMail) {
                sendBill()
            }
        }
        OutlinedTextField(
            enabled = isSendBillToMail,
            placeholder = { Text(text = "Email", color = MaterialTheme.colors.secondaryVariant) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            isError = isErrorMail,
            shape = RoundedCornerShape(8.dp),
            value = email,
            onValueChange = { value ->
                email = value
                onEmailChange(value)
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledBorderColor = Color.Transparent,
                containerColor = MaterialTheme.colors.background,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.primary,
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    isFocused = !isFocused
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Go
            )
        )
    }
}

@Composable
private fun CardComponent(
    modifier: Modifier = Modifier,
    item: ContractsResponseItem,
    cards: Pair<String, CardItem>?,
    onClick: () -> Unit,
    clickSwitch: () -> Unit,
    getCards: () -> Unit,
    autoPayChange: (Boolean, String) -> Unit,
    pay: (DataForPay) -> Unit,
    onSettings: () -> Unit,
    deleteCard: (String, String) -> Unit,
    onActivateLimit: () -> Unit,
) {
    val data by remember(key1 = item) { mutableStateOf(item) }
    var isShowBottomSheet by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Договор:", color = MaterialTheme.colors.secondaryVariant)
            Icon(
                painter = painterResource(id = R.drawable.ic_setting_active),
                tint = MaterialTheme.colors.primary,
                contentDescription = null,
                modifier = Modifier.clickable {
                    onSettings()
                }
            )
        }
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = data.contractName,
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSecondary
            )
            Text(
                text = data.address.replace("квартира", " - "),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSecondary
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ваш баланс:", color = MaterialTheme.colors.secondaryVariant)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClick() }
            ) {
                Text(
                    text = "${data.balance}₽",
                    style = MaterialTheme.typography.h2,
                    modifier = Modifier.padding(end = 10.dp),
                    color = MaterialTheme.colors.onSecondary
                )

                Icon(
                    modifier = Modifier.size(25.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }

            if (data.limitStatus) {
                Text(
                    text = "подключен доверительный платеж на ${data.limitDays} дней",
                    style = MaterialTheme.typography.overline,
                    color = MaterialTheme.colors.primary
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { isShowBottomSheet = !isShowBottomSheet },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.background(MaterialTheme.colors.onPrimary)
            ) {
                Text(text = "Пополнить")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (data.limitAvailable && !data.limitStatus) {
                    Text(
                        text = "Взять доверительный платеж",
                        textDecoration = TextDecoration.Underline,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.secondaryVariant,
                        modifier = Modifier
                            .clickable { onActivateLimit() }
                    )
                }
            }

            if (isShowBottomSheet) {
                BottomSheetPay(
                    cards = cards,
                    start = { getCards() },
                    contractName = data.contractName,
                    isShowBottomSheet = { isShowBottomSheet = it },
                    autoPayChange = { boolean, bindingId -> autoPayChange(boolean, bindingId) },
                    pay = { data -> pay(data); isShowBottomSheet = !isShowBottomSheet },
                    deleteCard = { merchant, bindingId -> deleteCard(merchant, bindingId) }
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colors.background,
            thickness = 1.dp,
            modifier = Modifier
                .padding(top = 15.dp, bottom = 15.dp)
        )
        Column {
            ServicesRoundElement(services = data.services)
        }

        ParentControlComponent(
            parentControlStatus = data.parentControlStatus,
            parentControlEnable = data.parentControlEnable,
            onSwitch = {
                clickSwitch()
            })
    }
}

@Composable
private fun ParentControlComponent(
    parentControlStatus: Boolean?,
    parentControlEnable: Boolean,
    onSwitch: () -> Unit,
) {
    if (parentControlEnable) {
        var isHelp by remember { mutableStateOf(false) }
        var isActive by remember { mutableStateOf(false) }

        when {
            isActive && parentControlStatus == null -> isHelp = true
        }

        if (isHelp) {
            ParentControlDialog(
                onDismissRequest = {
                    isHelp = false
                    isActive = false
                },
                onConfirmation = {
                    isHelp = false
                    isActive = false
                }
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colors.background,
            thickness = 1.dp,
            modifier = Modifier
                .padding(top = 15.dp, bottom = 15.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.parent_control_title),
                    color = MaterialTheme.colors.secondaryVariant
                )
                Spacer(modifier = Modifier.width(5.dp))
                Image(
                    modifier = Modifier.clickable { isHelp = !isHelp },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_help),
                    contentDescription = null
                )
            }
            SwitchCustom(parentControlStatus ?: false) {
                isActive = it
                onSwitch()
            }
        }
    }
}

@Composable
private fun SwitchCustom(
    boolean: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        enabled = enabled,
        checked = boolean,
        onCheckedChange = {
            onCheckedChange(!boolean)
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colors.background,
            checkedTrackColor = MaterialTheme.colors.primary,
            uncheckedThumbColor = MaterialTheme.colors.background,
            uncheckedTrackColor = MaterialTheme.colors.onBackground,
            checkedTrackAlpha = 1.0f,
            uncheckedTrackAlpha = 1.0f
        ),
    )
}


@Composable
private fun ServicesRoundElement(services: List<String>) {
    val iconsList = listOf(
        Services.Cctv,
        Services.Domophone,
        Services.Internet,
        Services.Iptv
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconsList.forEach { itemService ->
            val icon = when (itemService.value) {
                Services.Domophone.value -> R.drawable.intercom_label
                Services.Cctv.value -> R.drawable.cctv_label
                Services.Internet.value -> R.drawable.internet_lebel
                Services.Iptv.value -> R.drawable.iptv_label
                else -> R.drawable.ic_map_icon_bg
            }
            val isFind = services.find { service -> service == itemService.value } != null

            IconToolTip(stringRes = itemService.nameId, iconRes = icon, isActive = isFind)
        }
    }
}

@Composable
private fun IconToolTip(stringRes: Int, iconRes: Int, isActive: Boolean) {
    var isShowTooltip by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        if (isShowTooltip) {
            Box(
                modifier = Modifier
                    .zIndex(55f)
                    .offset(0.dp, (-35).dp)
            ) {
                TooltipProvider(stringResource(id = stringRes))
            }
            LaunchedEffect(key1 = Unit) {
                delay(2000)
                isShowTooltip = false
            }
        }
        Box {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            isShowTooltip = false
                        }
                    }
                    .focusable()
                    .clickable(
                        onClick = { isShowTooltip = !isShowTooltip; focusRequester.requestFocus() },
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = false, radius = 20.dp)
                    )
                    .clip(CircleShape)
                    .size(40.dp),
                tint = if (isActive) MaterialTheme.colors.primary else MaterialTheme.colors.secondaryVariant
            )
        }

    }
    Spacer(modifier = Modifier.width(3.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Pager(count: Int, composable: @Composable (Int, Int) -> Unit) {
    val pageCount = count * 400
    val pagerState = rememberPagerState(
        initialPage = if (count > 1) pageCount / 2 else 0,
        pageCount = { if (count > 1) pageCount else count })
    val coroutineScope = rememberCoroutineScope()

    Column {
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fill,
            pageSpacing = 4.dp,
        ) { page ->
            if (count > 1) {
                composable(page % count, pagerState.currentPage % count)
            } else {
                composable(0, pagerState.currentPage)
            }
        }
        if (count > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, top = 4.dp)
            ) {
                repeat(count) { iteration ->
                    val color =
                        if (pagerState.currentPage % count == iteration) Color.Red else Color.LightGray
                    Box(modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(iteration)
                            }
                        }
                    )
                }
            }
        }
    }
}