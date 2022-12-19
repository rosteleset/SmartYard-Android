package com.sesameware.smartyard_oem.ui.map

import android.content.Context
import android.view.View
import org.osmdroid.util.BoundingBox
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.domain.utils.listenerGenericR
import com.sesameware.smartyard_oem.R

data class LatLng(
    val latitude: Double,
    val longitude: Double
)
class MapSettings(
    val context: Context,
    val initCoord: LatLng = MapProvider.DEFAULT_COORDS,
    val onMarkerClick: listenerGenericR<MarkerData, Boolean>? = null
)
abstract class SimpleMap(val settings: MapSettings) {
    val context = settings.context

    abstract fun create(onInit: listenerEmpty, onLayout: listenerEmpty): View
    abstract fun move(coord: LatLng, zoom: Float, instant: Boolean = false)
    abstract fun move(pointsList: List<LatLng>, instant: Boolean = false)
    abstract fun setZoom(zoom: Float)
    abstract fun getZoom(): Float
    abstract fun onStop()
    abstract fun onPermissionGranted()
    abstract fun placeMarker(data: MarkerData, moveTo: Boolean = true, instant: Boolean = false)
    abstract fun placeMarkers(list: List<MarkerData>, moveTo: Boolean = true, instant: Boolean = false, bBox: BoundingBox? = null)
    abstract fun onResume()
    abstract fun onPause()
    abstract fun onLowMemory()
    abstract fun onDestroy()
    abstract fun clearObjects()
}

data class MarkerData(
    val type: MarkerType,
    val position: LatLng,
    val title: String = "",
    val index: Int? = null
)
enum class MarkerType(val drawable: Int, val zIndex: Float, val anchor: Array<Float> = arrayOf(0.5f, 0.5f)) {
    Camera(R.drawable.ic_map_camera, 3F, arrayOf(0.5f, 0.5f)),
    CityCamera(R.drawable.ic_map_city_camera, 3F, arrayOf(0.5f, 0.5f))
}
