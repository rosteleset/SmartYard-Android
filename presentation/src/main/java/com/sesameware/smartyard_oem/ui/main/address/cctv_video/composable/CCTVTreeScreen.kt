package com.sesameware.smartyard_oem.ui.main.address.cctv_video.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.CCTVDataTree
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.rememberBottomNavInsets

@Composable
fun CCTVTreeScreen(
    groupData: CCTVDataTree?,
    address: String,
    onBackClick: () -> Unit,
    onGroupItemClick: (CCTVDataTree) -> Unit,
    onCameraItemClick: (Int, CCTVDataTree) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groupData == null) return

    val extraBottomPadding = rememberBottomNavInsets()

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_top_narrow),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = "Back",
                modifier = Modifier
                    .padding(start = 24.dp, top = 44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackClick
                    )
            )
            Text(
                text = stringResource(id = R.string.address_choose_camera_title),
                color = colorResource(id = R.color.on_filled),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 40.dp, top = 20.dp)
            )
            Text(
                text = address,
                color = colorResource(id = R.color.on_filled),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp)
            )

            Column(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .weight(1f)
                    .clip(shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(color = colorResource(id = R.color.shaded_background))
            ) {
                groupData.groupName?.let { groupName ->
                    Text(
                        text = groupName,
                        color = colorResource(id = R.color.accent),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 8.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = extraBottomPadding + 16.dp)
                ) {
                    groupData.childGroups?.let { childGroups ->
                        items(childGroups) { group ->
                            GroupItem(
                                group = group,
                                onClick = { onGroupItemClick(group) }
                            )
                        }
                    }

                    groupData.cameras?.let { cameras ->
                        itemsIndexed(cameras) { index, cctvData ->
                            CameraItem(
                                camera = cctvData,
                                onClick = { onCameraItemClick(index, groupData) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun CCTVTreeScreenPreview() {
    MaterialTheme {
        CCTVTreeScreen(
            address = "ул. Московская, д. 10, кв. 42",
            groupData = PreviewCCTVData.mockTree,
            onBackClick = {},
            onGroupItemClick = {},
            onCameraItemClick = { _, _ -> }
        )
    }
}

private object PreviewCCTVData {
    val baseCamera = CCTVData(
        id = 100500,
        name = "Тестовая камера",
        latitude = null,
        longitude = null,
        token = "",
        url = ""
    )

    val mockCameras = listOf(
        baseCamera.copy(id = 100501, name = "Камера во дворе (въезд)"),
        baseCamera.copy(id = 100502, name = "Подъезд 1 (холл)"),
        baseCamera.copy(id = 100503, name = "Лифтовая площадка")
    )

    val baseNode = CCTVDataTree(
        groupId = 12340,
        groupName = "",
        _type = "list",
        childGroups = null,
        cameras = null
    )
    val mockChildGroups = listOf(
        baseNode.copy(
            groupId = 12341,
            groupName = "Первый этаж",
            cameras = listOf(baseCamera.copy(id = 100504, name = "Черный ход"))
        ),
        baseNode.copy(
            groupId = 12342,
            groupName = "Паркинг",
            cameras = listOf(
                baseCamera.copy(id = 100505, name = "Секция A1"),
                baseCamera.copy(id = 100506, name = "Секция B4")
            )
        )
    )

    val mockTree = baseNode.copy(
        groupId = 1233,
        groupName = "Основной вид",
        childGroups = mockChildGroups,
        cameras = mockCameras
    )
}