package com.ccs.stayhere.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ccs.stayhere.CuartoAdapter
import com.ccs.stayhere.EditarPerfil
import com.ccs.stayhere.OpcionesLogin
import com.ccs.stayhere.R
import com.ccs.stayhere.cuartos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FragmentPerfilArrendador : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_perfil_arrendador, container, false)

        cuartoRecyclerView = view.findViewById(R.id.cuartolist)
        cuartoRecyclerView.layoutManager = LinearLayoutManager(mContext)
        cuartoRecyclerView.setHasFixedSize(true)

        cuartoArrayList = arrayListOf()
        getCuartoData()

        firebaseAuth = FirebaseAuth.getInstance()

        view.findViewById<View>(R.id.btn_editar).setOnClickListener {
            startActivity(Intent(mContext, EditarPerfil::class.java))
        }

        view.findViewById<View>(R.id.btn_csesion).setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLogin::class.java))
            activity?.finishAffinity()
        }

        view.findViewById<View>(R.id.btn_nuevo).setOnClickListener {
            // Implementa la lógica para agregar un nuevo cuarto ofrecido
        }

        return view
    }

    private fun getCuartoData() {
        dbref = FirebaseDatabase.getInstance().getReference("cuartos") // Aquí se cambia a "cuartos"

        dbref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cuartoArrayList.clear()

                for (cuartoSnapshot in snapshot.children) {
                    val cuarto = cuartoSnapshot.getValue(cuartos::class.java)
                    cuarto?.let { cuartoArrayList.add(it) }
                }

                cuartoRecyclerView.adapter = CuartoAdapter(cuartoArrayList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores si es necesario
            }
        })
    }
}
