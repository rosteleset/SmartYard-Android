package com.sesameware.smartyard_oem.ui.main.settings

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.databinding.FragmentSettingsBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.MainActivityViewModel
import com.sesameware.smartyard_oem.ui.main.settings.dialog.DialogServiceFragment
import com.sesameware.smartyard_oem.ui.main.settings.model.AccessManagementPayload
import com.sesameware.smartyard_oem.ui.main.settings.model.AddressSettingsPayload
import com.sesameware.smartyard_oem.ui.main.settings.model.AvailableServicePayload
import com.sesameware.smartyard_oem.ui.openUrl
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.java.KoinJavaComponent.injectOrNull
import timber.log.Timber

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val delegate: SettingsDelegate? by injectOrNull(SettingsDelegate::class.java)

    private val mViewModel by sharedViewModel<SettingsViewModel>()
    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()
    private var adapter: SettingsAddressAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initObservers()
        initRecycler()
        bindViews()

        mViewModel.refreshSentName()

        delegate?.extendConfig(binding, adapter)
    }

    private fun bindViews() {
        binding.swipeContainer.setOnRefreshListener {
            mViewModel.getDataList(true)
        }

        binding.ivBackAddressSettings.setOnClickListener {
            this.findNavController().popBackStack()
        }
    }

    private fun showServiceDialog(data: SettingsViewModel.DialogServiceData) {
        val dialog = DialogServiceFragment()
        dialog.setData(data.dialog, getString(data.service.nameId))
        dialog.setListener(object : DialogServiceFragment.OnDialogServiceListener {
            override fun onDismiss() {
                dialog.dismiss()
            }

            override fun onDone() {
                context?.let {
                    mMainViewModel.navigateToChatAndMsg(it, data)
                }
                dialog.dismiss()
            }
        })
        dialog.show(parentFragmentManager, "")
    }

    private fun initObservers() {
        mViewModel.dataList.observe(
            viewLifecycleOwner
        ) {
            binding.tvEmptyList.isVisible = (it == null || it.isEmpty())
            adapter?.submitList(it)
            binding.swipeContainer.isRefreshing = false
        }

        mViewModel.progress.observe(
            viewLifecycleOwner
        ) {
            if (!binding.swipeContainer.isRefreshing) {
                binding.progressBar.isVisible = it
            }
            binding.swipeContainer.isRefreshing = false
        }

        mViewModel.dialogService.observe(
            viewLifecycleOwner,
            EventObserver {
                showServiceDialog(it)
            }
        )
    }

    private fun initRecycler() {
        val buttonAdapter = ButtonAdapter {
            (activity as MainActivity?)?.navigateToAddressAuthFragment()
        }
        binding.rvSettings.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            applyBottomNavInsetsToPadding()
        }
        adapter = SettingsAddressAdapter(
            ::onAddressSettingsClick,
            ::onAvailableServiceClick,
            ::onAccessManagementClick,
            ::onExpandItemClick,
            ::onAddressAccountClick
        )

        val config = ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false)
            .build()

        binding.rvSettings.adapter = ConcatAdapter(config, adapter, buttonAdapter)
    }

    private fun onAddressSettingsClick(payload: AddressSettingsPayload) {
        val action = SettingsFragmentDirections.actionSettingsFragmentToAddressSettingsFragment("")
        action.address = payload.address
        action.flatId = payload.flatId
        action.isKey = payload.isKey
        action.flatOwner = payload.flatOwner
        action.clientId = payload.clientId
        this.findNavController().navigate(action)
    }

    private fun onAvailableServiceClick(payload: AvailableServicePayload) {
        mViewModel.getAccess(payload.serviceType, payload.model, payload.isConnected)
    }

    private fun onAccessManagementClick(payload: AccessManagementPayload) {
        val action =
            SettingsFragmentDirections.actionSettingsFragmentToAccessAddressFragment(
                "",
                ""
            )
        action.address = payload.address
        action.flatId = payload.flatId
        action.flatOwner = payload.flatOwner
        action.hasGates = payload.hasGates
        action.hasPlog = payload.hasPlog
        action.clientId = payload.clientId
        this.findNavController().navigate(action)
    }

    private fun onExpandItemClick(position: Int, isExpanded: Boolean) {
        revealExpandedItem(position, isExpanded)
        mViewModel.modifyExpandedFlats(position, isExpanded)
    }

    private fun revealExpandedItem(position: Int, isExpanded: Boolean) {
        if (isExpanded) {
            val layoutManager = binding.rvSettings.layoutManager as LinearLayoutManager
            val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
            smoothScroller.targetPosition = position
            layoutManager.startSmoothScroll(smoothScroller)
        }
    }

    private fun onAddressAccountClick(url: String) {
        openUrl(activity, url)
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm lo")
            intent?.let {
                mViewModel.getDataList(true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mViewModel.onStart()
        context?.let {
            LocalBroadcastManager.getInstance(it)
                .registerReceiver(receiver, IntentFilter(MainActivity.BROADCAST_LIST_UPDATE))
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        adapter = null
        _binding = null
    }
}
