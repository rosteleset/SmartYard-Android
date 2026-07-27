package com.sesameware.smartyard_oem.ui.main.settings.faceSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentFaceSettingsBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogViewModel
import com.sesameware.smartyard_oem.ui.main.address.event_log.Flat
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.adapters.FaceSettingsAdapter
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.dialogRemovePhoto.DialogRemovePhotoFragment
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.dialogViewPhoto.DialogViewPhotoFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class FaceSettingsFragment : Fragment() {
    private var _binding: FragmentFaceSettingsBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by sharedViewModel<FaceSettingsViewModel>()
    private val mEventLogVM by sharedViewModel<EventLogViewModel>()
    private var flatId = 0
    private var address = ""
    private var canAddFace = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentFaceSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            flatId = FaceSettingsFragmentArgs.fromBundle(it).flatId
            address = FaceSettingsFragmentArgs.fromBundle(it).address
            canAddFace = FaceSettingsFragmentArgs.fromBundle(it).canAddFace
            mViewModel.listFaces(flatId, true)
        }

        binding.ivFSAddFace.isVisible = canAddFace
        binding.tvFSComments.isVisible = canAddFace

        binding.ivFaceSettingsBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.srlFaceSettings.setOnRefreshListener {
            binding.srlFaceSettings.isRefreshing = false
            mViewModel.listFaces(flatId, true)
        }

        initObservers()

        binding.ivFSAddFace.setOnClickListener {
            if (!canAddFace) return@setOnClickListener

            mEventLogVM.address = address
            mEventLogVM.flatsAll = listOf(Flat(flatId, "", true))
            mEventLogVM.filterFlat = null
            mEventLogVM.lastLoadedDayFilterIndex.value = -1
            mEventLogVM.currentEventItem = null

            (requireActivity() as MainActivity).binding.bottomNav.selectedItemId = R.id.address
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.addressFragment, false)
                .build()
            findNavController().navigate(R.id.eventLogFragment, null, navOptions)
        }
    }

    private fun initObservers() {
        mViewModel.faces.observe(
            viewLifecycleOwner
        ) {
            binding.rvFSFaces.apply {
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                it?.let { faces ->
                    adapter = FaceSettingsAdapter(faces,
                        { position ->
                            val dialogViewPhoto =
                                DialogViewPhotoFragment(faces[position].faceImage)
                            dialogViewPhoto.show(requireActivity().supportFragmentManager, "")
                        },
                        { position ->
                            val dialogRemovePhoto =
                                DialogRemovePhotoFragment(faces[position].faceImage) {
                                    mViewModel.removeFace(
                                        flatId,
                                        faces[position].faceId.toInt()
                                    )
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
    }
}
