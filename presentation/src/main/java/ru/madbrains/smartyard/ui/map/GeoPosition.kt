package ru.madbrains.smartyard.ui.map

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import ru.madbrains.smartyard.Event
import timber.log.Timber

class GeoPosition(val fragment: Fragment) : LifecycleEventObserver {
//    private val context = fragment.requireContext()
//    private var requestingLocationUpdates: Boolean = false
//    private lateinit var locationCallback: LocationCallback
//    private var fusedLocationClient: FusedLocationProviderClient? = null
//    private lateinit var locationRequest: LocationRequest
//
//    private val _location = MutableLiveData<List<Location>>()
//    val location: LiveData<List<Location>>
//        get() = _location
//
//    companion object {
//        const val TAG: String = "GeoPosition"
//        const val REQUEST_GEO_CODE = 0x1
//    }
//
//    init {
//        fragment.lifecycle.addObserver(this)
//        onAttach()
//    }
//
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
//                onCreate()
            }

            Lifecycle.Event.ON_START -> {
            }

            Lifecycle.Event.ON_RESUME -> {
            }

            Lifecycle.Event.ON_PAUSE -> {
            }

            Lifecycle.Event.ON_STOP -> {
            }

            Lifecycle.Event.ON_DESTROY -> {
//                stopLocationUpdates()
            }

            Lifecycle.Event.ON_ANY -> {
            }
        }
    }
//
//
//    private fun onAttach() {
//        checkPermissions()
//    }
//
//    fun onCreate() {
//        fusedLocationClient =
//            LocationServices.getFusedLocationProviderClient(fragment.requireContext())
//        getCurrentUpdatePosition()
//    }
//
//    private fun stopLocationUpdates() {
//        fusedLocationClient?.removeLocationUpdates(locationCallback)
//    }
//
//    private fun getCurrentUpdatePosition() {
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                _location.value = locationResult.locations
////                for (location in locationResult.locations) {
////                    Timber.d("${location.longitude} ${location.latitude}")
////                }
//            }
//
//            override fun onLocationAvailability(p0: LocationAvailability) {
//                super.onLocationAvailability(p0)
//            }
//        }
//    }
//
//
//    private val resolutionForResult =
//        fragment.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
//            if (activityResult.resultCode == Activity.RESULT_OK) {
//                startLocationUpdates()
//            } else {
//
//            }
//        }
//
//
//    private fun startLocationUpdates() {
//        checkPermission()
//        fusedLocationClient?.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )
//    }
//
//    private fun createLocationRequest() {
//        locationRequest = LocationRequest.create()
//        locationRequest.apply {
//            interval = 10000
//            fastestInterval = 10000
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        val builder = LocationSettingsRequest.Builder()
//            .addLocationRequest(locationRequest)
//        val client = LocationServices.getSettingsClient(context)
//        val task = client.checkLocationSettings(builder.build())
//        task.addOnSuccessListener { locationSettingsResponse ->
//            locationSettingsResponse?.let {
//                startLocationUpdates()
//            }
//        }
//        task.addOnFailureListener { exception ->
//            if (exception is ResolvableApiException) {
//                try {
//                    // Show the dialog by calling startResolutionForResult(),
//                    // and check the result in onActivityResult().
//                    exception.startResolutionForResult(
//                        fragment.requireActivity(),
//                        REQUEST_GEO_CODE
//                    )
//                    val intentSenderRequest =
//                        IntentSenderRequest.Builder(exception.resolution).build()
//                    resolutionForResult.launch(intentSenderRequest)
//                } catch (sendEx: IntentSender.SendIntentException) {
//                    // Ignore the error.
//                }
//            }
//        }
//
//    }
//
//    private fun checkPermissions() {
//        val locationPermissionRequest = fragment.registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()
//        ) { permissions ->
//            when {
//                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
//                    // Precise location access granted.
//                    createLocationRequest()
//                }
//
//                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
//                    // Only approximate location access granted.
//                    createLocationRequest()
//                }
//
//                else -> {
//                    // No location access granted.
//                }
//            }
//        }
//
//        locationPermissionRequest.launch(
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//        )
//    }
//
//    private fun checkPermission() {
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            return
//        }
//    }
}