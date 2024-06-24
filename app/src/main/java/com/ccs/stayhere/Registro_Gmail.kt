package com.ccs.stayhere

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.ccs.stayhere.Fragmentos.FragmentPerfilArrendador
import com.ccs.stayhere.databinding.ActivityRegistroGmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Registro_Gmail : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroGmailBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroGmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.btnRegistrar.setOnClickListener {
            validarInfo()
        }

    }

    private var gmail = ""
    private var password = " "
    private var r_password = ""
    private var nombres = ""
    private var f_nac = ""
    private var celular = ""
    private var codTelefono = ""

    private fun validarInfo() {
        // Trim para eliminar espacios en blanco
        gmail = binding.editGmail.text.toString().trim()
        password = binding.editPassword.text.toString().trim()
        r_password = binding.editRPassword.text.toString().trim()
        nombres = binding.editNombre.text.toString().trim()
        f_nac = binding.editFNac.text.toString().trim()
        celular = binding.editTelefono.text.toString().trim()
        codTelefono = binding.selectorCod.selectedCountryCodeWithPlus

        if (!Patterns.EMAIL_ADDRESS.matcher(gmail).matches()) {
            binding.editGmail.error = "Email invalido"
            binding.editGmail.requestFocus()
        } else if (gmail.isEmpty()) {
            binding.editPassword.error = "Ingrese password"
            binding.editPassword.requestFocus()
        } else if (r_password.isEmpty()) {
            binding.editRPassword.error = "Repite el password"
            binding.editRPassword.requestFocus()
        } else if (password != r_password) {
            binding.editRPassword.error = "No coinciden"
            binding.editRPassword.requestFocus()
        } else {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(gmail, password)
            .addOnSuccessListener {
                llenarInfoBD()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registró el usuario debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun llenarInfoBD() {
        progressDialog.setMessage("Guardando Información")

        val tiempo = Constantes.obtenerTiempoDis()
        val gmailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = nombres
        hashMap["codigoTelefono"] = codTelefono
        hashMap["Telefono"] = celular
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Gmail"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["online"] = true
        hashMap["gmail"] = gmailUsuario!!
        hashMap["uid"] = uidUsuario!!
        hashMap["fecha_nac"] = f_nac
        hashMap["tipo_usuario"] = if (binding.radioEstudiante.isChecked) "estudiante" else "arrendador"

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")

        ref.child(uidUsuario)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registró debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


}
