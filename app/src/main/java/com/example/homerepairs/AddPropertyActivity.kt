package com.example.homerepairs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.homerepairs.databinding.ActivityPropertyBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth

class AddPropertyActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPropertyBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val permissionCode = 101
    private var selectedLatLng: LatLng? = null
    private var selectedPropertyType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            navigateToSignIn(clearStack = true)
            return
        }

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDg-u7_cdNW2ZbCMmaLBTmK_YQ2FZ1mHiI")
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

//        val mapFragment = supportFragmentManager
//            .findFragmentById(R.id.map) as SupportMapFragment
//        mapFragment.getMapAsync(this)

        // Property type click listeners
        setPropertyTypeClick(binding.house, "House")
        setPropertyTypeClick(binding.apartment, "Apartment")
        setPropertyTypeClick(binding.condo, "Condo")
        setPropertyTypeClick(binding.commercial, "Commercial")

        // Skip button
        binding.BtNskip.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // Continue button
//        binding.Btncontinue.setOnClickListener {
//            val address = binding.address.text.toString().trim()
//            val unit = binding.unitNumber.text.toString().trim() // Unit/Apt
//
//            if (selectedPropertyType == null) {
//                Toast.makeText(this, "Please select a property type", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            if (address.isEmpty()) {
//                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            if (unit.isEmpty()) {
//                Toast.makeText(this, "Please enter Unit/Apt number", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            if (selectedLatLng == null) {
//                Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val locationLink = "https://www.google.com/maps/search/?api=1&query=${selectedLatLng!!.latitude},${selectedLatLng!!.longitude}"
//
//            // Save all data to SharedPreferences
//            val prefs = getSharedPreferences("PROPERTY_PREFS", MODE_PRIVATE)
//            prefs.edit()
//                .putString("property_type", selectedPropertyType)
//                .putString("address", address)
//                .putString("unit", unit)
//                .putString("location_link", locationLink)
//                .apply()
//
//            // Navigate to HomeActivity
//            startActivity(Intent(this, HomeActivity::class.java))
//            finish()
//        }
    }

    private fun setPropertyTypeClick(view: TextView, type: String) {
        view.setOnClickListener {
            selectedPropertyType = type
            // Highlight selected view
            resetPropertyTypeBackgrounds()
            view.setBackgroundResource(R.drawable.rounded_corner) // define in drawable
        }
    }

    private fun resetPropertyTypeBackgrounds() {
        binding.house.setBackgroundResource(R.drawable.rounded_edittext)
        binding.apartment.setBackgroundResource(R.drawable.rounded_edittext)
        binding.condo.setBackgroundResource(R.drawable.rounded_edittext)
        binding.commercial.setBackgroundResource(R.drawable.rounded_edittext)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermissionAndShow()

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            selectedLatLng = latLng
        }
    }

    private fun checkLocationPermissionAndShow() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissionCode)
            return
        }
        showCurrentLocation()
    }

    private fun showCurrentLocation() {
        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    selectedLatLng = currentLatLng
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToSignIn(clearStack: Boolean) {
        val intent = Intent(this, SignInActivity::class.java)
        if (clearStack) intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
