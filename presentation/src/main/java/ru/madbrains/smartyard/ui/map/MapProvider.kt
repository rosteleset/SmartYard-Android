package ru.madbrains.smartyard.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.ProgressDialog
import ru.madbrains.smartyard.ui.requestPermission
import ru.madbrains.smartyard.ui.showStandardAlert
import java.io.File

class MapProvider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        const val MIN_ZOOM = 2f
        const val MAX_ZOOM = 22f
        const val MAX_FOCUS_ZOOM = 19.9f
        const val DEFAULT_ZOOM = 12f
        val DEFAULT_COORDS = LatLng(55.75, 37.61)
    }

    private var mProgress: FrameLayout? = null
    var map: SimpleMap? = null

    private fun configOsm(applicationContext: Context) {
        Configuration.getInstance().userAgentValue = applicationContext.packageName
        val osmConf = Configuration.getInstance()
        val basePath = File(applicationContext.cacheDir.absolutePath, "osmdroid")
        osmConf.osmdroidBasePath = basePath
        val tileCache = File(osmConf.osmdroidBasePath.absolutePath, "tile")

        osmConf.osmdroidTileCache = tileCache
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
    }

    fun createMap(
        fragment: Fragment,
        settings: MapSettings,
        onInit: listenerGeneric<SimpleMap>? = null
    ) {
        context.applicationContext?.let { context ->
            val granted = {
                configOsm(context)
                map?.let {
                    it.onStop()
                    it.onDestroy()
                }
                val map = OSMMap(settings)
                this.map = map
                val view = map.create(
                    onInit = {
                        map.move(settings.initCoord, DEFAULT_ZOOM, instant = true)
                    },
                    onLayout = {
                        onInit?.invoke(map)
                    }
                )
                addView(view)
                mProgress = ProgressDialog().getView(context, true)
                addView(mProgress)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Для Android 10+ не запрашиваем разрешения
                //Для Android 13+ запрос на разрешение WRITE_EXTERNAL_STORAGE всегда выдаёт false
                granted()
            } else {
                requestPermission(
                    arrayListOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    context,
                    onGranted = {
                        granted()
                    },
                    onDenied = {
                        showStandardAlert(
                            fragment.requireContext(),
                            context.getString(R.string.error_map_permission),
                            ""
                        ) {
                            fragment.findNavController().popBackStack()
                        }
                    }
                )
            }
        }
    }
}
