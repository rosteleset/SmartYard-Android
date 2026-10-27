package com.sesameware.smartyard_oem.ui.main.address.cctv_video.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.CCTVDataTree
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose.HeroScreen
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

    HeroScreen(
        title = stringResource(id = R.string.address_choose_camera_title),
        subtitle = address,
        onBackClick = onBackClick,
        contentPadding = PaddingValues(all = 16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        groupData.groupName?.let { groupName ->
            Text(
                text = groupName,
                color = colorResource(id = R.color.accent),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 28.dp, bottom = extraBottomPadding)
        ) {
            groupData.childGroups?.let { childGroups ->
                items(childGroups) { group ->
                    GroupItem(
                        group = group,
                        onClick = { onGroupItemClick(group) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }

            groupData.cameras?.let { cameras ->
                itemsIndexed(cameras) { index, cctvData ->
                    CameraItem(
                        camera = cctvData,
                        onClick = { onCameraItemClick(index, groupData) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    locale = "hy"
)
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
        url = "",
        _hasSound = true
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