package com.sesameware.smartyard_oem.ui.main.settings.trackedEvents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogViewModel
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.EventLogModel
import com.sesameware.smartyard_oem.ui.main.settings.trackedEvents.compose.TrackedEventsScreen
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class TrackedEventsFragment : Fragment() {

    private val args: TrackedEventsFragmentArgs by navArgs()
    private val mViewModel: TrackedEventsViewModel by viewModel()
    private val mEventLogVM by sharedViewModel<EventLogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.loadEvents(args.flatId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    TrackedEventsScreen(
                        viewModel = mViewModel,
                        address = args.address,
                        onBackClick = { findNavController().popBackStack() },
                        onFabClick = {
                            val eventLogModel = EventLogModel().apply {
                                address = args.address
                                flats = emptyList() // Apartments at the address will be loaded into EventLogFragment
                            }
                            val bundle = Bundle().apply {
                                putParcelable("eventLogModel", eventLogModel)
                            }
                            mEventLogVM.address = args.address
                            mEventLogVM.flatsAll = listOf(Flat(args.flatId, "", true))
                            mEventLogVM.filterFlat = null
                            mEventLogVM.lastLoadedDayFilterIndex.value = -1
                            mEventLogVM.currentEventItem = null
                            (requireActivity() as MainActivity).binding.bottomNav.selectedItemId = R.id.address
                            val navOptions = NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setPopUpTo(R.id.addressFragment, false)
                                .build()
                            findNavController().navigate(R.id.eventLogFragment, bundle, navOptions)
                        }
                    )
                }
            }
        }
    }
}
