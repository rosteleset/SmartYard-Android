package ru.madbrains.smartyard.ui.main.settings

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
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentSettingsBinding
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.auth.AuthFragment
import ru.madbrains.smartyard.ui.main.settings.accessAddress.AccessAddressFragment
import ru.madbrains.smartyard.ui.main.settings.addressSettings.AddressSettingsFragment
import ru.madbrains.smartyard.ui.main.settings.dialog.DialogServiceFragment
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
//            this.findNavController().popBackStack()
            parentFragmentManager.popBackStack()
        }
        binding.floatingActionButton.setOnClickListener() {
//            (activity as MainActivity?)?.navigateToAddressAuthFragment()
            binding.rvSettings.stopScroll()
            binding.floatingActionButton.hide()
            val transaction = parentFragmentManager.beginTransaction()
            val newFragment = AuthFragment()
            transaction.add(R.id.cl_fragment_setting, newFragment)
            transaction.addToBackStack(null)
            transaction.commit()
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.swipeContainer.setOnRefreshListener {
            mViewModel.getDataList(true)
        }

        mViewModel.dataList.observe(
            viewLifecycleOwner,
            Observer {
                adapter.items = it
                adapter.notifyDataSetChanged()
                binding.swipeContainer.isRefreshing = false

                if (binding.floatingActionButton.visibility != View.VISIBLE) {
                    binding.floatingActionButton.show()
                }
            }
        )

        mViewModel.progress.observe(
            viewLifecycleOwner,
            Observer {
                if (!binding.swipeContainer.isRefreshing) {
                    binding.progressBar.isVisible = it
                }
                binding.swipeContainer.isRefreshing = false
            }
        )

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
        adapter = ListDelegationAdapter<List<SettingsAddressModel>>(
            SettingsAddressDelegate(
                requireActivity(),
                { address, flatId, isKey, contractOwner, clientId ->
//                    val action =
//                        SettingsFragmentDirections.actionSettingsFragmentToAddressSettingsFragment("")
//                    action.address = address
//                    action.flatId = flatId
//                    action.isKey = isKey
//                    action.contractOwner = contractOwner
//                    action.clientId = clientId
//                    this.findNavController().navigate(action)

                    val bundleSettings = Bundle()
                    bundleSettings.putString("address", address)
                    bundleSettings.putString("clientId", clientId)
                    bundleSettings.putInt("flatId", flatId)
                    bundleSettings.putBoolean("isKey", isKey)
                    bundleSettings.putBoolean("contractOwner", contractOwner)
                    binding.floatingActionButton.hide()

                    val transaction = parentFragmentManager.beginTransaction()
                    val newFragment = AddressSettingsFragment()
                    newFragment.arguments = bundleSettings

                    transaction.add(R.id.cl_fragment_setting, newFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                },
                mViewModel::getAccess,
                { address, flatId, contractOwner, hasGates, clientId ->
//                    val action =
//                        SettingsFragmentDirections.actionSettingsFragmentToAccessAddressFragment(
//                            "",
//                            ""
//                        )
//                    action.address = address
//                    action.flatId = flatId
//                    action.contractOwner = contractOwner
//                    action.hasGates = hasGates
//                    action.clientId = clientId
//                    this.findNavController().navigate(action)

                    val bundleSettings = Bundle()
                    bundleSettings.putString("address", address)
                    bundleSettings.putString("clientId", clientId)
                    bundleSettings.putInt("flatId", flatId)
                    bundleSettings.putBoolean("hasGates", hasGates)
                    bundleSettings.putBoolean("contractOwner", contractOwner)
                    binding.floatingActionButton.hide()

                    val transaction = parentFragmentManager.beginTransaction()
                    val newFragment = AccessAddressFragment()
                    newFragment.arguments = bundleSettings
                    transaction.add(R.id.cl_fragment_setting, newFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                },
                { position ->
                    val layoutManager = binding.rvSettings
                        .layoutManager as LinearLayoutManager
                    val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
                        override fun getVerticalSnapPreference(): Int {
                            return SNAP_TO_START
                        }
                    }
                    smoothScroller.targetPosition = position
                    layoutManager.startSmoothScroll(smoothScroller)
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
                    && binding.floatingActionButton.visibility != View.VISIBLE
                ) {
                    binding.floatingActionButton.show()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mViewModel.getDataList(true)
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
