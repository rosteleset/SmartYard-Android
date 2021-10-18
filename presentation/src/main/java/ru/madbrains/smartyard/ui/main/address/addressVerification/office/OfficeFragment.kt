package ru.madbrains.smartyard.ui.main.address.addressVerification.office

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_office.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.addressVerification.courier.CourierFragment

class OfficeFragment : Fragment() {

    private val viewModel by viewModel<OfficeViewModel>()
    private var address = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_office, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
        arguments?.let {
            address = it.getString(CourierFragment.ADDRESS_FIELD, "")
        }
        btnOk.setOnClickListener {
            viewModel.createIssue(address)
        }
        setupObserve()
    }

    private fun setupObserve() {
        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                (activity as MainActivity?)?.reloadToAddress()
            }
        )

        viewModel.offices.observe(
            viewLifecycleOwner,
            Observer {
                it.forEach {
                    addGeoPoint(map, it.lat, it.lon, it.address)
                }
                map?.zoomToBoundingBox(
                    BoundingBox.fromGeoPoints(it.map { GeoPoint(it.lat, it.lon) }),
                    true
                )
                map?.postInvalidate()
            }
        )
    }

    private fun addGeoPoint(
        mapView: MapView,
        a: Double,
        b: Double,
        title: String
    ) {
        val position = GeoPoint(a, b)
        val marker = Marker(mapView)
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.position = position
        marker.icon = ContextCompat.getDrawable(context!!, R.drawable.ic_marker)
        mapView.overlays.add(marker)
    }

    private fun initMap() {
        map?.setTileSource(TileSourceFactory.MAPNIK)
        map?.setBuiltInZoomControls(true)
        map?.setMultiTouchControls(true)
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    companion object {
        fun getInstance(address: String): OfficeFragment {
            val officeFragment = OfficeFragment()
            val bundle = Bundle().apply {
                putString(ADDRESS_FIELD, address)
            }
            return officeFragment.apply {
                arguments = bundle
            }
        }

        const val ADDRESS_FIELD = "address_field"
    }
}
