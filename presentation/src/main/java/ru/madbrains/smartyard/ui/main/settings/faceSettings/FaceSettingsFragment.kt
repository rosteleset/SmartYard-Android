package ru.madbrains.smartyard.ui.main.settings.faceSettings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.bottom_nav
import kotlinx.android.synthetic.main.fragment_face_settings.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.getFragmentTag
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.Flat
import ru.madbrains.smartyard.ui.main.settings.faceSettings.adapters.FaceSettingsAdapter
import ru.madbrains.smartyard.ui.main.settings.faceSettings.dialogRemovePhoto.DialogRemovePhotoFragment
import ru.madbrains.smartyard.ui.main.settings.faceSettings.dialogViewPhoto.DialogViewPhotoFragment

class FaceSettingsFragment : Fragment() {
    private val mViewModel by sharedViewModel<FaceSettingsViewModel>()
    private val mEventLogVM by sharedViewModel<EventLogViewModel>()
    private var flatId = 0
    private var address = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_face_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            flatId = FaceSettingsFragmentArgs.fromBundle(it).flatId
            address = FaceSettingsFragmentArgs.fromBundle(it).address
            mViewModel.listFaces(flatId, true)
        }

        ivFaceSettingsBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        srlFaceSettings.setOnRefreshListener {
            srlFaceSettings.isRefreshing = false
            mViewModel.listFaces(flatId, true)
        }

        initObservers()

        ivFSAddFace.setOnClickListener {
            mEventLogVM.address = address
            mEventLogVM.flatsAll = listOf(Flat(flatId, "", true))
            mEventLogVM.filterFlat = null
            mEventLogVM.lastLoadedDayFilterIndex.value = -1
            mEventLogVM.currentEventItem = null

            (requireActivity() as MainActivity).bottom_nav.selectedItemId = R.id.address
            val host = (requireActivity() as? MainActivity)?.supportFragmentManager?.findFragmentByTag(
                getFragmentTag(0)) as NavHostFragment?
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.addressFragment, false)
                .build()
            host?.navController?.navigate(R.id.eventLogFragment, null, navOptions)
        }
    }

    private fun initObservers() {
        mViewModel.faces.observe(
            viewLifecycleOwner,
            {
                rvFSFaces.apply {
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    it?.let {faces ->
                        adapter = FaceSettingsAdapter(faces,
                            {position ->
                                val dialogViewPhoto =
                                    DialogViewPhotoFragment(faces[position].faceImage)
                                dialogViewPhoto.show(requireActivity().supportFragmentManager, "")
                            },
                            {position ->
                                val dialogRemovePhoto =
                                    DialogRemovePhotoFragment(faces[position].faceImage) {
                                        mViewModel.removeFace(flatId,
                                            faces[position].faceId.toInt())
                                    }
                                dialogRemovePhoto.show(requireActivity().supportFragmentManager, "")
                            }
                        )
                    }
                    if (it == null) {
                        adapter = FaceSettingsAdapter(listOf(), {}, {})
                    }
                }
            }
        )
    }
}
