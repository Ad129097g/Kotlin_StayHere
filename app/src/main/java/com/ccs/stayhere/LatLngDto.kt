package com.ccs.stayhere

data class LatLngDto(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this(0.0, 0.0)
}
