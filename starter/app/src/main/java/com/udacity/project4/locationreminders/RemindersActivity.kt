package com.udacity.project4.locationreminders

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.udacity.project4.R
import kotlinx.android.synthetic.main.activity_reminders.*

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private var foregroundLocationPermission:Boolean = false
    private var backgroundLocationPermission:Boolean = false

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.e("DEBUG", "${it.key} = ${it.value}")
            if (it.value == false && (
                it.key == "android.permission.ACCESS_FINE_LOCATION" ||
                it.key == "android.permission.ACCESS_COARSE_LOCATION")) {
                Toast.makeText(applicationContext,getString(R.string.toast_message_loc_permissions),Toast.LENGTH_LONG).show()}

            if (it.key == "android.permission.ACCESS_FINE_LOCATION") {
                foregroundLocationPermission = it.value
            }

            if (it.key == "android.permission.ACCESS_BACKGROUND_LOCATION") {
                backgroundLocationPermission = it.value
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)


    }

    fun getForegroundPermissionStatus():Boolean {
        requestMultiplePermissions.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        return foregroundLocationPermission
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getBackGroundPermissionStatus():Boolean {
        requestMultiplePermissions.launch(
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))

        return true
    }

    fun checkLocationPermissions () {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                (nav_host_fragment as NavHostFragment).navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


}
