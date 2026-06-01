package com.sesameware.smartyard_oem.ui.main.settings.trackedEvents.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.domain.model.response.Plog
import com.sesameware.domain.model.response.TrackedEvent
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.trackedEvents.TrackedEventsViewModel

@Composable
fun getEventName(eventType: Int): String {
    return when (eventType) {
        Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> stringResource(R.string.event_door_phone_call_unanswered)
        Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> stringResource(R.string.event_door_phone_call_answered)
        Plog.EVENT_OPEN_BY_KEY -> stringResource(R.string.event_open_by_key)
        Plog.EVENT_OPEN_FROM_APP -> stringResource(R.string.event_open_from_app)
        Plog.EVENT_OPEN_BY_FACE -> stringResource(R.string.event_open_by_face)
        Plog.EVENT_OPEN_BY_CODE -> stringResource(R.string.event_open_by_code)
        Plog.EVENT_OPEN_GATES_BY_CALL -> stringResource(R.string.event_open_gates_by_call)
        Plog.EVENT_OPEN_GATES_BY_VEHICLE -> stringResource(R.string.event_open_gates_by_vehicle)
        else -> stringResource(R.string.event_unknown)
    }
}

@Composable
fun TrackedEventsScreen(
    viewModel: TrackedEventsViewModel,
    address: String,
    onBackClick: () -> Unit,
    onFabClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var eventToDelete by remember { mutableStateOf<TrackedEvent?>(null) }
    val listState = rememberLazyListState()

    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    var isFabVisible by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to
                    listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val scrollDelta = offset - previousScrollOffset
            isFabVisible = when {
                index > previousIndex -> false
                index < previousIndex -> true
                scrollDelta > 0 -> false
                scrollDelta < 0 -> true
                else -> isFabVisible
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text(text = stringResource(id = R.string.tracked_events_delete_confirm_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.tracked_events_delete_confirm_message,
                        (getEventName(
                            eventToDelete?.eventType ?: 0
                        ) + if (eventToDelete?.eventDetail?.isNotEmpty() == true) ": " + eventToDelete?.eventDetail else "")
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        eventToDelete?.let { viewModel.untrackEvent(it.watcherId) }
                        eventToDelete = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.tracked_events_delete_confirm_positive), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text(text = stringResource(id = R.string.tracked_events_delete_confirm_negative), color = Color.Black)
                }
            }
        )
    }

    Image(
        painter = painterResource(id = R.drawable.background_top_narrow),
        contentDescription = null,
        alignment = Alignment.TopStart,
        modifier = Modifier
            .fillMaxWidth()
    )
    Column {
        Image(
            painter = painterResource(id = R.drawable.ic_back_arrow),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 24.dp, top = 44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onBackClick()
                }
        )
        Text(
            text = stringResource(id = R.string.tracked_events_title),
            color = colorResource(id = R.color.on_filled),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 40.dp, top = 20.dp)
        )
        Text(
            text = address,
            color = colorResource(id = R.color.on_filled),
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 40.dp, top = 4.dp, bottom = 28.dp)
        )

        Scaffold(
            backgroundColor = Color.Transparent,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = onFabClick,
                        modifier = Modifier.offset(y = (-12).dp),  // Adjust the offset as needed
                        backgroundColor = colorResource(R.color.brand),
                        contentColor = colorResource(id = R.color.on_filled)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = "Add"
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(12.dp, 12.dp))
                    .background(color = colorResource(id = R.color.shaded_background))
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null -> {
                        Text(
                            text = state.error ?: "",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = Color.Red
                        )
                    }
                    state.events.isEmpty() -> {
                        Text(
                            text = stringResource(id = R.string.tracked_events_empty),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp, bottom = 24.dp)
                        ) {
                            items(state.events) { event ->
                                TrackedEventItem(
                                    event = event,
                                    onDelete = { eventToDelete = event }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
