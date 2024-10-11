package ru.madbrains.smartyard.ui.main.intercom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.smartyard.DiskCache
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentIntercomComposableBinding
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment.Companion.REFRESH_INTENT
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogFragment
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.Flat
import ru.madbrains.smartyard.ui.main.address.models.ParentModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.EventLogModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.IntercomItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModelP
import ru.madbrains.smartyard.ui.main.address.models.interfaces.Yard
import ru.madbrains.smartyard.ui.main.burger.cityCameras.CityCameraFragment
import ru.madbrains.smartyard.ui.main.burger.cityCameras.CityCamerasViewModel
import ru.madbrains.smartyard.ui.player.classes.VideoFrameLoader
import ru.madbrains.smartyard.ui.theme.ComposeAppTheme
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask


class IntercomComposableFragment : Fragment() {
    lateinit var binding: FragmentIntercomComposableBinding

    private val mCCTVViewModel by sharedStateViewModel<CCTVViewModel>()
    private val mViewModel by sharedViewModel<AddressViewModel>()
    private val mEventLog by sharedViewModel<EventLogViewModel>()
    private val mCityCameraViewModel by sharedViewModel<CityCamerasViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            FragmentIntercomComposableBinding.inflate(layoutInflater, container, false).apply {
                cvIntercom.apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        ComposeAppTheme {
                            MainIntercom(mCCTVViewModel, mViewModel) { dest, bundle ->
                                if (dest is CityCameraFragment) {
                                    bundle?.getInt("id")?.let {
                                        mCityCameraViewModel.chooseCityCameraById(it)
                                    }
                                }
                                navigateToFragment(R.id.cl_fragment_intercom_composable, dest, bundle)
                            }
                        }
                    }
                }
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadObservers()
        mCityCameraViewModel.getCityCameras {  }

    }

    private fun loadObservers() {
        mViewModel.openLink.observe(
            viewLifecycleOwner
        ) {
            if (it != null) {
                val intent = Intent(SHARE_OPEN_URL)
                intent.putExtra("message", it.second)
                intent.putExtra("id", it.first)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            }
        }
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (intent.action) {
                    RESET_CODE_DOOR -> {
                        val id = intent.getIntExtra("flatId", 0)
                        val domophoneId = intent.getLongExtra("domophoneId", 0)
                        mViewModel.resetCode(id, domophoneId)
                    }

                    OPEN_EVENT_LOG -> {
                        val houseId = intent.getIntExtra("houseId", 0)
                        val address = intent.getStringExtra("address") ?: ""
                        var flats: List<Flat>? = null
                        mViewModel.dataList.value?.forEach { item ->
                            if (item is ParentModel) {
                                if (item.houseId == houseId) {
                                    item.children.forEach {
                                        if (it is EventLogModel) {
                                            flats = it.flats
                                        }
                                    }
                                }
                            }
                        }
                        flats?.let {
                            mEventLog.address = address
                            mEventLog.flatsAll = it
                            mEventLog.filterFlat = null
                            mEventLog.currentEventDayFilter = null
                            mEventLog.lastLoadedDayFilterIndex.value = -1
                            mEventLog.currentEventItem = null
                            mEventLog.getAllFaces()
                            navigateToFragment(
                                R.id.cl_fragment_intercom_composable,
                                EventLogFragment()
                            )
                        }
                    }

                    REFRESH_INTENT -> {
                        mCCTVViewModel.getCameras(VideoCameraModelP(0,"")){
                            mViewModel.refresh()
                        }
                    }

                    else -> {}
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(RESET_CODE_DOOR)
        intentFilter.addAction(OPEN_EVENT_LOG)
        intentFilter.addAction(REFRESH_INTENT)
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                intentFilter
            )
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }


    private fun navigateToFragment(id: Int, fragment: Fragment, bundle: Bundle? = null) {
        val transaction = parentFragmentManager.beginTransaction()
        val existingFragment =
            parentFragmentManager.findFragmentByTag(fragment.javaClass.simpleName)
        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            if (bundle != null) {
                fragment.arguments = bundle
            }
            transaction.add(id, fragment, fragment.javaClass.simpleName)
            if (fragment !is IntercomWebViewFragment) {
                transaction.addToBackStack("root")
            }
        }
        val currentFragment = parentFragmentManager.findFragmentById(id)
        if (currentFragment != null) {
            transaction.hide(currentFragment)
        }
        transaction.commit()
    }

    private fun shouldReturnToMainScreen(fragment: Fragment): Boolean =
        fragment is IntercomWebViewFragment


    companion object {
        const val SHARE_OPEN_URL = "SHARE_OPEN_URL"
        const val RESET_CODE_DOOR = "RESET_CODE_DOOR"
        const val OPEN_EVENT_LOG = "OPEN_EVENT_LOG"
    }
}
