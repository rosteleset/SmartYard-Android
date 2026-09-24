package com.sesameware.smartyard_oem.ui.main.settings.faceSettings.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.FaceData
import com.sesameware.domain.model.response.GroupData
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose.BrandedOutlinedButton
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.FaceSettingsViewModel

@Composable
fun FaceSettingsScreen(
    viewModel: FaceSettingsViewModel,
    flatId: Int,
    canAddFace: Boolean,
    bottomNavHeight: Int = 0,
    onBackClick: () -> Unit,
    onAddFaceClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val hasFaceGroups = DataModule.providerConfig.hasFaceGroups
    val hasFaceClustering = DataModule.providerConfig.hasFaceClustering
    val hasEventsTracking = DataModule.providerConfig.hasEventsTracking

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<GroupData?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupData?>(null) }
    var faceToDelete by remember { mutableStateOf<FaceData?>(null) }
    var showClusteringDialog by remember { mutableStateOf(false) }
    var showAttachToGroupDialog by remember { mutableStateOf<FaceData?>(null) }

    val filteredFaces = remember(state.faces, state.selectedGroupId) {
        when (state.selectedGroupId) {
            null -> state.faces
            -1 -> state.faces.filter { it.groupId == null || it.groupId == 0 }
            else -> state.faces.filter { it.groupId == state.selectedGroupId }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.listFaces(flatId)
        if (hasFaceGroups) {
            viewModel.listGroups(flatId)
        }
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
            text = stringResource(id = R.string.registered_faces),
            color = colorResource(id = R.color.on_top_background),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 40.dp, top = 20.dp, bottom = 20.dp)
        )

        val density = LocalDensity.current
        val bottomPadding = remember(bottomNavHeight) {
            density.run { bottomNavHeight.toDp() }
        }

        Scaffold(
            backgroundColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(24.dp, 24.dp))
                    .background(color = colorResource(id = R.color.shaded_background))
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = bottomPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        if (hasFaceGroups) {
                            FaceGroupsList(
                                hasEventsTracking = hasEventsTracking,
                                groups = state.groups,
                                selectedGroupId = state.selectedGroupId,
                                onGroupSelect = { viewModel.selectGroup(flatId, it) },
                                onAddGroup = { showAddGroupDialog = true },
                                onEditGroup = { groupToEdit = it },
                                onDeleteGroup = { groupToDelete = it },
                                modifier = Modifier.width(200.dp).fillMaxHeight()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = if (hasFaceGroups) 0.dp else 24.dp, topEnd = 24.dp))
                        ) {
                            if (state.isLoading) {
                                //CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else  {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                ) {
                                    if (canAddFace) {
                                        AddFaceItem(onClick = onAddFaceClick)
                                    }
                                    if (filteredFaces.isEmpty() && state.selectedGroupId != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.face_group_faces_empty),
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.wrapContentWidth()
                                        ) {
                                            items(filteredFaces) { face ->
                                                val groupName = state.groups.find { it.groupId == face.groupId }?.groupName
                                                FaceItem(
                                                    face = face,
                                                    groupName = groupName,
                                                    onDeleteClick = { faceToDelete = face },
                                                    onAttachClick = { showAttachToGroupDialog = face },
                                                    onDetachClick = {
                                                        face.groupId?.let { gid ->
                                                            viewModel.detachFaceFromGroup(flatId, face.faceId.toInt(), gid)
                                                        }
                                                    },
                                                    hasFaceGroups = hasFaceGroups,
                                                    selectedGroupId = state.selectedGroupId
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (hasFaceClustering && hasFaceGroups && state.selectedGroupId == -1) {
                        BrandedOutlinedButton(
                            title = stringResource(id = R.string.face_group_clustering),
                            onClick = { showClusteringDialog = true },
                            modifier = Modifier.padding(16.dp),
                            enabled = filteredFaces.isNotEmpty()
                        )
                    }

                    Text(
                        text = stringResource(id = if (canAddFace) R.string.face_settings_comments_has_plog else R.string.face_settings_comments_no_plog, stringResource(id = R.string.add_face)),
                        color = colorResource(id = R.color.accent),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddGroupDialog) {
        InputDialog(
            title = stringResource(id = R.string.face_group_add),
            hint = stringResource(id = R.string.event_log_enter_group_name),
            isTracked = if (hasEventsTracking) false else null,
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { groupName, doTracking ->
                viewModel.addGroup(flatId, groupName, doTracking)
                showAddGroupDialog = false
            }
        )
    }

    if (groupToEdit != null) {
        InputDialog(
            title = stringResource(id = R.string.face_group_edit),
            hint = stringResource(id = R.string.event_log_enter_group_name),
            isTracked = if (hasEventsTracking) groupToEdit?.watcherId != null else false,
            initialValue = groupToEdit?.groupName ?: "",
            onDismiss = { groupToEdit = null },
            onConfirm = { groupName, doTracking ->
                groupToEdit?.let { group ->
                    viewModel.updateGroup(flatId, group.groupId, groupName, groupToEdit?.watcherId, doTracking)
                }
                groupToEdit = null
            }
        )
    }

    if (groupToDelete != null) {
        DeleteDialog(
            title = stringResource(id = R.string.face_group_delete),
            body = stringResource(id = R.string.face_group_delete_confirm, groupToDelete?.groupName ?: ""),
            onConfirm = {
                groupToDelete?.let { viewModel.deleteGroup(flatId, it.groupId) }
                groupToDelete = null
            },
            onDismiss = {
                groupToDelete = null
            }
        )
    }

    if (faceToDelete != null) {
        DeleteDialog(
            title = stringResource(id = R.string.title_delete_face),
            bodyImage = faceToDelete?.faceImage ?: "",
            onConfirm = {
                faceToDelete?.let { viewModel.removeFace(flatId, it.faceId.toInt()) }
                faceToDelete = null
            },
            onDismiss = { faceToDelete = null },
        )
    }

    if (showClusteringDialog) {
        InputDialog(
            title = stringResource(id = R.string.face_group_clustering),
            hint = stringResource(id = R.string.face_group_clustering_prefix),
            onDismiss = { showClusteringDialog = false },
            onConfirm = { prefix, _ ->
                viewModel.clusterFaces(flatId, prefix)
                showClusteringDialog = false
            }
        )
    }

    if (showAttachToGroupDialog != null) {
        GroupSelectionDialog(
            groups = state.groups,
            onDismiss = { showAttachToGroupDialog = null },
            onSelect = { group ->
                showAttachToGroupDialog?.let { face ->
                    viewModel.attachFaceToGroup(flatId, face.faceId.toInt(), group.groupId)
                }
                showAttachToGroupDialog = null
            }
        )
    }
}

@Composable
fun FaceGroupsList(
    hasEventsTracking: Boolean,
    groups: List<GroupData>,
    selectedGroupId: Int?,
    onGroupSelect: (Int?) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (GroupData) -> Unit,
    onDeleteGroup: (GroupData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(colorResource(id = R.color.shaded_background))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable(onClick = onAddGroup),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_group_add),
                contentDescription = "Add group",
                modifier = Modifier.size(32.dp),
                tint = colorResource(id = R.color.brand)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.face_group_add),
                color = colorResource(id = R.color.brand),
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                GroupListItem(
                    text = stringResource(id = R.string.face_group_all_faces),
                    isSelected = selectedGroupId == null,
                    isTracked = false,
                    onClick = { onGroupSelect(null) }
                )
            }
            item {
                GroupListItem(
                    text = stringResource(id = R.string.face_group_ungrouped_faces),
                    isSelected = selectedGroupId == -1,
                    isTracked = false,
                    onClick = { onGroupSelect(-1) }
                )
            }
            items(groups) { group ->
                GroupListItem(
                    text = group.groupName,
                    isSelected = selectedGroupId == group.groupId,
                    isTracked = hasEventsTracking && (group.watcherId != null),
                    onClick = { onGroupSelect(group.groupId) },
                    onEdit = { onEditGroup(group) },
                    onDelete = { onDeleteGroup(group) }
                )
            }
        }
    }
}

