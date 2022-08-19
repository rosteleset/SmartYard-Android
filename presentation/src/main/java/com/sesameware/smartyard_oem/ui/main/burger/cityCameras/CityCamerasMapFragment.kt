package com.sesameware.smartyard_oem.ui.main.burger.cityCameras

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.domain.model.response.CCTVCityCameraData
import com.sesameware.smartyard_oem.MapFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.CityCamerasMapFragmentBinding
import com.sesameware.smartyard_oem.toLatLng
import com.sesameware.smartyard_oem.ui.map.*

class CityCamerasMapFragment : MapFragment() {
    private var _binding: CityCamerasMapFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityCamerasViewModel by sharedStateViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = CityCamerasMapFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        context?.let { context ->
            setupUi(context)
        }
    }

    private fun setupUi(context: Context) {
        binding.llCityCamerasMap.clipToOutline = true
        binding.ivCityCamerasBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        viewModel.getCityCameras {
            createMapProvider(context, viewModel.cityCameraList)
        }
    }

    private fun createMapProvider(
        context: Context,
        list: List<CCTVCityCameraData>
    ) {
        val mapProvider = MapProvider(context)
        this.mapProvider = mapProvider
        val settings = MapSettings(context) { marker ->
            marker.index?.let {
                viewModel.chooseCityCamera(it)
                this.findNavController().navigate(R.id.action_cityCamerasMapFragment_to_cityCameraFragment)
            }
            true
        }
        val listMarker = list.mapIndexed { index, item ->
            MarkerData(MarkerType.CityCamera, item.toLatLng(), "", index)
        }
        binding.llCityCamerasMap.removeAllViews()
        binding.llCityCamerasMap.addView(mapProvider)
        mapProvider.createMap(this, settings) {
            it.placeMarkers(
                listMarker,
                instant = true,
                bBox = viewModel.camerasBoundingBox
            )
        }
    }

    override fun onStop() {
        super.onStop()

        viewModel.camerasBoundingBox = (this.mapProvider?.map as? OSMMap)?.getBoundingBox()
    }

}
