package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sesameware.smartyard_oem.R

@Composable
fun GateAccessTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(id = R.string.tab_title_by_license_plate),
        stringResource(id = R.string.tab_title_by_phone_number)
    )

    TabRow(
        selectedTabIndex = selectedTabIndex,
        backgroundColor = Color.Transparent,
        contentColor = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(
                color = colorResource(R.color.light_background),
                shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner))
            )
            .padding(4.dp),
        divider =  @Composable {},
        indicator = @Composable { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                Box(
                    modifier = Modifier
                        .zIndex(-1f)
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .fillMaxHeight()
                        .background(
                            color = colorResource(R.color.shaded_background),
                            shape = RoundedCornerShape(
                                size = dimensionResource(R.dimen.tab_layout_corner_radius)
                            )
                        )
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) {
                        colorResource(R.color.accent)
                    } else {
                        colorResource(R.color.no_accent)
                    },
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.source_sans_pro_regular))
                )
            }
        }
    }
}

@Preview
@Composable
private fun GateAccessTabRowPreview() {
    var selectedTab by remember { mutableIntStateOf(0) }
    GateAccessTabRow(
        selectedTabIndex = selectedTab,
        onTabSelected = {
            @Suppress("AssignedValueIsNeverRead")
            if (selectedTab != it) selectedTab = it
        }
    )
}