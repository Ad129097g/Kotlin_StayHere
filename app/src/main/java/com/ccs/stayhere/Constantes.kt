package com.ccs.stayhere

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log

object Constantes {

    fun obtenerTiempoDis(): Long {
        return System.currentTimeMillis()
    }

    fun obtenerFecha(tiempo: Long): String {
        val calendario = Calendar.getInstance(Locale.ENGLISH)
        calendario.timeInMillis = tiempo

        return DateFormat.format("dd/MM/yyyy", calendario).toString()
    }

    // Para la Edad
    fun calcularEdad(fechaNacimiento: String): Int {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val birthDate = sdf.parse(fechaNacimiento)
            val birthCalendar = Calendar.getInstance()
            birthCalendar.time = birthDate

            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Constantes", "Error al calcular la edad: ${e.message}")
            -1 // Retornar -1 en caso de error
        }
    }
}
