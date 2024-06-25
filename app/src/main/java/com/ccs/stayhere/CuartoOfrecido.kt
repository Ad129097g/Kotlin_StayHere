package com.ccs.stayhere

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ccs.stayhere.Fragmentos.MapDialogFragment
import com.ccs.stayhere.databinding.ActivityCuartoOfrecidoBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class CuartoOfrecido : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCuartoOfrecidoBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedImageUris: MutableList<Uri> = mutableListOf()
    private lateinit var storageReference: StorageReference
    private var mMap: GoogleMap? = null
    private val PERMISSION_CODE = 1001
    private var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuartoOfrecidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        storageReference = FirebaseStorage.getInstance().reference

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        requestPermissionsIfNeeded()

        binding.FABCambiarImg.setOnClickListener {
            if (selectedImageUris.size >= 3) {
                Toast.makeText(this, "Solo se permiten hasta 3 imágenes.", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, 1000)
                Log.d("CuartoOfrecido", "Botón cambiar imagen clicado")
            }
        }

        binding.btnPublicar.setOnClickListener {
            val nombreAlojamiento = binding.editNombreAlojamiento.text.toString().trim()
            val precio = binding.editPrecio.text.toString().trim()
            val descripcion = binding.editDescripcion.text.toString().trim()
            val caracteristicas = binding.editCaracteristicas.text.toString().trim()

            if (selectedImageUris.size >= 3 && nombreAlojamiento.isNotEmpty() && precio.isNotEmpty() && descripcion.isNotEmpty() && caracteristicas.isNotEmpty() && selectedLocation != null) {
                uploadDataToFirebase(nombreAlojamiento, precio, descripcion, caracteristicas)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos, seleccione al menos 3 imágenes e inserte una ubicación en el mapa.", Toast.LENGTH_SHORT).show()
            }
            Log.d("CuartoOfrecido", "Botón publicar clicado")
        }

        binding.btnBuscarUbicacion.setOnClickListener {
            showMapDialog()
        }

        binding.editCaracteristicas.setOnClickListener {
            showCaracteristicasDialog()
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
            val imageUri = data?.data
            if (imageUri != null) {
                selectedImageUris.add(imageUri)
                when (selectedImageUris.size) {
                    1 -> binding.imgCuarto.setImageURI(imageUri)
                    2 -> binding.imgCuarto2.setImageURI(imageUri)
                    3 -> binding.imgCuarto3.setImageURI(imageUri)
                }
            }
        }
    }

    private fun uploadDataToFirebase(nombreAlojamiento: String, precio: String, descripcion: String, caracteristicas: String) {
        val cuartoId = UUID.randomUUID().toString()
        val imagesUrls = mutableListOf<String>()
        var uploadCount = 0

        for (uri in selectedImageUris) {
            val filename = "$cuartoId/${UUID.randomUUID()}"
            val imageRef = storageReference.child(filename)
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { url ->
                        imagesUrls.add(url.toString())
                        uploadCount++
                        if (uploadCount == selectedImageUris.size) {
                            saveDataToFirebase(nombreAlojamiento, precio, descripcion, caracteristicas, imagesUrls)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al subir imagen.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveDataToFirebase(nombreAlojamiento: String, precio: String, descripcion: String, caracteristicas: String, imagesUrls: List<String>) {
        val database = FirebaseDatabase.getInstance().getReference("cuartos")
        val cuarto = Cuarto(nombreAlojamiento, precio, descripcion, caracteristicas, imagesUrls, selectedLocation)

        database.child(cuarto.id).setValue(cuarto)
            .addOnSuccessListener {
                Toast.makeText(this, "Cuarto publicado con éxito.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al publicar cuarto.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCaracteristicasDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_caracteristicas, null)
        builder.setView(dialogView)
        builder.setTitle("Seleccione las características")
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chipGroupCaracteristicas)
            val selectedCaracteristicas = mutableListOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedCaracteristicas.add(chip.text.toString())
                }
            }
            binding.editCaracteristicas.setText(selectedCaracteristicas.joinToString(", "))
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showMapDialog() {
        val mapDialogFragment = MapDialogFragment()
        mapDialogFragment.setOnLocationSelectedListener(object : MapDialogFragment.OnLocationSelectedListener {
            override fun onLocationSelected(location: LatLng) {
                selectedLocation = location
                binding.editUbicacion.setText("Ubicación seleccionada: ${location.latitude}, ${location.longitude}")
            }
        })
        mapDialogFragment.show(supportFragmentManager, "mapDialog")
    }

    data class Cuarto(
        val nombreAlojamiento: String,
        val precio: String,
        val descripcion: String,
        val caracteristicas: String,
        val imagesUrls: List<String>,
        val location: LatLng?
    ) {
        val id: String = UUID.randomUUID().toString()
    }
}
