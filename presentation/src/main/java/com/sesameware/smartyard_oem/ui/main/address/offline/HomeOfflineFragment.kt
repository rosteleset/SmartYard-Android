package com.sesameware.smartyard_oem.ui.main.address.offline

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sesameware.smartyard_oem.databinding.FragmentHomeOfflineBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class HomeOfflineFragment : Fragment() {
    private var _binding: FragmentHomeOfflineBinding? = null
    private val viewModel by viewModel<HomeOfflineViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentHomeOfflineBinding.inflate(inflater, container, false).also { _binding = it }.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = _binding ?: return
        val adapter = HomeOfflineAdapter(viewModel::setHouseItemExpanded)
        binding.rvOfflineAddresses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOfflineAddresses.adapter = adapter
        fun dismiss() { (activity as? MainActivity)?.attemptDismissOffline() }
        binding.ivBack.setOnClickListener { dismiss() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { dismiss() }

        var startX = 0f
        var startY = 0f
        binding.headerLayout.setOnTouchListener { header, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y; true }
                MotionEvent.ACTION_UP -> {
                    val dy = event.y - startY
                    if (dy > 48 * resources.displayMetrics.density && dy > abs(event.x - startX)) dismiss()
                    else header.performClick()
                    true
                }
                else -> true
            }
        }
        val headerTop = binding.headerLayout.paddingTop
        val listBottom = binding.rvOfflineAddresses.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            root.updatePadding(left = bars.left, right = bars.right)
            binding.headerLayout.updatePadding(top = headerTop + bars.top)
            binding.rvOfflineAddresses.updatePadding(bottom = listBottom + bars.bottom)
            binding.tvEmptyState.updatePadding(bottom = bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        viewModel.offlineAddresses.observe(viewLifecycleOwner) { addresses ->
            adapter.submitList(addresses)
            binding.tvEmptyState.visibility = if (addresses.isEmpty()) View.VISIBLE else View.GONE
            binding.rvOfflineAddresses.visibility = if (addresses.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.loadData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (viewModel.offlineAddresses.value != null) {
            viewModel.persistExpandedHouseIds()
        }
    }

    override fun onDestroyView() {
        _binding?.rvOfflineAddresses?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
