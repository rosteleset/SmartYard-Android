package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.domain.model.response.CCTVDataTree
import com.sesameware.domain.model.response.CCTVRepresentationType
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.composable.CCTVTreeScreen
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel

class CCTVTreeFragment : Fragment() {
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private var groupData: CCTVDataTree? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        groupData = arguments?.let { CCTVTreeFragmentArgs.fromBundle(it).cameraGroup }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            tag = "has_header"

            setContent {
                MaterialTheme {
                    CCTVTreeScreen(
                        groupData = groupData,
                        address = mCCTVViewModel.cctvModel.value?.address ?: "",
                        onBackClick = ::onBackClick,
                        onGroupItemClick = ::onGroupItemClick,
                        onCameraItemClick = ::onCameraItemClick,
                    )
                }
            }
        }
    }

    private fun onGroupItemClick(group: CCTVDataTree) {
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

    private fun onCameraItemClick(index: Int, parent: CCTVDataTree) {
        mCCTVViewModel.getCameraList(parent.cameras ?: listOf(), parent.type) {
            mCCTVViewModel.chooseCamera(index)
            val action =
                CCTVTreeFragmentDirections.actionCCTVTreeFragmentToCCTVDetailFragment()
            findNavController().navigate(action)
        }
    }

    private fun onBackClick() {
        findNavController().popBackStack()
    }
}
