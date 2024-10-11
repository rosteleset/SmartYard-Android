package ru.madbrains.smartyard.ui.main.address.cctv_video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.threeten.bp.LocalDate
import ru.madbrains.domain.model.response.targetZoneId
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentCctvDetailBinding
import ru.madbrains.smartyard.ui.main.address.addressVerification.TabAdapter
import ru.madbrains.smartyard.ui.main.address.cctv_video.detail.CCTVOnlineTab
import ru.madbrains.smartyard.ui.main.address.cctv_video.detail.arhive.CCTVArchiveTab
import timber.log.Timber

class CCTVDetailFragment : Fragment() {
    private var _binding: FragmentCctvDetailBinding? = null
    private val binding get() = _binding!!

    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private var endDate = LocalDate.now(targetZoneId)
    private var startDate = endDate.minusDays(minusDate)

    companion object {
        const val ONLINE_TAB_POSITION = 0
        const val ARCHIVE_TAB_POSITION = 1
        private const val minusDate = 6L
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("debug_dmm __onActivityCreated")
        mCCTVViewModel.loadPeriod()
        setupObserve()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCctvDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("debug_dmm onViewCreated")
    }

    private fun setupUi(fm: FragmentManager) {
        binding.contentWrap.clipToOutline = true
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val adapter = TabAdapter(fm)
        Timber.d("debug_dmm __new adapter CCTVDetailFragment")
        adapter.addFragment(
            CCTVOnlineTab.newInstance(),
            resources.getString(R.string.cctv_detail_tab_online)
        )
        adapter.addFragment(
            CCTVArchiveTab.newInstance(startDate, endDate, mCCTVViewModel.availableRanges),
            resources.getString(R.string.cctv_detail_tab_archive)
        )

        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.viewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mCCTVViewModel.setCurrentTabPosition(position)
                setupTabs(adapter, position)
            }
        })
        binding.viewPager.setCurrentItem(mCCTVViewModel.currentTabId, false)
    }

    private fun setupTabs(adapter: TabAdapter, position: Int) {
        val onlineTabFragment =
            adapter.getItem(ONLINE_TAB_POSITION) as CCTVOnlineTab
        when (position) {
            ARCHIVE_TAB_POSITION -> {
                onlineTabFragment.releasePlayer()
            }

            ONLINE_TAB_POSITION -> {
                onlineTabFragment.initPlayer()
            }
        }
    }

    fun archiveCallback(chosenDate: LocalDate) {
//        val action =
//            CCTVDetailFragmentDirections.actionCCTVDetailFragmentToCCTVTrimmerFragment1(
//                chosenDate,
//                startDate
//            )
//
//        this.findNavController().navigate(action)
        val transaction = parentFragmentManager.beginTransaction()
        val newFragment = CCTVTrimmerFragment()
        val bundle = Bundle()
        bundle.putSerializable("chosenDate", chosenDate)
        bundle.putSerializable("startDate", startDate)
        newFragment.arguments = bundle

//        transaction.add(R.id.cl_fragment_wv_intercom, newFragment)
        transaction.add(R.id.cl_fragment_intercom_composable, newFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun setupObserve() {
        mCCTVViewModel.closedRangeCalendar.observe(
            viewLifecycleOwner,
            EventObserver {
                startDate = it.start
                endDate = it.endInclusive
                setupUi(childFragmentManager)
            }
        )
        mCCTVViewModel.chosenCamera.observe(
            viewLifecycleOwner,
            Observer { item ->
                item?.let {
                    val slash = it.name.indexOf("-")
                    if (0 < slash && slash < it.name.length - 1) {
                        binding.tvTitle.text = it.name.substring(0, slash).trim()
                        binding.tvTitleSub.text = it.name.substring(slash + 1).trim()
                    } else {
                        binding.tvTitle.text = it.name
                    }
                }
            }
        )
        mCCTVViewModel.updateCalendar.observe(
            viewLifecycleOwner,
            EventObserver {
                startDate = it.start
                endDate = it.endInclusive
                val adapter = binding.viewPager.adapter as TabAdapter
                val archive = adapter.getItem(ARCHIVE_TAB_POSITION) as CCTVArchiveTab
                archive.updateInstance(startDate, endDate, mCCTVViewModel.availableRanges)
            }
        )
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm __detail fragment onHidden, hidden = $hidden")
        if (hidden) {
            if (mCCTVViewModel.currentTabId == ONLINE_TAB_POSITION) {
                val onlineTabFragment =
                    (binding.viewPager.adapter as? TabAdapter)?.getItem(ONLINE_TAB_POSITION) as? CCTVOnlineTab
                Timber.d("debug_dmm __onlineTabFragment: $onlineTabFragment")
                onlineTabFragment?.releasePlayer()
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            if (mCCTVViewModel.currentTabId == ONLINE_TAB_POSITION) {
                val onlineTabFragment =
                    (binding.viewPager.adapter as? TabAdapter)?.getItem(ONLINE_TAB_POSITION) as? CCTVOnlineTab
                onlineTabFragment?.initPlayer()
            }
        }

        super.onHiddenChanged(hidden)
    }
}
