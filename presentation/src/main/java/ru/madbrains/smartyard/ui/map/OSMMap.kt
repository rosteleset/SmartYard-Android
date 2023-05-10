package ru.madbrains.smartyard.ui.map

import android.view.View
import android.widget.LinearLayout
import org.osmdroid.api.IMapController
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.ANCHOR_CENTER
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.getCenter
import ru.madbrains.smartyard.toGeoPoint
import ru.madbrains.smartyard.ui.createIconWithText
import ru.madbrains.smartyard.ui.createIconWithoutText
import ru.madbrains.smartyard.ui.map.MapProvider.Companion.MAX_FOCUS_ZOOM
import ru.madbrains.smartyard.ui.map.MapProvider.Companion.MAX_ZOOM
import ru.madbrains.smartyard.ui.map.MapProvider.Companion.MIN_ZOOM
import java.lang.Exception

class OSMMap(settings: MapSettings) : SimpleMap(settings) {
    companion object {
        private const val moveDuration: Long = 0
    }

    private var controller: IMapController? = null
    private var map: MapView? = null
    override fun create(onInit: listenerEmpty, onLayout: listenerEmpty): View {
        val view = MapView(context)
        map = view
        controller = view.controller

        view.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        onInit()
        view.minZoomLevel = MIN_ZOOM.toDouble()
        view.maxZoomLevel = MAX_ZOOM.toDouble()
        view.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        view.setMultiTouchControls(true)
        map?.addOnFirstLayoutListener { v, left, top, right, bottom ->
            onLayout()
        }
        return view
    }

    override fun move(coord: LatLng, zoom: Float, instant: Boolean) {
        setZoom(zoom)
        val point = coord.toGeoPoint()
        if (instant) {
            controller?.setCenter(point)
        } else {
            controller?.animateTo(point, zoom.toDouble(), moveDuration)
        }
    }

    override fun move(pointsList: List<LatLng>, instant: Boolean) {
        if (map?.isLayoutOccurred == true) {
            val box = BoundingBox.fromGeoPointsSafe(pointsList.map { it.toGeoPoint() })
            if (instant) {
                map?.zoomToBoundingBox(box, false)
            } else {
                map?.zoomToBoundingBox(box, true)
            }
        } else {
            move(pointsList.getCenter(), MapProvider.DEFAULT_ZOOM, instant = true)
            map?.addOnFirstLayoutListener(object : MapView.OnFirstLayoutListener {
                override fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int) {
                    move(pointsList, instant)
                    map?.removeOnFirstLayoutListener(this)
                }
            })
        }
        map?.invalidate()
    }

    override fun placeMarker(data: MarkerData, moveTo: Boolean, instant: Boolean) {
        val marker = data.toMarkerWithIndex()
        map?.overlays?.add(marker)
        if (moveTo) move(data.position, MAX_FOCUS_ZOOM, instant)
    }

    override fun placeMarkers(
        list: List<MarkerData>,
        moveTo: Boolean,
        instant: Boolean,
        bBox: BoundingBox?
    ) {
        list.forEachIndexed { index: Int, it: MarkerData ->
            val marker = it.toMarkerWithIndex()
            map?.overlays?.add(marker)
        }
        val listLatLng = list.map { LatLng(it.position.latitude, it.position.longitude) }
        val boundingBox = bBox ?: BoundingBox.fromGeoPointsSafe(listLatLng.map { it.toGeoPoint() })
        val borderSize = if (bBox != null) 0 else 100
        map?.post(
            Runnable {
                try {
                    map?.zoomToBoundingBox(
                        boundingBox,
                        true,
                        borderSize,
                        MAX_FOCUS_ZOOM.toDouble(),
                        moveDuration
                    )
                } catch (e: Exception) {
                }
            }
        )
        map?.invalidate()
    }

    override fun setZoom(zoom: Float) {
        controller?.setZoom(zoom.toDouble())
    }

    override fun getZoom(): Float {
        return map?.zoomLevelDouble?.toFloat() ?: 0f
    }

    override fun onPermissionGranted() {}

    override fun onResume() {
        map?.onResume()
    }

    override fun onPause() {
        map?.onPause()
    }

    override fun onStop() {}

    override fun onLowMemory() {}

    override fun onDestroy() {}

    override fun clearObjects() {
    }

    private fun MarkerData.toMarkerWithIndex(): Marker {
        val marker = Marker(map)
        marker.setAnchor(ANCHOR_CENTER, ANCHOR_CENTER)
        marker.position = position.toGeoPoint()
        if (this.type == MarkerType.CityCamera) {
            marker.icon =
                createIconWithText(context, this.type.drawable, android.R.color.transparent, null)
        } else {
//            marker.icon = createIconWithText(context, R.drawable.ic_map_oval, R.drawable.ic_map_camera, index?.plus(1)?.toString())
            marker.icon = createIconWithoutText(context, R.drawable.bg_oval_active, R.drawable.ic_map_camera)
        }

        marker.setInfoWindow(null)
        marker.isDraggable = false
        marker.isFlat = true
        settings.onMarkerClick?.let { click ->
            marker.setOnMarkerClickListener { _, _ -> click(this) }
        }
        return marker
    }

    //для запоминания текущего положения карты камер
    fun getBoundingBox(): BoundingBox? {
        return map?.boundingBox
    }
}
