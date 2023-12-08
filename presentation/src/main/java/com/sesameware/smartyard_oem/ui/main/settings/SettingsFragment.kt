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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.databinding.FragmentSettingsBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.MainActivityViewModel
import com.sesameware.smartyard_oem.ui.main.settings.dialog.DialogServiceFragment
import timber.log.Timber

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by sharedViewModel<SettingsViewModel>()
    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()
    lateinit var adapter: ListDelegationAdapter<List<SettingsAddressModel>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()
        binding.ivBackAddressSettings.setOnClickListener {
            this.findNavController().popBackStack()
        }
        binding.floatingActionButton.setOnClickListener() {
            (activity as MainActivity?)?.navigateToAddressAuthFragment()
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

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.swipeContainer.setOnRefreshListener {
            mViewModel.getDataList(true)
        }

        mViewModel.dataList.observe(
            viewLifecycleOwner
        ) {
            adapter.items = it
            adapter.notifyDataSetChanged()
            binding.swipeContainer.isRefreshing = false

            if (binding.floatingActionButton.visibility != View.VISIBLE) {
                binding.floatingActionButton.show()
            }
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

        mViewModel.refreshSentName()
    }

    private fun initRecycler() {
        binding.rvSettings.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter(
            SettingsAddressDelegate(
                requireActivity(),
                { address, flatId, isKey, flatOwner, clientId ->
                    val action =
                        SettingsFragmentDirections.actionSettingsFragmentToAddressSettingsFragment("")
                    action.address = address
                    action.flatId = flatId
                    action.isKey = isKey
                    action.flatOwner = flatOwner
                    action.clientId = clientId
                    this.findNavController().navigate(action)
                },
                mViewModel::getAccess,
                { address, flatId, flatOwner, hasGates, clientId ->
                    val action =
                        SettingsFragmentDirections.actionSettingsFragmentToAccessAddressFragment(
                            "",
                            ""
                        )
                    action.address = address
                    action.flatId = flatId
                    action.flatOwner = flatOwner
                    action.hasGates = hasGates
                    action.clientId = clientId
                    this.findNavController().navigate(action)
                },
                { position, isExpanded ->
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
                    adapter.items?.get(position)?.let {
                        if (isExpanded) {
                            mViewModel.expandedFlatId.add(it.flatId)
                        } else {
                            mViewModel.expandedFlatId.remove(it.flatId)
                        }
                        it.isExpanded = isExpanded
                    }
                }
            )
        )
        adapter.items = emptyList()
        binding.rvSettings.adapter = adapter
        binding.rvSettings.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.floatingActionButton.visibility == View.VISIBLE) {
                    binding.floatingActionButton.hide()
                } else if (dy < 0 && binding.floatingActionButton.visibility != View.VISIBLE) {
                    binding.floatingActionButton.show()
                }

                if (!binding.rvSettings.canScrollVertically(-1)
                    && binding.floatingActionButton.visibility != View.VISIBLE) {
                    binding.floatingActionButton.show()
                }
            }
        })
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm lo")
            intent?.let {
                mViewModel.nextListNoCache = true
                mViewModel.getDataList()
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
}
