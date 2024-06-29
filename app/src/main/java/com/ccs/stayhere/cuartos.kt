package com.ccs.stayhere

import java.util.UUID

data class cuartos(
    var nombreAlojamiento: String? = null,
    var precio: String? = null,
    var descripcion: String? = null,
    var caracteristicas: String? = null,
    var imagesUrls: List<String>? = null,
    var location: LatLngDto? = null // Usamos el DTO en lugar de LatLng
) {
    val id: String = UUID.randomUUID().toString()
}
