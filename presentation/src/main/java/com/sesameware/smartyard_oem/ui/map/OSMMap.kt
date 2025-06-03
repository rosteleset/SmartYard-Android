package com.sesameware.smartyard_oem.ui.map

import android.content.res.Configuration
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.getCenter
import com.sesameware.smartyard_oem.toGeoPoint
import com.sesameware.smartyard_oem.ui.createIconWithText
import com.sesameware.smartyard_oem.ui.map.MapProvider.Companion.MAX_FOCUS_ZOOM
import com.sesameware.smartyard_oem.ui.map.MapProvider.Companion.MAX_ZOOM
import com.sesameware.smartyard_oem.ui.map.MapProvider.Companion.MIN_ZOOM
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.ANCHOR_CENTER


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
        setNightMode()
        onInit()
        view.minZoomLevel = MIN_ZOOM.toDouble()
        view.maxZoomLevel = MAX_ZOOM.toDouble()
        view.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        view.setMultiTouchControls(true)
        map?.addOnFirstLayoutListener { _, _, _, _, _ ->
            onLayout()
        }
        return view
    }

    private fun setNightMode() {
        val map = map!!
        val isNightModeOn = when (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_YES -> true
            else -> {
                false
            }
        }

        if (!isNightModeOn) return

        map.setBackgroundColor(ContextCompat.getColor(context, R.color.light_background))
        map.setTileSource(
            XYTileSource(
                "Carto CDN",
                0, 20, 256, ".png",
                arrayOf(
                    "https://a.basemaps.cartocdn.com/dark_all/",
                    "https://b.basemaps.cartocdn.com/dark_all/",
                    "https://c.basemaps.cartocdn.com/dark_all/"
                )
            )
        )
/*        val mapboxTilePolicy = object : TileSourcePolicy() {
            override fun getHttpCacheControlDuration(pHttpCacheControlHeader: String?): Long {
                val fixedHeader = pHttpCacheControlHeader
                    ?.split(',')
                    ?.joinToString(", ")
                return super.getHttpCacheControlDuration(fixedHeader)
            }
        }
        map.setTileSource(
            object : OnlineTileSourceBase(
                "Mapbox Dark",
                0, 19, 1024, null,
                arrayOf("https://api.mapbox.com/styles/v1/mapbox/dark-v11/tiles/"),
                null, mapboxTilePolicy
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    return (baseUrl + "512/" + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                            MapTileIndex.getX(pMapTileIndex) + "/" +
                            MapTileIndex.getY(pMapTileIndex) + "@2x" +
                            "?access_token=${DataModule.mapBoxToken}")
                }
            }
        )*/
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

    override fun placeMarkers(list: List<MarkerData>, moveTo: Boolean, instant: Boolean, bBox: BoundingBox?) {
        list.forEachIndexed { _: Int, it: MarkerData ->
            val marker = it.toMarkerWithIndex()
            map?.overlays?.add(marker)
        }
        val listLatLng = list.map { LatLng(it.position.latitude, it.position.longitude) }
        val boundingBox = bBox ?: BoundingBox.fromGeoPointsSafe(listLatLng.map { it.toGeoPoint() })
        val borderSize = if (bBox != null) 0 else 100
        map?.post(
            Runnable {
                try {
                    map?.zoomToBoundingBox(boundingBox, true, borderSize, MAX_FOCUS_ZOOM.toDouble(), moveDuration)
                } catch(e: Exception) {}
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
            marker.icon = createIconWithText(context, this.type.drawable, android.R.color.transparent, null)
        } else {
            marker.icon = createIconWithText(context, R.drawable.ic_map_oval, R.drawable.ic_map_camera, index?.plus(1)?.toString())
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
