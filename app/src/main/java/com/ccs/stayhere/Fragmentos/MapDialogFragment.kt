package com.ccs.stayhere.Fragmentos

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.ccs.stayhere.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapDialogFragment : DialogFragment(R.layout.dialog_map), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var onLocationSelectedListener: OnLocationSelectedListener? = null

    interface OnLocationSelectedListener {
        fun onLocationSelected(location: LatLng)
    }

    fun setOnLocationSelectedListener(listener: OnLocationSelectedListener) {
        onLocationSelectedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Centrar el mapa en Perú al inicio
        val peruLatLng = LatLng(-9.19, -75.0152)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(peruLatLng, 5f))

        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Obtener la última ubicación conocida
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Si se encuentra una ubicación válida, centrar el mapa en esa ubicación
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } else {
                        // Si no se puede obtener la ubicación actual, mostrar un mensaje de error
                        Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        mMap?.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            mMap?.clear()
            mMap?.addMarker(MarkerOptions().position(latLng))

            onLocationSelectedListener?.onLocationSelected(latLng)
            dismiss()
        }
    }
}
