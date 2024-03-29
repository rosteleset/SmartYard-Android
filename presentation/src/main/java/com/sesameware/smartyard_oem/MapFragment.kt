package com.sesameware.smartyard_oem

import androidx.fragment.app.Fragment
import com.sesameware.smartyard_oem.ui.map.MapProvider

abstract class MapFragment : Fragment() {
    protected var mapProvider: MapProvider? = null

    override fun onResume() {
        super.onResume()
        mapProvider?.map?.onResume()
    }
    override fun onPause() {
        super.onPause()
        mapProvider?.map?.onPause()
    }

    override fun onStop() {
        mapProvider?.map?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapProvider?.map?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapProvider?.map?.onDestroy()
    }
}
