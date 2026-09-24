package com.sesameware.smartyard_oem.ui.main.settings.faceSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.BottomNavProvider
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogViewModel
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.compose.FaceSettingsScreen
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class FaceSettingsFragment : Fragment() {

    private val mViewModel by sharedViewModel<FaceSettingsViewModel>()
    private val mEventLogVM by sharedViewModel<EventLogViewModel>()
    private var flatId = 0
    private var address = ""
    private var canAddFace = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bottomNavHeight = (requireActivity() as? BottomNavProvider)?.getBottomNavHeight() ?: 0
        arguments?.let {
            flatId = FaceSettingsFragmentArgs.fromBundle(it).flatId
            address = FaceSettingsFragmentArgs.fromBundle(it).address
            canAddFace = FaceSettingsFragmentArgs.fromBundle(it).canAddFace
        }

        return ComposeView(requireContext()).apply {
            setContent {
                FaceSettingsScreen(
                    viewModel = mViewModel,
                    flatId = flatId,
                    bottomNavHeight = bottomNavHeight,
                    canAddFace = canAddFace,
                    onBackClick = { findNavController().popBackStack() },
                    onAddFaceClick = {
                        mEventLogVM.address = address
                        mEventLogVM.flatsAll = listOf(Flat(flatId, "", true))
                        mEventLogVM.filterFlat = null
                        mEventLogVM.lastLoadedDayFilterIndex.value = -1
                        mEventLogVM.currentEventItem = null

                        (requireActivity() as MainActivity).binding.bottomNav.selectedItemId =
                            R.id.address
                        val navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.addressFragment, false)
                            .build()
                        findNavController().navigate(R.id.eventLogFragment, null, navOptions)
                    }
                )
            }
        }
    }
}
