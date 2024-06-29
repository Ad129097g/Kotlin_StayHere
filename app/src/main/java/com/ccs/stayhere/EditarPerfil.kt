package com.ccs.stayhere

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.ccs.stayhere.databinding.ActivityEditarPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog : ProgressDialog

    private var imageUri : Uri?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        cargarInfo()

        binding.FABCambiarImg.setOnClickListener {
            selec_imagen_de()
        }
        binding.btnActualizar.setOnClickListener {
            actualizarPerfil()
        }
    }

    private fun cargarInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(firebaseAuth.uid ?: "")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = snapshot.child("nombres").value.toString()
                    val imagen = snapshot.child("urlImagenPerfil").value.toString()
                    val fechaNac = snapshot.child("fecha_nac").value.toString()
                    val telefono = snapshot.child("Telefono").value.toString()
                    val codTelefono = snapshot.child("codigoTelefono").value.toString()
                    val mostrarBotonWhatsApp = snapshot.child("mostrarBotonWhatsApp").value as? Boolean ?: false

                    //setear
                    binding.editNombres.setText(nombres)
                    binding.editFNac.setText(fechaNac)
                    binding.editTelefono.setText(telefono)
                    binding.checkBoxMostrarWhatsApp.isChecked = mostrarBotonWhatsApp

                    // Cargar imagen con Glide
                    Glide.with(this@EditarPerfil)
                        .load(imagen)
                        .placeholder(R.drawable.ic_perfil) // Placeholder en caso de error
                        .error(R.drawable.ic_perfil) // Imagen de error
                        .into(binding.imgPerfil)

                    // Configurar código de país
                    try {
                        val codigo = codTelefono.replace("+", "").toInt()
                        binding.selectorCod.setCountryForPhoneCode(codigo)
                    } catch (e: Exception) {
                        Toast.makeText(this@EditarPerfil,
                            "Error al configurar código de país: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar cancelación de la operación
                    Toast.makeText(this@EditarPerfil,
                        "Operación cancelada",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun selec_imagen_de(){
        val popupMenu = PopupMenu(this, binding.FABCambiarImg)

        popupMenu.menu.add(Menu.NONE,1,1,"Cámara")
        popupMenu.menu.add(Menu.NONE,2,2,"Galeria")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item->
            val itemId = item.itemId
            if (itemId == 1){
                //camara
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                    concederPermisoCamara.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
            }else if (itemId == 2){
                //galeria
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    imagenGaleria()
                }else{
                    concederPermisoAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            return@setOnMenuItemClickListener true
        }

    }

    private val concederPermisoCamara =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){resultado->
            var concedidoTodos = true
            for (seConcede in resultado.values){
                concedidoTodos = concedidoTodos && seConcede
            }

            if (concedidoTodos){
                imagenCamara()
            }else{
                Toast.makeText(
                    this,
                    "El permiso de la camara o almacenamiento ha sido denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun imagenCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Titulo_imagen")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Descripction_imagen")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        resultadoCamara_ARL.launch(intent)
    }

    private val resultadoCamara_ARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado->
            if(resultado.resultCode == Activity.RESULT_OK){
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_perfil)
                        .into(binding.imgPerfil)
                }catch (e:Exception){

                }
            }else{
                Toast.makeText(
                    this,
                    "Cancelado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val concederPermisoAlmacenamiento =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){esConcedido->
            if (esConcedido){
                imagenGaleria()
            }else{
                Toast.makeText(
                    this,
                    "El permiso de almacenamiento ha sido denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_ARL.launch(intent)
    }

    private val resultadoGaleria_ARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado->
            if (resultado.resultCode == Activity.RESULT_OK){
                val data = resultado.data
                imageUri = data!!.data
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_perfil)
                        .into(binding.imgPerfil)
                }catch (e:Exception){

                }
            }else{
                Toast.makeText(
                    this,
                    "Cancelado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private fun actualizarPerfil() {
        progressDialog.setMessage("Actualizando perfil...")
        progressDialog.show()

        val nombres = binding.editNombres.text.toString().trim()
        val fechaNac = binding.editFNac.text.toString().trim()
        val telefono = binding.editTelefono.text.toString().trim()
        val codigoTelefono = binding.selectorCod.selectedCountryCodeWithPlus
        val mostrarBotonWhatsApp = binding.checkBoxMostrarWhatsApp.isChecked

        if (nombres.isEmpty() || fechaNac.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }

        val hashMap = hashMapOf<String, Any>(
            "nombres" to nombres,
            "fecha_nac" to fechaNac,
            "Telefono" to telefono,
            "codigoTelefono" to codigoTelefono,
            "mostrarBotonWhatsApp" to mostrarBotonWhatsApp
        )

        if (imageUri != null) {
            val filePathAndName = "Perfil_Imagenes/" + firebaseAuth.uid
            val reference = FirebaseStorage.getInstance().reference.child(filePathAndName)
            reference.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val uploadedImageUrl = uriTask.result.toString()

                    hashMap["urlImagenPerfil"] = uploadedImageUrl

                    actualizarInfoEnBD(hashMap)
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            actualizarInfoEnBD(hashMap)
        }
    }
    private fun actualizarInfoEnBD(hashMap: HashMap<String, Any>) {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(firebaseAuth.uid ?: "")
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
