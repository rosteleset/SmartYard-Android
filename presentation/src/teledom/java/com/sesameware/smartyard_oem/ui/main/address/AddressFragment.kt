package com.sesameware.smartyard_oem.ui.main.address

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.CCTVDataTree
import com.sesameware.domain.model.response.CCTVRepresentationType
import com.sesameware.domain.model.response.CCTVViewTypeType
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentAddressBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.MainActivityViewModel
import com.sesameware.smartyard_oem.ui.main.address.adapters.AddressListAdapter
import com.sesameware.smartyard_oem.ui.main.address.adapters.HouseViewHolder
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVViewModel
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogViewModel
import com.sesameware.smartyard_oem.ui.main.address.guestAccessDialog.GuestAccessDialogFragment
import com.sesameware.smartyard_oem.ui.main.address.helpers.DragToSortCallback
import com.sesameware.smartyard_oem.ui.main.address.models.HouseAction
import com.sesameware.smartyard_oem.ui.main.address.models.IssueAction
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import com.sesameware.smartyard_oem.ui.main.address.models.OnCameraClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnEventLogClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnExpandClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnHouseAddressLongClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnIssueClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnItemFullyExpanded
import com.sesameware.smartyard_oem.ui.main.address.models.OnOpenEntranceClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnQrCodeClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnWebExtensionClick
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP
import com.sesameware.smartyard_oem.ui.updateAllWidget
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class AddressFragment : Fragment(), GuestAccessDialogFragment.OnGuestAccessListener {
    private var _binding: FragmentAddressBinding? = null
    private val binding get() = _binding!!

    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private val mViewModel by sharedViewModel<AddressViewModel>()
    private val mEventLog by sharedViewModel<EventLogViewModel>()

    private var adapter: AddressListAdapter? = null
    private var layoutManager: LinearLayoutManager? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm address")
            intent?.let {
                mViewModel.getDataList(true)
            }
        }
    }

    private val showHideFabListener = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy > 0 && binding.floatingActionButton.visibility == View.VISIBLE) {
                binding.floatingActionButton.hide()
            } else if (dy < 0 && binding.floatingActionButton.visibility != View.VISIBLE) {
                binding.floatingActionButton.show()
            }

            if (!recyclerView.canScrollVertically(-1)
                && binding.floatingActionButton.visibility != View.VISIBLE) {
                binding.floatingActionButton.show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAddressList()
        bindViews()
        initActivityObservers()
        initObservers()
    }

    private fun initAddressList() {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = AddressListAdapter(::onAddressAction, ::onIssueAction)
        binding.addressList.let {
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addOnScrollListener(showHideFabListener)
            val callback = DragToSortCallback(
                mViewModel::setHouseItemSavedPosition,
                ::onItemDrag,
                ::onItemRelease
            )
            itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper!!.attachToRecyclerView(it)
        }
    }

    private fun onItemDrag(viewHolder: RecyclerView.ViewHolder?) {
        (viewHolder as? HouseViewHolder)?.onThisItemDragged()
        requireInitialized(adapter).onViewDragged()
        val manager = requireInitialized(layoutManager)
        val firstVisible = manager.findFirstVisibleItemPosition()
        val lastVisible = manager.findLastVisibleItemPosition()
        (firstVisible..lastVisible).forEach {
            (binding.addressList.findViewHolderForLayoutPosition(it) as? HouseViewHolder)
                ?.onAnyItemDragged(true)
        }
        mViewModel.onItemDrag()
    }

    private fun onItemRelease(viewHolder: RecyclerView.ViewHolder?) {
        (viewHolder as? HouseViewHolder)?.onThisItemReleased()
        requireInitialized(adapter).onViewReleased()
    }

    private fun onAddressAction(action: HouseAction) {
        when (action) {
            is OnCameraClick -> navigateToCCTVFragment(action.model)
            is OnEventLogClick -> {
                prepareEventLogViewModel(action.title,action.houseId)
                navigateToEventLogFragment()
            }
            is OnExpandClick -> {
                mViewModel.setHouseItemExpanded(action.position, action.isExpanded)
            }
            is OnOpenEntranceClick -> mViewModel.openDoor(action.entranceId)
            is OnItemFullyExpanded -> scrollUntilFullItemVisible(action.position)
            is OnHouseAddressLongClick -> startDrag(action.position)
            is OnWebExtensionClick -> navigateToWebFragment(action.title, action.basePath, action.code)
        }
    }

    private fun startDrag(position: Int) {
        val helper = requireInitialized(itemTouchHelper)
        (binding.addressList.findViewHolderForLayoutPosition(position) as? HouseViewHolder)?.let {
            helper.startDrag(it)
        }
    }

    private fun scrollUntilFullItemVisible(position: Int) {
        val layoutManager = binding.addressList.layoutManager as LinearLayoutManager
        val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
        smoothScroller.targetPosition = position
        layoutManager.startSmoothScroll(smoothScroller)
    }

    private fun navigateToCCTVFragment(model: VideoCameraModelP) {
        val cctvDisplayMode = DataModule.providerConfig.cctvView
        val showOnMap = mViewModel.mPreferenceStorage.showCamerasOnMap
        if (cctvDisplayMode == CCTVViewTypeType.TREE ||
            cctvDisplayMode == CCTVViewTypeType.USER_DEFINED && !showOnMap) {
            prepareCCTVViewModelForTree(model)
        } else {
            mCCTVViewModel.getCameras(model) {
                this.findNavController().navigate(R.id.action_addressFragment_to_mapCameraFragment)
            }
        }
    }

    private fun prepareCCTVViewModelForTree(model: VideoCameraModelP) {
        mCCTVViewModel.getCamerasTree(model) {
            val group = mCCTVViewModel.cameraGroups.value
            mCCTVViewModel.chosenIndex.value = null
            mCCTVViewModel.chosenCamera.value = null
            mCCTVViewModel.chooseGroup(group?.groupId ?: CCTVDataTree.DEFAULT_GROUP_ID)
            mCCTVViewModel.getCameraList(
                group?.cameras ?: listOf(),
                group?.type ?: CCTVRepresentationType.MAP
            ) {
                val action = if (group?.type == CCTVRepresentationType.LIST)
                    AddressFragmentDirections.actionAddressFragmentToCCTVTreeFragment(group)
                else AddressFragmentDirections.actionAddressFragmentToMapCameraFragment()
                this.findNavController().navigate(action)
            }
        }
    }

    private fun prepareEventLogViewModel(title: String, houseId: Int) {
        mEventLog.address = title
        mEventLog.flatsAll = mViewModel.houseIdFlats[houseId] ?: listOf()
        mEventLog.filterFlat = null
        mEventLog.currentEventDayFilter = null
        mEventLog.lastLoadedDayFilterIndex.value = -1
        mEventLog.currentEventItem = null
        mEventLog.getAllFaces()
    }

    private fun navigateToEventLogFragment() {
        findNavController().navigate(R.id.action_addressFragment_to_eventLogFragment)
    }

    private fun navigateToWebFragment(title: String?, basePath: String?, code: String?) {
        val action = AddressFragmentDirections.actionAddressFragmentToCustomWebViewFragmentAddress(
            R.id.customWebViewFragmentAddress,
            R.id.customWebBottomFragmentAddress,
            basePath,
            code,
            title)
        this.findNavController().navigate(action)
    }

    private fun onIssueAction(action: IssueAction) {
        when (action) {
            is OnIssueClick -> {
                if (action.issue.courier) {
                    navigateToWorkSoonOfficeFragment(action.issue)
                } else {
                    navigateToWorkSoonCourierFragment(action.issue)
                }
            }
            OnQrCodeClick -> navigateToQrCodeFragment()
        }
    }

    private fun navigateToWorkSoonCourierFragment(issue: IssueModel) {
        val action = AddressFragmentDirections
            .actionAddressFragmentToWorkSoonCourierFragment(issue)
        findNavController().navigate(action)
    }

    private fun navigateToWorkSoonOfficeFragment(issue: IssueModel) {
        val action = AddressFragmentDirections
            .actionAddressFragmentToWorkSoonOfficeFragment(issue)
        findNavController().navigate(action)
    }

    private fun navigateToQrCodeFragment() {
        findNavController().navigate(R.id.action_addressFragment_to_qrCodeFragment)
    }

    private fun bindViews() {
        binding.floatingActionButton.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_addressFragment_to_authFragment)
        }
        binding.swipeContainer.setOnRefreshListener {
            mViewModel.getDataList(true)
        }
    }

    private fun initActivityObservers() {
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
                Timber.d("debug_dmm reloadToAddress")
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_global_addressFragment2)
                mViewModel.getDataList(true)
            }
        )
    }

    private fun initObservers() {
        val adapter = requireInitialized(adapter)
        mViewModel.addressUiState.observe(
            viewLifecycleOwner
        ) { addressList ->
            binding.tvEmptyList.isVisible = addressList.isEmpty()
            adapter.submitList(addressList)
            binding.swipeContainer.isRefreshing = false
            updateAllWidget(requireContext())

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

        mViewModel.navigateToAuth.observe(
            viewLifecycleOwner,
            EventObserver {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_addressFragment_to_authFragment)
            }
        )
    }

    override fun onDismiss(dialog: GuestAccessDialogFragment) {
        dialog.dismiss()
    }

    override fun onShare() {}

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
        mViewModel.persistUi()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.addressList.adapter = null
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        adapter = null
        layoutManager = null
        _binding = null
    }

    private fun <T> requireInitialized(value: T?): T =
        requireNotNull(value) { "Value must be initialized at this point" }
}
