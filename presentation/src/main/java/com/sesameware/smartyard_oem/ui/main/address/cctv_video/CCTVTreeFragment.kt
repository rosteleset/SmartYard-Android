package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.fragment.findNavController
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.CCTVDataTree
import com.sesameware.domain.model.response.CCTVRepresentationType
import com.sesameware.smartyard_oem.R
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel

class CCTVTreeFragment : Fragment() {
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private var groupData: CCTVDataTree? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireNotNull(arguments).let {
            groupData = CCTVTreeFragmentArgs.fromBundle(it).cameraGroup
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    var listHeight: Dp by remember { mutableStateOf(0.dp) }
                    Image(
                        painter = painterResource(id = R.drawable.background_1),
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
                                    interactionSource = MutableInteractionSource(),
                                    indication = null
                                ) {
                                    findNavController().popBackStack()
                                }
                            )
                        Text(
                            text = stringResource(id = R.string.address_choose_camera_title),
                            color = colorResource(id = R.color.white),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 40.dp, top = 20.dp)
                        )
                        Text(
                            text = mCCTVViewModel.cctvModel.value?.address ?: "",
                            color = colorResource(id = R.color.white),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(start = 40.dp, top = 4.dp)
                        )

                        Column(
                            modifier = Modifier
                                .padding(top = 28.dp)
                                .clip(shape = RoundedCornerShape(12.dp, 12.dp))
                                .background(color = colorResource(id = R.color.white_200))
                        ) {
                            groupData?.groupName?.let { groupName ->
                                Text(
                                    text = groupName,
                                    color = colorResource(id = R.color.black_200),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(start = 16.dp, top = 28.dp)
                                )
                            }

                            //this Box with calculated height is a workaround because for some reason the last item is partially shown in LazyColumn
                            //everything begins working correctly only if LazyColumn's height is set with specific number
                            Box(modifier = Modifier
                                .padding(top = 24.dp, bottom = 48.dp)
                                .fillMaxSize()
                                .onGloballyPositioned {
                                    val screenPixelDensity =
                                        context.resources.displayMetrics.density
                                    val dpVal = it.size.height / screenPixelDensity
                                    listHeight = dpVal.dp
                                }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.height(listHeight)
                                ) {
                                    groupData?.childGroups?.let {childGroups ->
                                        items(childGroups) {
                                            GroupItem(group = it)
                                        }
                                    }

                                    groupData?.cameras?.let { cameras ->
                                        cameras.forEachIndexed { index, cctvData ->
                                            item {
                                                CameraItem(parent = groupData!!, index = index, camera = cctvData)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GroupItem(group: CCTVDataTree) {
        Card(
            shape = RoundedCornerShape(12.dp),
            backgroundColor = colorResource(id = R.color.white_0),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .clickable {
                        mCCTVViewModel.chosenIndex.value = null
                        mCCTVViewModel.chosenCamera.value = null
                        mCCTVViewModel.chooseGroup(group.groupId, group.groupName)
                        mCCTVViewModel.getCameraList(group.cameras ?: listOf(), group.type) {
                            val action = when (group.type) {
                                CCTVRepresentationType.LIST -> CCTVTreeFragmentDirections.actionCCTVTreeFragmentSelf(
                                    group
                                )
                                else -> CCTVTreeFragmentDirections.actionCCTVTreeFragmentToCCTVMapFragment()
                            }
                            findNavController().navigate(action)
                        }
                    }
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 24.dp, top = 16.dp, bottom = 16.dp),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.black_200),
                    text = group.groupName ?: "",
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 24.dp)
                )
            }
        }
    }

    @Composable
    private fun CameraItem(parent: CCTVDataTree, index: Int, camera: CCTVData) {
        Card(
            shape = RoundedCornerShape(12.dp),
            backgroundColor = colorResource(id = R.color.white_0),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .clickable {
                        mCCTVViewModel.getCameraList(parent.cameras ?: listOf(), parent.type) {
                            mCCTVViewModel.chooseCamera(index)
                            val action =
                                CCTVTreeFragmentDirections.actionCCTVTreeFragmentToCCTVDetailFragment()
                            findNavController().navigate(action)
                        }
                    }
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 24.dp, top = 16.dp, bottom = 16.dp),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.black_200),
                    text = camera.name,
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_video_camera),
                    contentDescription = null,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 16.dp, end = 24.dp)
                        .size(24.dp),
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.grey_100))
                )
            }
        }
    }
}
