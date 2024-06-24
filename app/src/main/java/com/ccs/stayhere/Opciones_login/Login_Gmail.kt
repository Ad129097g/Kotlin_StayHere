package com.ccs.stayhere.Opciones_login

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.ccs.stayhere.MainActivity
import com.ccs.stayhere.R
import com.ccs.stayhere.Registro_Gmail
import com.ccs.stayhere.databinding.ActivityLoginGmailBinding
import com.google.firebase.auth.FirebaseAuth

class Login_Gmail : AppCompatActivity() {

    private lateinit var binding: ActivityLoginGmailBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginGmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.btnIngresar.setOnClickListener {
            validarInfo()
        }

        binding.txtRegistrar.setOnClickListener {
            startActivity(Intent(this@Login_Gmail, Registro_Gmail::class.java))
        }
    }

    private var gmail = ""
    private var password = ""
    private fun validarInfo() {
       gmail =  binding.editGmail.text.toString().trim()
        password = binding.editPassword.text.toString().trim()

        if(!Patterns.EMAIL_ADDRESS.matcher(gmail).matches()){
            binding.editGmail.error = "Gmail invalido"
            binding.editGmail.requestFocus()
        }
        else if (gmail.isEmpty()){
            binding.editGmail.error = "Ingrese gmail"
            binding.editGmail.requestFocus()
        }
        else if (password.isEmpty()){
            binding.editPassword.error = "Ingrese pasword"
            binding.editPassword.requestFocus()
        }else{
            loginUsuario()
        }

    }

    private fun loginUsuario() {
        progressDialog.setMessage("Ingresando")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(gmail, password)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
                Toast.makeText(
                    this,
                    "Bienvenido(a)",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se puedo iniciar sesion debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
    }
}