package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessAddressViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessType
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue

// Модель для объединения данных в один список
sealed class GateAccessItemData {
    data class LicensePlate(val licensePlate: String) : GateAccessItemData()
    data class Phone(val phoneNumber: String) : GateAccessItemData()
    data class AddShortcut(@field:StringRes val id: Int) : GateAccessItemData()
}

@Composable
fun DetailGateAccessScreen(
    viewModel: AccessAddressViewModel,
    flatId: Int,
    address: String,
    clientId: String,
    initialTab: Int,
    initialPosition: Int,
    hasGates: Boolean,
    onBackClick: () -> Unit,
    onAddLicensePlateClick: () -> Unit,
    onAddPhoneClick: () -> Unit
) {
    val roommates = viewModel.roommate.observeAsState(emptyList()).value
    val licensePlates = viewModel.licensePlates.observeAsState(emptyList()).value
    val intercom = viewModel.intercom.observeAsState().value ?: return

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    val hasLprs = intercom.lprsDisabled == false

    val listItems = remember(selectedTab, roommates, licensePlates) {
        if (selectedTab == 0) {
            licensePlates.map { GateAccessItemData.LicensePlate(it.value) } +
                    GateAccessItemData.AddShortcut(R.string.barrier_add_vehicle)
        } else {
            roommates
                .filter { it.type == AccessType.GATE_BY_PHONE.roommateType }
                .map { GateAccessItemData.Phone(it.phone) } +
                    GateAccessItemData.AddShortcut(R.string.barrier_add_contact)
        }
    }

    HeroScreen(
        title = stringResource(R.string.title_gate_access),
        onBackClick = onBackClick,
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            AddressCard(
                address = address,
                modifier = Modifier
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp)
            )

            if (hasLprs && hasGates) {
                GateAccessTabRow(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                )
            }

            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 20.dp + bottomInset)
            val state = rememberLazyListState(initialFirstVisibleItemIndex = initialPosition)
            LazyColumn(
                state = state,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listItems) { item ->
                    when (item) {
                        is GateAccessItemData.LicensePlate -> DetailLicensePlateGateAccessItem(
                            licensePlate = item.licensePlate,
                            onDeleteClick = {
                                viewModel.removeLicensePlate(flatId,
                                    LicensePlateValue(item.licensePlate)
                                )
                            }
                        )
                        is GateAccessItemData.Phone -> DetailPhoneGateAccessItem(
                            phoneNumber = item.phoneNumber,
                            onSmsClick = {
                                viewModel.resend(flatId, item.phoneNumber)
                            },
                            onDeleteClick = {
                                viewModel.deleteRoommate(flatId, item.phoneNumber, clientId)
                            },
                        )
                        is GateAccessItemData.AddShortcut -> BrandedOutlinedButton(
                            title = stringResource(item.id),
                            onClick = if (selectedTab == 0) onAddLicensePlateClick else onAddPhoneClick
                        )
                    }
                }
            }
        }
    }
}