@Composable
fun GroupListItem(
    text: String,
    isSelected: Boolean,
    isTracked: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        color = if (isSelected) colorResource(id = R.color.light_background) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        shape = if (isSelected) RoundedCornerShape(8.dp) else RectangleShape,
        elevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            if (isTracked) {
                Icon(
                    painterResource(id = R.drawable.ic_eye_tracking),
                    contentDescription = "Tracked",
                    tint = colorResource(R.color.brand),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colorResource(id = R.color.brand) else Color.Black,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (onEdit != null && isSelected) {
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (onDelete != null && isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FaceItem(
    face: FaceData,
    groupName: String?,
    onDeleteClick: () -> Unit,
    onAttachClick: () -> Unit,
    onDetachClick: () -> Unit,
    hasFaceGroups: Boolean,
    selectedGroupId: Int?
) {
    Row {
        Surface(
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp,
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray)
                    ) {
                        GlideImage(
                            model = face.faceImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (hasFaceGroups) {
                            if (face.groupId == null || face.groupId == 0) {
                                IconButton(onClick = onAttachClick, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_add_link),
                                        contentDescription = "Attach",
                                        tint = colorResource(id = R.color.brand)
                                    )
                                }
                            } else {
                                IconButton(onClick = onDetachClick, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_link_off),
                                        contentDescription = "Detach",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_remove_face),
                                contentDescription = "Delete",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (hasFaceGroups && face.groupId != null && selectedGroupId == null) {
                    Text(
                        modifier = Modifier
                            .widthIn(max = 112.dp)
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        text = groupName ?: "",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AddFaceItem(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorResource(id = R.color.shaded_background),
        modifier = Modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_face),
                contentDescription = "Add face",
                modifier = Modifier.size(32.dp),
                tint = colorResource(id = R.color.brand)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.add_face),
                color = colorResource(id = R.color.brand),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InputDialog(
    title: String,
    hint: String,
    initialValue: String = "",
    isTracked: Boolean? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    var checked by remember { mutableStateOf(isTracked ?: false) }
    val isValid = text.isNotBlank()
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.dialog_large_corner_radius)),
            color = colorResource(id = R.color.dialog_background)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    color = colorResource(id = R.color.accent),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = colorResource(R.color.brand),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = colorResource(R.color.brand),
                        textColor = colorResource(R.color.accent),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(text = hint)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isValid
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isTracked != null) {
                    Row(
                        modifier = Modifier
                            .clickable {checked = !checked},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.event_log_track_group))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                FloodedGreenButton(
                    title = stringResource(id = android.R.string.ok),
                    onClick = { onConfirm(text, isTracked != null && checked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isValid,
                )

                Spacer(modifier = Modifier.height(20.dp))

                TransparentButton(
                    title = stringResource(id = R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DeleteDialog(
    title: String = "",
    body: String = "",
    bodyImage: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(id = R.string.text_delete),
    cancelText: String = stringResource(id = R.string.text_dismiss)
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.dialog_large_corner_radius)),
            color = colorResource(id = R.color.dialog_background)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.accent),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (body.isNotBlank()) {
                    Text(
                        text = body,
                        fontSize = 16.sp,
                        color = colorResource(id = R.color.accent),
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                } else if (bodyImage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.CenterHorizontally)
                            .background(Color.LightGray)
                    ) {
                        GlideImage(
                            model = bodyImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                FloodedRedButton(
                    title = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                TransparentButton(
                    title = cancelText,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun GroupSelectionDialog(
    groups: List<GroupData>,
    onDismiss: () -> Unit,
    onSelect: (GroupData) -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxListHeight = configuration.screenHeightDp.dp * 0.6f

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                dimensionResource(R.dimen.dialog_large_corner_radius)
            ),
            color = colorResource(id = R.color.dialog_background)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.face_group_attach),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.accent),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (groups.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.face_group_empty)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxListHeight)
                    ) {
                        items(groups) { group ->
                            Text(
                                text = group.groupName,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(group)
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                FloodedBrandButton(
                    title = stringResource(id = R.string.text_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
