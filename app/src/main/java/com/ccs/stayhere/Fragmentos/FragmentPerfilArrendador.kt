package com.ccs.stayhere.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ccs.stayhere.Constantes
import com.ccs.stayhere.CuartoAdapter
import com.ccs.stayhere.CuartoOfrecido
import com.ccs.stayhere.EditarPerfil
import com.ccs.stayhere.OpcionesLogin
import com.ccs.stayhere.R
import com.ccs.stayhere.cuartos
import com.ccs.stayhere.databinding.FragmentPerfilArrendadorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FragmentPerfilArrendador : Fragment() {

    private lateinit var binding: FragmentPerfilArrendadorBinding

    private lateinit var dbref : DatabaseReference
    private lateinit var cuartoRecyclerView: RecyclerView
    private lateinit var cuartoArrayList: ArrayList<cuartos>

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPerfilArrendadorBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.cuartolist.layoutManager = LinearLayoutManager(mContext)
        binding.cuartolist.setHasFixedSize(true)

        cuartoArrayList = arrayListOf()
        getCuartoData()

        firebaseAuth = FirebaseAuth.getInstance()

        leerInfo()

        binding.btnEditar.setOnClickListener {
            startActivity(Intent(mContext, EditarPerfil::class.java))
        }

        binding.btnCsesion.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLogin::class.java))
            activity?.finishAffinity()
        }

        binding.btnNuevo.setOnClickListener {
            startActivity(Intent(mContext, CuartoOfrecido::class.java))
            activity?.finishAffinity()
        }

        return view
    }

    private fun leerInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(firebaseAuth.uid ?: "")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val tipoUsuario = snapshot.child("tipo_usuario").value?.toString() ?: ""
                        if (tipoUsuario == "arrendador") {
                            val nombres = snapshot.child("nombres").value?.toString() ?: ""
                            val gmail = snapshot.child("gmail").value?.toString() ?: ""
                            val imagen = snapshot.child("urlImagenPerfil").value?.toString() ?: ""
                            val f_nac = snapshot.child("fecha_nac").value?.toString() ?: ""
                            var tiempo = snapshot.child("tiempo").value?.toString() ?: "0"
                            val telefono = snapshot.child("Telefono").value?.toString() ?: ""
                            val codTelefono = snapshot.child("codigoTelefono").value?.toString() ?: ""

                            val cod_tel = codTelefono + telefono

                            val for_tiempo = Constantes.obtenerFecha(tiempo.toLong())

                            // Calcular la edad
                            val edad = Constantes.calcularEdad(f_nac)

                            // Seteamos la información
                            binding.txtNombres.text = nombres
                            binding.txtEdad.text = if (edad != -1) edad.toString() else "N/A"
                            binding.txtNumero.text = cod_tel

                            // Seteo de la imagen
                            Glide.with(mContext)
                                .load(imagen)
                                .placeholder(R.drawable.a)
                                .into(binding.ivPerfil)
                        } else {
                            Toast.makeText(mContext, "Usuario no es arrendador", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(mContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("FragmentPerfilArrendador", "Error al leer la información", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(mContext, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun getCuartoData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val idUsuario = currentUser?.uid

        dbref = FirebaseDatabase.getInstance().getReference("cuartos")

        dbref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cuartoArrayList.clear()

                for (cuartoSnapshot in snapshot.children) {
                    val cuarto = cuartoSnapshot.getValue(cuartos::class.java)
                    if (cuarto != null && cuarto.idUsuario == idUsuario) {
                        cuartoArrayList.add(cuarto)
                    }
                }

                binding.cuartolist.adapter = CuartoAdapter(cuartoArrayList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores si es necesario
            }
        })
    }

}
