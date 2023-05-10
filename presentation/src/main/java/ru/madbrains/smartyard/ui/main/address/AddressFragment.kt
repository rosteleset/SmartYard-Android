package ru.madbrains.smartyard.ui.main.address

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAddressBinding
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.adapters.ParentListAdapter
import ru.madbrains.smartyard.ui.main.address.adapters.ParentListAdapterSetting
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogViewModel
import ru.madbrains.smartyard.ui.main.address.guestAccessDialog.GuestAccessDialogFragment
import ru.madbrains.smartyard.ui.main.address.models.ParentModel
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber

class AddressFragment : Fragment(), GuestAccessDialogFragment.OnGuestAccessListener {
    private var _binding: FragmentAddressBinding? = null
    private val binding get() = _binding!!
    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private val mViewModel by sharedViewModel<AddressViewModel>()
    private val mEventLog by sharedViewModel<EventLogViewModel>()
    lateinit var recyclerView: RecyclerView
    private var adapter: ParentListAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecycler()
        binding.floatingActionButton.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_addressFragment_to_authFragment)
        }
        binding.swipeContainer.setOnRefreshListener {
            mViewModel.getDataList(true)
        }
        mainActivityViewModel.navigationToAddressAuthFragmentAction.observe(
            viewLifecycleOwner,
            EventObserver {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_addressFragment_to_authFragment)
            }
        )
        mainActivityViewModel.reloadToAddress.observe(
            viewLifecycleOwner,
            EventObserver {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_global_addressFragment2)
            }
        )
//        var test = true

        mViewModel.dataList.observe(
            viewLifecycleOwner
        ) {
            // val items = ParentDataFactory.getParents(5) + it
            adapter?.items = it
            adapter?.notifyDataSetChanged()
            binding.swipeContainer.isRefreshing = false
            updateAllWidget(requireContext())

//            if (test) {
//                mViewModel.getDataList(true)
//                test = false
//            }//TODO метод для обновления адресов при добавлении нового адреса

            if (binding.floatingActionButton.visibility != View.VISIBLE) {
                binding.floatingActionButton.show()
            }
        }
        mViewModel.progress.observe(
            viewLifecycleOwner
        ) {
            if (!binding.swipeContainer.isRefreshing)
                binding.progressBarAddress.isVisible = it
            binding.swipeContainer.isRefreshing = false
        }
        mViewModel.navigationToAuth.observe(
            viewLifecycleOwner,
            EventObserver {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_addressFragment_to_authFragment)
            }
        )
    }

    private fun initRecycler() {
        adapter = ParentListAdapter(
            ParentListAdapterSetting(
                context = requireContext(),
                clickOpen = { domophoneId, doorId ->
                    mViewModel.openDoor(domophoneId, doorId)
                },
                clickPos = { position, isExpanded ->
                    if (isExpanded) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val smoothScroller: SmoothScroller =
                            object : LinearSmoothScroller(context) {
                                override fun getVerticalSnapPreference(): Int {
                                    return SNAP_TO_START
                                }
                            }
                        smoothScroller.targetPosition = position
                        layoutManager.startSmoothScroll(smoothScroller)
                    }
                    (adapter?.items?.get(position) as? ParentModel)?.let { parent ->
                        if (isExpanded) {
                            mViewModel.expandedHouseId.add(parent.houseId)
                        } else {
                            mViewModel.expandedHouseId.remove(parent.houseId)
                        }
                        parent.isExpanded = isExpanded
                    }
                },
                clickItemIssue = {
                    if (it.courier) {
                        val action =
                            AddressFragmentDirections.actionAddressFragmentToWorkSoonOfficeFragment(
                                it
                            )
                        this.findNavController().navigate(action)
                    } else {
                        val action =
                            AddressFragmentDirections.actionAddressFragmentToWorkSoonCourierFragment(
                                it
                            )
                        this.findNavController().navigate(action)
                    }
                },
                clickQrCode = {
                    this.findNavController().navigate(R.id.action_addressFragment_to_qrCodeFragment)
                },
                clickCamera = {
                    mCCTVViewModel.getCameras(it.toParcelable()) {
                        this.findNavController()
                            .navigate(R.id.action_addressFragment_to_mapCameraFragment)
                    }
                },
                clickEventLog = {
                    Timber.d("__Q__ event log clicked: ${it.flats}")
                    mEventLog.address = it.address
                    mEventLog.flatsAll = it.flats
                    mEventLog.filterFlat = null
                    mEventLog.currentEventDayFilter = null
                    mEventLog.lastLoadedDayFilterIndex.value = -1
                    mEventLog.currentEventItem = null
                    mEventLog.getAllFaces()

                    this.findNavController()
                        .navigate(R.id.action_addressFragment_to_eventLogFragment)
                }
            )
        )
        recyclerView = binding.rvParent
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.floatingActionButton.visibility == View.VISIBLE) {
                    binding.floatingActionButton.hide()
                } else if (dy < 0 && binding.floatingActionButton.visibility != View.VISIBLE) {
                    binding.floatingActionButton.show()
                }
                if (!recyclerView.canScrollVertically(-1)
                    && binding.floatingActionButton.visibility != View.VISIBLE
                ) {
                    binding.floatingActionButton.show()
                }
            }
        })
    }

    override fun onDismiss(dialog: GuestAccessDialogFragment) {
        dialog.dismiss()
    }

    override fun onShare() {}
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm address")
            intent?.let {
                mViewModel.nextListNoCache = true
                mViewModel.getDataList()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                IntentFilter(
                    MainActivity.BROADCAST_LIST_UPDATE
                )
            )
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }
}