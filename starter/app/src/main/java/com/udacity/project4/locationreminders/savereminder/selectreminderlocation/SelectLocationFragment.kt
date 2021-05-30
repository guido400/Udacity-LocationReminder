package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import kotlin.math.roundToLong

class SelectLocationFragment : BaseFragment(){

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val TAG = this::class.java.simpleName

    private lateinit var map: GoogleMap
    private lateinit var marker:Marker



    private val callback = OnMapReadyCallback {
            googleMap ->
        map = googleMap
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.map_style))
        enableMyLocation()
        setCameraOnCurrentLocation()
        setMapClick()
    }

    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        binding.fabSaveLocation.setOnClickListener { onLocationSelected() }
    }


    private fun onLocationSelected() {

        //send back the selected location details to the view model
        if (this::marker.isInitialized) {
            _viewModel.setLocation(marker)
        }
        else {
            val defaultLatLng = LatLng(52.631, 4.750)
            val defaultMarker = map.addMarker(MarkerOptions()
                .position(defaultLatLng)
                .title("Cheese Market Alkmaar"))
            _viewModel.setLocation(defaultMarker)
        }
        //navigate back to the previous fragment to save the reminder
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment()
            )
        )


    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            return
        }
    }

    @SuppressLint("MissingPermission")
    private fun setCameraOnCurrentLocation() {
        try{
        if (isPermissionGranted() ) {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    val result = task.result
                    if (result != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(result.latitude,
                                result.longitude), 12f))
                    }
                } else {
                    Log.e(TAG, "Exception: %s", task.exception)
                }
            }
        }
    } catch (e: SecurityException) {
        Log.e("Exception: %s", e.message, e)
    }
}
    private fun setMapClick() {
        map.setOnPoiClickListener { poi ->
            //remove old marker
            if (this::marker.isInitialized) { marker.remove() }

            //create new marker
            this.marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

        }

        map.setOnMapClickListener { latLng ->

            if (this::marker.isInitialized) { marker.remove() }

            this.marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.location_selected))
            )
        }



    }
}
