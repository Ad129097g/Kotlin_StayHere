package com.ccs.stayhere

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ccs.stayhere.R
import com.ccs.stayhere.databinding.ActivityCuartoOfrecidoBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class CuartoOfrecido : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCuartoOfrecidoBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedImageUri: Uri? = null
    private var mMap: GoogleMap? = null
    private val PERMISSION_CODE = 1001
    private var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuartoOfrecidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermissionsIfNeeded()

        binding.FABCambiarImg.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1000)
            Log.d("CuartoOfrecido", "Botón cambiar imagen clicado")
        }

        binding.btnPublicar.setOnClickListener {
            val precio = binding.editPrecio.text.toString().trim()
            val compartida = binding.editCompartida.text.toString().trim()
            val bano = binding.editBano.text.toString().trim()
            val mascotas = binding.editMascotas.text.toString().trim()

            if (selectedImageUri != null && precio.isNotEmpty() && compartida.isNotEmpty() && bano.isNotEmpty() && mascotas.isNotEmpty() && selectedLocation != null) {
                uploadDataToFirebase(precio, compartida, bano, mascotas)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos e inserte una imagen y seleccione una ubicación en el mapa.", Toast.LENGTH_SHORT).show()
            }
            Log.d("CuartoOfrecido", "Botón publicar clicado")
        }

        binding.btnBuscarUbicacionActual.setOnClickListener {
            obtenerUbicacionActual()
        }

        binding.btnBuscarUbicacion.setOnClickListener {
            val ubicacion = binding.editUbicacion.text.toString().trim()
            if (ubicacion.isNotEmpty()) {
                buscarUbicacion(ubicacion)
            } else {
                Toast.makeText(this, "Por favor, ingrese una ubicación.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Centrar el mapa en Perú al inicio
        val peruLatLng = LatLng(-9.19, -75.0152)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(peruLatLng, 5f))

        // Verificar permisos de ubicación
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Obtener la última ubicación conocida
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Si se encuentra una ubicación válida, centrar el mapa en esa ubicación
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } else {
                        // Si no se puede obtener la ubicación actual, mostrar un mensaje de error
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        mMap?.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            mMap?.clear()
            mMap?.addMarker(MarkerOptions().position(latLng))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == AppCompatActivity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.imgCuarto.setImageURI(selectedImageUri)
        }
    }

    private fun uploadDataToFirebase(precio: String, compartida: String, bano: String, mascotas: String) {
        if (selectedImageUri != null) {
            val filename = UUID.randomUUID().toString()
            val storageReference = FirebaseStorage.getInstance().getReference("images/$filename")
            storageReference.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        saveDataToFirebase(precio, compartida, bano, mascotas, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al subir la imagen.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveDataToFirebase(precio: String, compartida: String, bano: String, mascotas: String, imageUrl: String) {
        val database = FirebaseDatabase.getInstance().getReference("cuartos")
        val cuartoId = database.push().key
        val cuarto = Cuarto(precio, compartida, bano, mascotas, imageUrl, selectedLocation)

        if (cuartoId != null) {
            database.child(cuartoId).setValue(cuarto).addOnCompleteListener {
                Toast.makeText(this, "Cuarto publicado con éxito.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun obtenerUbicacionActual() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun buscarUbicacion(ubicacion: String) {
        val geocoder = Geocoder(this)
        val addressList = geocoder.getFromLocationName(ubicacion, 1)
        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            val latLng = LatLng(address.latitude, address.longitude)
            mMap?.clear()
            mMap?.addMarker(MarkerOptions().position(latLng).title(ubicacion))
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            Toast.makeText(this, "No se pudo encontrar la ubicación.", Toast.LENGTH_SHORT).show()
        }
    }

    data class Cuarto(
        val precio: String,
        val compartida: String,
        val bano: String,
        val mascotas: String,
        val imageUrl: String,
        val location: LatLng?
    )
}