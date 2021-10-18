package ru.madbrains.smartyard.ui.main.address.cctv_video

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_cctv_map.*
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.smartyard.MapFragment
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.toLatLng
import ru.madbrains.smartyard.ui.map.MapProvider
import ru.madbrains.smartyard.ui.map.MapSettings
import ru.madbrains.smartyard.ui.map.MarkerData
import ru.madbrains.smartyard.ui.map.MarkerType
import ru.madbrains.smartyard.utils.stateSharedViewModel

class CCTVMapFragment : MapFragment() {
    private val mCCTVViewModel: CCTVViewModel by stateSharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_cctv_map, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let { context ->
            setupUi(context)
        }
    }

    private fun setupUi(context: Context) {
        contentWrap.clipToOutline = true
        ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        tvTitleSub.text = mCCTVViewModel.cctvModel.value?.address
        mCCTVViewModel.cameraList.value?.let { list ->
            createMapProvider(context, list)
        }
    }

    private fun createMapProvider(
        context: Context,
        list: List<CCTVData>
    ) {
        val mapProvider = MapProvider(context)
        this.mapProvider = mapProvider
        val settings = MapSettings(context) { marker ->
            marker.index?.let {
                mCCTVViewModel.chooseCamera(it)
                this.findNavController().navigate(R.id.action_CCTVMapFragment_to_CCTVDetailFragment)
            }
            true
        }
        val listMarker = list.mapIndexed { index, item ->
            MarkerData(MarkerType.Camera, item.toLatLng(), "", index)
        }
        contentWrap.removeAllViews()
        contentWrap.addView(mapProvider)
        mapProvider.createMap(this, settings) {
            it.placeMarkers(
                listMarker,
                instant = true
            )
        }
    }

    private fun setupObserve() {}
}
