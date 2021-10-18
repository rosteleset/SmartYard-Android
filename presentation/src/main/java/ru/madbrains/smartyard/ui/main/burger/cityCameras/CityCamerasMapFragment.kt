package ru.madbrains.smartyard.ui.main.burger.cityCameras

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.city_cameras_map_fragment.*
import ru.madbrains.domain.model.response.CCTVCityCameraData
import ru.madbrains.smartyard.MapFragment
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.toLatLng
import ru.madbrains.smartyard.ui.map.*
import ru.madbrains.smartyard.utils.stateSharedViewModel

class CityCamerasMapFragment : MapFragment() {
    private val viewModel: CityCamerasViewModel by stateSharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.city_cameras_map_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        context?.let { context ->
            setupUi(context)
        }
    }

    private fun setupUi(context: Context) {
        llCityCamerasMap.clipToOutline = true
        ivCityCamerasBack.setOnClickListener {
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
        llCityCamerasMap.removeAllViews()
        llCityCamerasMap.addView(mapProvider)
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
