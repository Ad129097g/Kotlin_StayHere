package com.ccs.stayhere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CuartoAdapter(private val cuartosList: List<cuartos>) : RecyclerView.Adapter<CuartoAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.cuarto_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return cuartosList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentCuarto = cuartosList[position]

        holder.nombre.text = currentCuarto.nombreAlojamiento
        holder.precio.text = currentCuarto.precio
        holder.descripcion.text = currentCuarto.descripcion
        holder.caracteristicas.text = currentCuarto.caracteristicas

        // Configurar la imagen utilizando Glide para cargar desde la lista de URLs
        if (!currentCuarto.imagesUrls.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(currentCuarto.imagesUrls?.get(0)) // Cargamos la primera imagen (puedes cambiar según necesites)
                .placeholder(R.drawable.ic_oh) // Placeholder en caso de que la URL sea inválida
                .into(holder.imagen)
        } else {
            holder.imagen.setImageResource(R.drawable.ic_oh) // Si no hay URLs disponibles, usa placeholder
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.txt_nombre_cuarto)
        val precio: TextView = itemView.findViewById(R.id.txt_precio_cuarto)
        val descripcion: TextView = itemView.findViewById(R.id.txt_descripcion_cuarto)
        val caracteristicas: TextView = itemView.findViewById(R.id.txt_caracteristicas_cuarto)
        val imagen: ImageView = itemView.findViewById(R.id.img_cuarto)
    }
}
