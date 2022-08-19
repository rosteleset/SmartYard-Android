package com.sesameware.smartyard_oem.ui.main.address.addressVerification.office

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentOfficeBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.courier.CourierFragment

class OfficeFragment : Fragment() {
    private var _binding: FragmentOfficeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<OfficeViewModel>()
    private var address = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfficeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
        arguments?.let {
            address = it.getString(CourierFragment.ADDRESS_FIELD, "")
        }
        binding.btnOk.setOnClickListener {
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
            viewLifecycleOwner
        ) { listOffice ->
            listOffice.forEach {
                addGeoPoint(binding.map, it.lat, it.lon, it.address)
            }
            binding.map.zoomToBoundingBox(
                BoundingBox.fromGeoPoints(listOffice.map { GeoPoint(it.lat, it.lon) }),
                true
            )
            binding.map.postInvalidate()
        }
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
        marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker)
        mapView.overlays.add(marker)
    }

    private fun initMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setBuiltInZoomControls(true)
        binding.map.setMultiTouchControls(true)
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
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
