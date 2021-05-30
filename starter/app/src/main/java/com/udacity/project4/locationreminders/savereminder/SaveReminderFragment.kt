package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.EspressoIdlingResource.wrapEspressoIdlingResource
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


const val ACTION_GEOFENCE_EVENT = "actionGeofenceIntent"
const val NEW_REMINDER_KEY = "newReminderKey"
const val NEW_REMINDER_BUNDLE = "newReminderBundle"
const val GEOFENCE_RADIUS_IN_METERS = 10000f
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "SaveReminderFragment"



class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var binding: FragmentSaveReminderBinding
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
    private lateinit var newReminder:ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        //set textview text for poi selected in select location fragment
        _viewModel.selectedPOI.observe(viewLifecycleOwner, Observer { poi->
            binding.selectedLocation.text = poi.name
        })


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            wrapEspressoIdlingResource {
                (activity as RemindersActivity).checkLocationPermissions()
            }

            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            newReminder = ReminderDataItem(title,description, location, latitude, longitude)

            val isValid = _viewModel.validateEnteredData(newReminder)

            if (isValid) {
                //add a geofencing request
                checkPermissionsAndAddGeofence()
            }


        }
    }

    private fun checkPermissionsAndAddGeofence() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                createGeofence()
            }
        }
    }

    /**
     * Create geofence by creating geofence object, geofence request and pending intent for broadcast receiver.
     * pending intent will use reminder object to create a notification when geofence entered.
     */
    @SuppressLint("MissingPermission")
    private fun createGeofence() {
        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_EVENT

            val bundle = Bundle()
            bundle.putParcelable(NEW_REMINDER_KEY, newReminder)

            intent.putExtra(NEW_REMINDER_BUNDLE, bundle)
            PendingIntent.getBroadcast(
                requireActivity(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val geofence = Geofence.Builder()
            .setRequestId(newReminder.id)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setCircularRegion(
                newReminder.latitude!!,
                newReminder.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()


        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                val title = newReminder.title!!
                Toast.makeText(
                    requireContext(), getString(R.string.geofences_added, title),
                    Toast.LENGTH_SHORT
                )
                    .show()
                Log.e("Add Geofence for $title", geofence.requestId)

                // save the reminder to the local db
                _viewModel.validateAndSaveReminder(newReminder)

            }
            addOnFailureListener {
                Toast.makeText(
                    requireContext(), R.string.geofences_not_added,
                    Toast.LENGTH_SHORT
                ).show()
                if ((it.message != null)) {
                    Log.w(TAG, it.message!!)
                }
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                (activity as RemindersActivity).
                            getForegroundPermissionStatus())
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                (activity as RemindersActivity).getBackGroundPermissionStatus()
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
