package com.example.ubicasure.models

data class Station(
    val name: String,
    val address: String,
    val location: Location,
    val type: String,
    val phone: String?,
    val opening_hours: List<String>?
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class StationsResponse(
    val fire_stations: List<Station>,
    val police_stations: List<Station>
)
